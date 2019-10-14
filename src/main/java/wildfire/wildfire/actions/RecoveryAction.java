package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.pitch.Pitch;
import wildfire.wildfire.pitch.Triangle;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class RecoveryAction extends Action {

	private final double fallStep = (1D / 120), zToBoost = 0.8;
	private final int fallDepth = (int)(2.6 / fallStep), // It only takes around 2.5 seconds to fall from the ceiling, so this is a safe bet
			fallStepIndex = 20;

	private boolean boostDown;

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);
		this.boostDown = false;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		boolean canBoostDown = (input.car.orientation.forward.dotProduct(Vector3.Z.scaled(-1)) > zToBoost && input.car.position.z > 150);

		Vector3[] fall = simulateFall(boostDown ? (canBoostDown ? Color.RED : Color.YELLOW) : Color.WHITE, input.car.position, input.car.velocity);

		Pair<Triangle, Slice> intersect = getIntersect(fall);
		Triangle triangle = (intersect == null ? null : intersect.getOne());
		Vector3 triangleCentre = (triangle == null ? null : triangle.getCentre());
		Vector3 intersectLocation = (intersect == null ? null : intersect.getTwo().getPosition());
		double intersectTime = (intersect == null ? 0 : intersect.getTwo().getFrame() * fallStep);
		wildfire.renderer.drawString2d("Est. Time: " + Utils.round(intersectTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);

		// whatisaphone's Secret Recipe.
		boostDown = (input.car.boost >= 1 && (intersect == null || (intersectTime > 0.6 && intersectLocation.z < Constants.CEILING - 500)));

		boolean planWavedash = (!input.car.hasDoubleJumped && !boostDown && input.car.velocity.z < -420 && input.car.orientation.up.z > 0.8
				&& (intersect != null && intersectLocation.z < 10));
		wildfire.renderer.drawString2d("Plan Wavedash: " + planWavedash, Color.WHITE, new Point(0, 60), 2, 2);

		Vector3 desiredRoof;
		if(triangle != null && !planWavedash){
			Vector3 fallNormal = triangle.getNormal().scaled(-1);
			desiredRoof = fallNormal;
		}else{
			desiredRoof = Vector3.Z;
		}

		Vector3 desiredNose;
		if(boostDown || (triangle != null && triangleCentre.z > 200 && intersectLocation.z < Constants.CEILING - 400)){
			// Aim down.
			desiredNose = Vector3.Z.scaled(-1);
//			desiredRoof = input.car.velocity;
		}else if(input.car.velocity.flatten().magnitude() > 800){
			desiredNose = input.car.velocity.withZ(0);
		}else{
			desiredNose = input.info.impact.getPosition().minus(input.car.position).withZ(0);
		}

		if(planWavedash){
			desiredNose = desiredNose.withZ(0);
			if(desiredNose.isZero()) desiredNose = input.car.velocity.withZ(0);
			desiredNose = desiredNose.normalised().withZ(0.3);
			if(input.car.position.z < 50){
				// Perform the wave-dash.
				return new ControlsOutput().withPitch(-1).withJump(true)
						.withYaw(0).withRoll(0);
			}
		}

		// Render the target orientations.
		wildfire.renderer.drawLine3d(Color.RED, input.car.position, input.car.position.plus(desiredNose.scaledToMagnitude(200)));
		wildfire.renderer.drawLine3d(Color.GREEN, input.car.position, input.car.position.plus(desiredRoof.scaledToMagnitude(200)));

		return new ControlsOutput().withPitchYawRoll(AirControl.getPitchYawRoll(input.car, desiredNose, desiredRoof))
				.withBoost(canBoostDown).withJump(false)
				.withThrottle(Math.abs(input.car.velocityDir(input.car.orientation.up)) < 400 && intersectTime < 0.2 ? 1 : 0); // Throttle to avoid turtling.
	}

	@Override
	public boolean expire(InfoPacket input){
		return input.car.hasWheelContact || input.gameInfo.isMatchEnded();
	}

	private Vector3[] simulateFall(Color colour, Vector3 position, Vector3 velocity){
		Vector3[] positions = new Vector3[fallDepth];
		positions[0] = position;
		return simulateFall(colour, positions, velocity, 1);
	}

	private Vector3[] simulateFall(Color colour, Vector3[] positions, Vector3 velocity, int depth){
		if(depth >= fallDepth) return positions;

		Vector3 position = positions[depth - 1];

		// Apply physics.
		velocity = velocity.plus(Vector3.Z.scaled(-Constants.GRAVITY * fallStep)); // Gravity.
		if(velocity.magnitude() > 2300) velocity = velocity.scaledToMagnitude(2300);
		Vector3 newPosition = position.plus(velocity.scaled(fallStep));

		wildfire.renderer.drawLine3d(colour, position, newPosition);
		positions[depth] = newPosition;
		return simulateFall(colour, positions, velocity, depth + 1);
	}

	private Pair<Triangle, Slice> getIntersect(Vector3[] fall){
		for(int i = 0; i < fall.length; i += fallStepIndex){
			int j = Math.min(fall.length - 1, i + fallStepIndex);
			Pair<Vector3, Vector3> lineSegment = new Pair<Vector3, Vector3>(fall[i], fall[j]);
			Pair<Triangle, Vector3> intersect = intersect(lineSegment.getOne(), lineSegment.getTwo());

			// Intersected.
			if(intersect != null){
				return new Pair<Triangle, Slice>(intersect.getOne(),
						new Slice(intersect.getTwo(), Utils.lerp(i, j, Utils.pointLineSegmentT(intersect.getTwo(), lineSegment)) * fallStep));
			}
		}
		return null;
	}

	private Pair<Triangle, Vector3> intersect(Vector3 segment1, Vector3 segment2){
		Pair<Triangle, Vector3> intersect = Pitch.segmentIntersect(segment1, segment2);
		if(intersect == null) return null;
		
		Triangle triangle = intersect.getOne();

		wildfire.renderer.drawTriangle(Color.GREEN, triangle);
//		wildfire.renderer.drawFlatSquare(Color.GREEN, intersect.getTwo(), triangle.getVector(1).minus(triangle.getVector(2)), triangle.getNormal(), 500);
		
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
