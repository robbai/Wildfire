package wildfire.wildfire.utils;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.WRenderer;

public class Handling {

	private static Vector2 forwardVector = new Vector2(0, 1);
//	private static Vector3 forwardVector3 = new Vector3(0, 1, 0);

	public static double aim(CarData car, Vector2 point){
		Vector2 carPosition = car.position.flatten();
	    Vector2 carDirection = car.orientation.noseVector.flatten();
		return carDirection.correctionAngle(point.minus(carPosition));
	}

	public static double aimFromPoint(Vector2 carPosition, Vector2 carDirection, Vector2 point){
		return carDirection.correctionAngle(point.minus(carPosition));
	}

	/**
	 * Inspired by the wonderful Darxeal
	 */
	public static ControlsOutput driveDownWall(DataPacket input){
		return new ControlsOutput().withThrottle(1).withBoost(false)
				.withSteer((float)(-3F * aimLocally(input.car, 
						input.car.position.plus(input.car.orientation.roofVector.scaledToMagnitude(50)).flatten())))
				.withSlide(!input.car.isDrifting() && input.car.orientation.noseVector.z > -0.3 && input.car.velocity.magnitude() > 500);
	}

	/**
	 * ATBA controller (no wiggle)
	 */
	public static ControlsOutput driveBall(DataPacket input){
		return new ControlsOutput().withSteer(aim(input.car, input.ball.position.flatten()) * -3).withBoost(false).withSlide(false).withThrottle(1);
	}

	public static ControlsOutput atba(DataPacket input, Vector3 target){
		return new ControlsOutput().withSteer(-Math.signum(aim(input.car, target.flatten()))).withBoost(false).withSlide(false).withThrottle(1);
	}
	
	public static boolean insideTurningRadius(CarData car, Vector2 point){
    	double turningRadius = Physics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector2 right = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten();
    	Vector2 left = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten();
    	return Math.min(point.distance(right), point.distance(left)) < turningRadius;
	}
	
	public static ControlsOutput drivePoint(DataPacket input, Vector2 point, boolean rush){
		double steer = Handling.aimLocally(input.car, point);
		
		double throttle = (rush ? 1 : Math.signum(Math.cos(steer)));
		
		boolean reverse = (throttle < 0);
		if(reverse) steer = (float)-Utils.invertAim(steer);
		
		double distance = input.car.position.distanceFlat(point);
		double velocity = input.car.velocity.magnitude();
		
		return new ControlsOutput().withThrottle(throttle)
				.withBoost(!reverse && Math.abs(steer) < 0.325 && !input.car.isSupersonic && distance > (rush ? 1200 : 2000))
				.withSteer(-steer * 3F).withSlide(rush && Math.abs(steer) > Math.PI * 0.3 && !input.car.isDrifting() && velocity > 600);
	}
	
	public static ControlsOutput driveDestination(CarData car, Vector3 destination){
		double velocityForward = car.forwardMagnitude();
		double steer = aimLocally(car, destination);
		
		return new ControlsOutput().withThrottle(1)
				.withBoost(Math.abs(steer) < 0.25 && !car.isSupersonic)
				.withSteer(-steer * 3F)
				.withSlide(Math.abs(steer) > 1.1 && velocityForward > 400 && !car.isDrifting());
	}
	
	public static ControlsOutput stayStill(CarData car){
		return new ControlsOutput().withThrottle(Handling.produceAcceleration(car, car.forwardMagnitude() * -60)).withBoost(false);
	}
	
	public static double aimLocally(CarData car, Vector3 point){
		return forwardVector.correctionAngle(Utils.toLocal(car, point).flatten());
	}
	
	public static double aimLocally(CarData car, Vector2 point){
		return aimLocally(car, point.withZ(0));
	}
	
	/**
	 * Yaw, pitch
	 */
	public static Vector2 getAngles(Vector3 target){
		double yaw = Utils.wrapAngle(Math.atan2(target.x, target.y));
		double pitch = Utils.wrapAngle(Math.atan2(target.z, target.y));
		
		return new Vector2(yaw, pitch);
	}

	/**
	 * Roll, pitch
	 */
	public static Vector2 getAnglesRoof(Vector3 target){
		double roll = Utils.wrapAngle(Math.atan2(target.x, target.z));
		double pitch = Utils.wrapAngle(Math.atan2(-target.y, target.z));

		return new Vector2(roll, pitch);
	}
	
	/*
	 * https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L112-L160
	 */
	public static double produceAcceleration(CarData car, double acceleration){
		double velocityForward = car.forwardMagnitude();
		double throttleAcceleration = Physics.determineAcceleration(velocityForward, 1, false);
		
		double brakeCoastTransition = -0.45 * Constants.BRAKEACC - 0.55 * Constants.COASTACC;
		double coastingThrottleTransition = -0.5 * Constants.COASTACC;
		double throttleBoostTransition = throttleAcceleration + 0.5 * Constants.BOOSTACC;
		
		// Sliding down the wall.
		if(car.orientation.roofVector.z < 0.7){
			brakeCoastTransition = -0.5 * Constants.BRAKEACC;
			coastingThrottleTransition = brakeCoastTransition;
		}
		
		if(acceleration <= brakeCoastTransition){
			return -1; // Brake.
		}else if(brakeCoastTransition < acceleration && acceleration < coastingThrottleTransition){
			return 0; // Coast.
		}else if(coastingThrottleTransition <= acceleration && acceleration <= throttleBoostTransition){
			return Utils.clamp(acceleration / throttleAcceleration, 0.02, 1); // Throttle.
		}else if(throttleBoostTransition < acceleration){
			return 10; // Boost.
		}
		return 0.02;
	}
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, PredictionSlice candidate, WRenderer renderer){
		Vector3 carPosition = car.position;
		Vector3 ballPosition = candidate.getPosition();
		
		ControlsOutput controls = driveDestination(car, ballPosition);
		
		// Jump constants.
		double gravity = -Constants.GRAVITY;
		double jumpVelocity = SmartDodgeAction.jumpVelocity;
		
		// Jump calculations.
		double jumpHeight = Math.min(SmartDodgeAction.maxJumpHeight - 1, ballPosition.z - car.position.z);
		double peakTime = (-jumpVelocity + Math.sqrt(Math.pow(jumpVelocity, 2) + 2 * gravity * jumpHeight)) / gravity; // https://math.stackexchange.com/questions/2119238/rearrange-equation-of-motion-for-time
		
		// Drive.
		double driveTime = candidate.getTime() - peakTime;
		double fullDistance = carPosition.distanceFlat(ballPosition);
		double initialVelocity = car.magnitudeInDirection(ballPosition.minus(carPosition).flatten());
		// full distance - peak time * final velocity = 0.5 * drive time * (initial velocity + final velocity)
		double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
		double acceleration = (finalVelocity - initialVelocity) / driveTime;
		
		if(renderer != null){
			renderer.drawCrosshair(car, ballPosition, Color.RED, 70);
			renderer.drawString2d("Initial Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			renderer.drawString2d("Final Vel.: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			renderer.drawString2d("Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		
		// Controls.
		double driveDistance = fullDistance - peakTime * finalVelocity;
		if(Math.abs(controls.getSteer()) > 0.8){
			return controls;
		}else if(!car.isSupersonic && (driveDistance < 0 
				|| finalVelocity < Physics.maxVelocity(initialVelocity, car.boost, driveTime) - 200 / Math.max(driveTime, 0.0001)
				)){
			return stayStill(car);
		}
		double throttle = produceAcceleration(car, acceleration); 
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}

}
