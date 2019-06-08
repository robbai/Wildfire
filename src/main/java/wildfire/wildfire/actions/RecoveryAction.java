package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.pitch.Pitch;
import wildfire.wildfire.pitch.Triangle;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Pair;
import wildfire.wildfire.utils.Utils;

public class RecoveryAction extends Action {
	
	/*
	 * Good old PID controllers, they never fail
	 */	
	private PID pitchPID, yawPID, rollPID;
	
	private final double fallStep = (1D / 30), zToBoost = -0.6;
	private final int fallDepth = (int)(2.6 / fallStep), // It only takes around 2.5 seconds to fall from the ceiling, so this is a safe bet
			fallStepIndex = 15; 

	private boolean boostDown;

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);
		
		this.pitchPID = new PID(4.5, 0, 1.3);
		this.yawPID = new PID(3, 0, 1.3);
		this.rollPID = new PID(1.2, 0, 0.25);
		
		this.boostDown = false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double yaw, roll, pitch;
		
		boolean planWavedash = (!input.car.doubleJumped && !boostDown && input.car.velocity.z < -420 && input.car.orientation.roofVector.normalized().z > 0.8);
		wildfire.renderer.drawString2d("Plan Wavedash: " + planWavedash, Color.WHITE, new Point(0, 40), 2, 2);
		
		Vector3[] fall = simulateFall(boostDown ? (input.car.orientation.noseVector.z < zToBoost ? Color.RED : Color.YELLOW) : Color.WHITE, input.car.position, input.car.velocity);
		
		Pair<Triangle, Vector3> intersect = getIntersect(fall);
		Triangle triangle = (intersect == null ? null : intersect.getOne());
		Vector3 triangleCentre = (triangle == null ? null : triangle.getCentre());
		Vector3 intersectLocation = (intersect == null ? null : intersect.getTwo());
		
		// whatisaphone's Secret Recipe.
		boostDown = (input.car.boost > 5 && (triangle == null || intersectLocation.distance(input.car.position) / Math.max(300, input.car.velocity.magnitude()) > 1.05)
				&& (triangle == null || triangleCentre.z < Constants.CEILING - 500));
		
		if(input.car.position.z > 50){
			// Yaw.
			Vector3 yawIdealDestination;
			if(triangle != null && triangleCentre.z > 300){
				yawIdealDestination = triangleCentre.scaled(0.95).withZ(17);
			}else if(input.car.velocity.flatten().magnitude() > 600){
				yawIdealDestination = input.car.position.plus(input.car.velocity).withZ(17);
			}else{
				yawIdealDestination = wildfire.impactPoint.getPosition();
			}
			
			double yawCorrection = -Handling.aimLocally(input.car, yawIdealDestination);
			yaw = yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
		}else{
			// Perform the wave-dash.
			if(planWavedash && input.car.position.z < 75){
				return new ControlsOutput().withPitch(-1).withYaw(0).withRoll(0).withJump(true);
			}
			yaw = 0;
		}
		
		// Roll and pitch.
		if(triangle != null && !boostDown && (!planWavedash || triangleCentre.z > 20)){
			Vector3 fallNormal = triangle.getNormal().scaled(-1);
			
			Vector3 normalLocal = Utils.toLocalFromRelative(input.car, fallNormal);
			double targetPitch = Math.atan(1 - normalLocal.z);
			double targetRoll = Math.atan(normalLocal.x);

			pitch = pitchPID.getOutput(input.elapsedSeconds, 0, targetPitch);
			roll = rollPID.getOutput(input.elapsedSeconds, 0, targetRoll);
		}else{
			roll = rollPID.getOutput(input.elapsedSeconds, -Math.atan(input.car.orientation.rightVector.z), 0);
			
			double angularCoefficient = Math.signum(Math.cos(input.car.orientation.eularRoll));
			pitch = angularCoefficient * pitchPID.getOutput(input.elapsedSeconds, Math.atan(input.car.orientation.noseVector.z), 
					boostDown ? -0.9 : (planWavedash ? Utils.clamp(input.car.position.z / 1000, 0.3, 0.8) : 0));
		}
		
		return new ControlsOutput().withRoll((float)roll).withPitch((float)pitch).withYaw((float)yaw)
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
		velocity = velocity.minus(velocity.scaled(Math.pow(0.03, 1D / fallStep)).withZ(0)); // Drag.
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		Vector3 newPosition = position.plus(velocity.scaled(fallStep));
		
		wildfire.renderer.drawLine3d(colour, position.toFramework(), newPosition.toFramework());
		positions[depth] = newPosition;
		return simulateFall(colour, positions, velocity, depth + 1);
	}
	
	private Pair<Triangle, Vector3> getIntersect(Vector3[] fall){
		for(int index = 0; index < fall.length; index += fallStepIndex){
			Pair<Triangle, Vector3> intersect = intersect(fall[index], 
					fall[Math.min(fall.length - 1, index + fallStepIndex)]);
			if(intersect != null) return intersect;
		}
		return null;
	}
	
	private Pair<Triangle, Vector3> intersect(Vector3 segment1, Vector3 segment2){
		Pair<Triangle, Vector3> intersect = Pitch.segmentIntersect(segment1, segment2);
		if(intersect == null) return null;
		
		wildfire.renderer.drawTriangle(Color.GREEN, intersect.getOne());
		return intersect;
	}

//	private double distanceToArena(Vector3 carPosition){
//		double X = Math.abs(Math.abs(carPosition.x) - Constants.PITCHWIDTH);
//		double Y = Math.abs(Math.abs(carPosition.y) - Constants.PITCHLENGTH);
//		double Z = Math.min(Math.abs(Math.abs(carPosition.z) - Constants.PITCHWIDTH), Math.abs(carPosition.z));
//		
//		return Math.min(X, Math.min(Y, Z));
//	}

}
