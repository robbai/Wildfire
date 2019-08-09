package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.pitch.Pitch;
import wildfire.wildfire.pitch.Triangle;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class RecoveryAction extends Action {
	
	private final double fallStep = (1D / 30), zToBoost = 0.8;
	private final int fallDepth = (int)(2.6 / fallStep), // It only takes around 2.5 seconds to fall from the ceiling, so this is a safe bet
			fallStepIndex = 10;
	
	private boolean boostDown;

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);

		this.boostDown = false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		boolean canBoostDown = (input.car.orientation.noseVector.dotProduct(AirControl.worldUp.scaled(-1)) > zToBoost);
				
		Vector3[] fall = simulateFall(boostDown ? (canBoostDown ? Color.RED : Color.YELLOW) : Color.WHITE, input.car.position, input.car.velocity);
		
		Pair<Triangle, PredictionSlice> intersect = getIntersect(fall);
		Triangle triangle = (intersect == null ? null : intersect.getOne());
		Vector3 triangleCentre = (triangle == null ? null : triangle.getCentre());
		Vector3 intersectLocation = (intersect == null ? null : intersect.getTwo().getPosition());
		double intersectTime = (intersect == null ? 10 : intersect.getTwo().getFrame() * fallStep);
		wildfire.renderer.drawString2d("Est. Time: " + Utils.round(intersectTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		
		// whatisaphone's Secret Recipe.
		boostDown = (input.car.boost > 5 && (intersect == null || (intersectTime > 1.3 && intersectLocation.z < Constants.CEILING - 400)));
		
		boolean planWavedash = (!input.car.doubleJumped && !boostDown && input.car.velocity.z < -420 && input.car.orientation.roofVector.z > 0.8
				&& (intersect == null || intersectLocation.z < 400));
		wildfire.renderer.drawString2d("Plan Wavedash: " + planWavedash, Color.WHITE, new Point(0, 60), 2, 2);
		
		Vector3 desiredRoof;
		if(boostDown){
			desiredRoof = input.car.velocity.withZ(0);
		}else if(triangle != null && (!planWavedash || triangleCentre.z > 20)){
			Vector3 fallNormal = triangle.getNormal().scaled(-1);
			
			wildfire.renderer.drawLine3d(Color.GREEN, input.car.position, input.car.position.plus(fallNormal.scaledToMagnitude(120)));
			
			desiredRoof = fallNormal;
		}else{
			desiredRoof = AirControl.worldUp;
		}
		
		Vector3 desiredNose;
		if(boostDown || (triangle != null && triangleCentre.z > 200 && intersectLocation.z < Constants.CEILING - 400)){
			// Aim down.
			desiredNose = AirControl.worldUp.scaled(-1);
		}else if(input.car.velocity.flatten().magnitude() > 800){
			desiredNose = input.car.velocity.withZ(0);
		}else{
			desiredNose = wildfire.impactPoint.getPosition().minus(input.car.position).withZ(0);
		}
			
		if(planWavedash){
			desiredNose = desiredNose.flatten().normalized().withZ(0.3);
			if(input.car.position.z < 50){
				// Perform the wave-dash.
				return new ControlsOutput().withPitch(-1).withJump(true)
						.withYaw(0).withRoll(0);
			}
		}
		
		return new ControlsOutput().withPitchYawRoll(AirControl.getPitchYawRoll(input.car, desiredNose, desiredRoof))
				.withBoost(boostDown && canBoostDown).withJump(false)
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
		for(int i = 0; i < fall.length; i += fallStepIndex){
			int j = Math.min(fall.length - 1, i + fallStepIndex);
			Pair<Vector3, Vector3> lineSegment = new Pair<Vector3, Vector3>(fall[i], fall[j]);
			Pair<Triangle, Vector3> intersect = intersect(lineSegment.getOne(), lineSegment.getTwo());
			
			// Intersected.
			if(intersect != null){
				return new Pair<Triangle, PredictionSlice>(intersect.getOne(),
						new PredictionSlice(intersect.getTwo(), Utils.lerp(i, j, Utils.pointLineSegmentT(intersect.getTwo(), lineSegment)) * fallStep));
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

//	private double distanceToArena(Vector3 carPosition){
//		double X = Math.abs(Math.abs(carPosition.x) - Constants.PITCHWIDTH);
//		double Y = Math.abs(Math.abs(carPosition.y) - Constants.PITCHLENGTH);
//		double Z = Math.min(Math.abs(Math.abs(carPosition.z) - Constants.PITCHWIDTH), Math.abs(carPosition.z));
//		
//		return Math.min(X, Math.min(Y, Z));
//	}

}
