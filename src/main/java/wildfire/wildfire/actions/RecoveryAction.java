package wildfire.wildfire.actions;

import java.awt.Color;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.pitch.Pitch;
import wildfire.wildfire.pitch.Triangle;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class RecoveryAction extends Action {
	
	/*
	 * Good old PID controllers, they never fail
	 */	
	private PID pitchPID, yawPID, rollPID;
	
	private final double fallStep = (1D / 30), zToBoost = -0.6;
	private final int fallDepth = (int)(2.6 / fallStep), // It only takes around 2.5 seconds to fall from the ceiling, so this is a safe bet
			fallStepIndex = 10; 

	private boolean bangBang;
	
	private boolean boostDown;

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);
		
		this.pitchPID = new PID(3.4, 0, 0.7);
		this.yawPID = new PID(2.8, 0, 1.2);
		this.rollPID = new PID(2.8, 0, 0.5);
		
		this.bangBang = false;
		
		this.boostDown = false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double yaw, roll, pitch;
		
		boolean planWavedash = (!input.car.doubleJumped && !boostDown && input.car.velocity.z < -420 && input.car.orientation.roofVector.normalized().z > 0.8);
//		wildfire.renderer.drawString2d("Plan Wavedash: " + planWavedash, Color.WHITE, new Point(0, 40), 2, 2);
		
		Vector3[] fall = simulateFall(boostDown ? (input.car.orientation.noseVector.z < zToBoost ? Color.RED : Color.YELLOW) : Color.WHITE, input.car.position, input.car.velocity);
		
		Pair<Triangle, PredictionSlice> intersect = getIntersect(fall);
		Triangle triangle = (intersect == null ? null : intersect.getOne());
		Vector3 triangleCentre = (triangle == null ? null : triangle.getCentre());
//		Vector3 intersectLocation = (intersect == null ? null : intersect.getTwo().getPosition());
		double intersectTime = (intersect == null ? 10 : intersect.getTwo().getFrame() * fallStep);
		
		// whatisaphone's Secret Recipe.
		boostDown = (input.car.boost > 5 && (triangle == null || (intersectTime > 0.7 && triangleCentre.z < Constants.CEILING - 500)));
		
		// Roll and pitch.
		boolean correctEnough = true;
		if(triangle != null && !boostDown && (!planWavedash || triangleCentre.z > 20)){
			Vector3 fallNormal = triangle.getNormal().scaled(-1);
			
			wildfire.renderer.drawLine3d(Color.GREEN, input.car.position, input.car.position.plus(fallNormal.scaledToMagnitude(120)));
			
			Vector3 normalLocal = Utils.toLocalFromRelative(input.car, fallNormal);
			Vector2 angles = Handling.getAnglesRoof(normalLocal);
			
			roll = this.rollPID.getOutput(input.elapsedSeconds, 0, angles.x);			
			pitch = this.pitchPID.getOutput(input.elapsedSeconds, 0, angles.y);
			
			// Yaw can get annoying unless at the others are right, it seems.
			correctEnough = (Math.abs(roll) + Math.abs(pitch) < 1.2);
		}else{
			roll = this.rollPID.getOutput(input.elapsedSeconds, Math.sin(input.car.orientation.eularRoll), 0);
			
			double targetPitch = boostDown ? -0.95 : (planWavedash ? Utils.clamp(input.car.position.z / 2000, 0.35, 0.6) : 0);
			pitch = this.pitchPID.getOutput(input.elapsedSeconds, Math.asin(input.car.orientation.noseVector.z), targetPitch);
		}
		
		if(input.car.position.z > 50){
			// Yaw.
			Vector3 yawIdealDestination;
			if(triangle != null && triangleCentre.z > 200){
				// Aim down the wall.
				yawIdealDestination = input.car.position
						.plus(input.car.orientation.roofVector.scaledToMagnitude(50))
						.withZ(17);
			}else if(input.car.velocity.withZ(Math.max(0, input.car.velocity.z)).magnitude() > 800){
				yawIdealDestination = input.car.position.plus(input.car.velocity).withZ(17);
			}else{
				yawIdealDestination = wildfire.impactPoint.getPosition();
			}
			
			if(correctEnough){
				double yawCorrection = Handling.aimLocally(input.car, yawIdealDestination);
				yaw = yawPID.getOutput(input.elapsedSeconds, yawCorrection, 0);
			}else{
				yaw = 0;
			}
		}else{
			// Perform the wave-dash.
			if(planWavedash && input.car.position.z < 90){
				return new ControlsOutput().withPitch(-1).withJump(true)
						.withYaw(0).withRoll(0);
			}
			yaw = 0;
		}
		
		// Bang-bang controls
		if(this.bangBang){
			pitch = Math.signum(pitch);
			yaw = Math.signum(yaw);
			roll = Math.signum(roll);
		}
		
		return new ControlsOutput().withRoll(roll).withPitch(pitch).withYaw(yaw)
				.withBoost(input.car.orientation.noseVector.z < zToBoost && boostDown).withJump(false)
				.withThrottle(timeDifference(input.elapsedSeconds) > 0.5 ? 1 : 0); //Throttle to avoid turtling
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}
	
	private Vector3[] simulateFall(Color colour, Vector3 position, Vector3 velocity){
		Vector3[] positions = new Vector3[fallDepth];
		positions[0] = position;
		return simulateFall(colour, positions, velocity, 1);
	}
	
	private Vector3[] simulateFall(Color colour, Vector3[] positions, Vector3 velocity, int depth){
		if(depth == fallDepth) return positions;
		Vector3 position = positions[depth - 1];
		
		// Apply physics.
		velocity = velocity.plus(new Vector3(0, 0, -Constants.GRAVITY * fallStep)); // Gravity.
		velocity = velocity.minus(velocity.scaled(Math.pow(0.03, 1D / fallStep)).withZ(0)); // Drag (no idea if the car does this lol).
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		Vector3 newPosition = position.plus(velocity.scaled(fallStep));
		
		wildfire.renderer.drawLine3d(colour, position.toFramework(), newPosition.toFramework());
		positions[depth] = newPosition;
		return simulateFall(colour, positions, velocity, depth + 1);
	}
	
	private Pair<Triangle, PredictionSlice> getIntersect(Vector3[] fall){
		for(int index = 0; index < fall.length; index += fallStepIndex){
			Pair<Triangle, Vector3> intersect = intersect(fall[index], 
					fall[Math.min(fall.length - 1, index + fallStepIndex)]);
			
			// Intersected.
			if(intersect != null){
				return new Pair<Triangle, PredictionSlice>(intersect.getOne(),
						new PredictionSlice(intersect.getTwo(), index)); // Time is a lower bound.
			}
		}
		return null;
	}
	
	private Pair<Triangle, Vector3> intersect(Vector3 segment1, Vector3 segment2){
		Pair<Triangle, Vector3> intersect = Pitch.segmentIntersect(segment1, segment2);
		if(intersect == null) return null;
		
		wildfire.renderer.drawTriangle(Color.GREEN, intersect.getOne());
		return intersect;
	}

	public Action bangBang(boolean bangBang){
		this.bangBang = bangBang;
		return this;
	}

//	private double distanceToArena(Vector3 carPosition){
//		double X = Math.abs(Math.abs(carPosition.x) - Constants.PITCHWIDTH);
//		double Y = Math.abs(Math.abs(carPosition.y) - Constants.PITCHLENGTH);
//		double Z = Math.min(Math.abs(Math.abs(carPosition.z) - Constants.PITCHWIDTH), Math.abs(carPosition.z));
//		
//		return Math.min(X, Math.min(Y, Z));
//	}

}
