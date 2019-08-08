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

	private static Vector2 forwardVector = new Vector2(0, 1)/**, sideVector = new Vector2(1, 0)*/;
	
	public static double aim(CarData car, Vector3 point){
		return forwardVector.correctionAngle(Utils.toLocal(car, point).flatten());
	}
	
	public static double aim(CarData car, Vector2 point){
		return aim(car, point.withZ(Constants.CARHEIGHT));
	}

	public static double aimFromPoint(Vector2 carPosition, Vector2 carDirection, Vector2 point){
		return carDirection.correctionAngle(point.minus(carPosition));
	}

	public static ControlsOutput driveDownWall(DataPacket input){
		double radians = aim(input.car, input.car.position.plus(input.car.orientation.roofVector.scaledToMagnitude(70)).flatten());
		return new ControlsOutput().withThrottle(1).withBoost(false)
				.withSteer(-3 * radians)
				.withSlide(Handling.canHandbrake(input.car) && Math.abs(radians) > Math.toDegrees(60)); // && input.car.velocity.magnitude() > 500
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
	
	public static boolean insideTurningRadius(CarData car, Vector3 point){
    	double turningRadius = Physics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector3 localPoint = Utils.toLocal(car, point);
    	Vector3 right = car.orientation.rightVector.scaledToMagnitude(turningRadius);
    	Vector3 left = car.orientation.rightVector.scaledToMagnitude(-turningRadius);
    	return Math.min(localPoint.distance(right), localPoint.distance(left)) < turningRadius;
	}
	
	public static ControlsOutput drivePoint(DataPacket input, Vector2 point, boolean rush){
		double steer = Handling.aim(input.car, point);
		
		double throttle = (rush ? 1 : Math.signum(Math.cos(steer)));
		
		boolean reverse = (throttle < 0);
		if(reverse) steer = (float)-Utils.invertAim(steer);
		
		double distance = input.car.position.distanceFlat(point);
		double velocity = input.car.velocity.magnitude();
		
		return new ControlsOutput().withThrottle(throttle)
				.withBoost(!reverse && Math.abs(steer) < 0.325 && !input.car.isSupersonic && distance > (rush ? 1200 : 2000))
				.withSteer(-steer * 3F).withSlide(rush && Math.abs(steer) > Math.PI * 0.3 && Handling.canHandbrake(input.car) && velocity > 600);
	}
	
	public static ControlsOutput driveDestination(CarData car, Vector3 destination){
		double velocityForward = car.forwardVelocity;
		double steer = aim(car, destination);
		
		return new ControlsOutput().withThrottle(1)
				.withBoost(Math.abs(steer) < 0.25 && !car.isSupersonic)
				.withSteer(-steer * 3F)
				.withSlide(Math.abs(steer) > 1.1 && velocityForward > 400 && Handling.canHandbrake(car));
	}
	
	public static ControlsOutput stayStill(CarData car){
		return new ControlsOutput().withThrottle(Handling.produceAcceleration(car, car.forwardVelocity * -60)).withBoost(false);
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
		double velocityForward = car.forwardVelocity;
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
		double initialVelocity = car.velocityDir(ballPosition.minus(carPosition).flatten());
		// full distance - peak time * final velocity = 0.5 * drive time * (initial velocity + final velocity)
		double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
		double acceleration = (finalVelocity - initialVelocity) / Math.max(0.00001, driveTime);
//		double driveDistance = fullDistance - peakTime * finalVelocity;
		
		if(renderer != null){
			renderer.drawCrosshair(car, ballPosition, Color.RED, 70);
			renderer.drawString2d("Initial Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			renderer.drawString2d("Final Vel.: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			renderer.drawString2d("Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		
		// Controls.
//		if(Math.abs(controls.getSteer()) > 0.5) return controls;
		if(Math.abs(controls.getSteer()) > 0.5) return turnOnSpot(car, ballPosition);
		if(acceleration > 0 && Behaviour.correctSideOfTarget(car, ballPosition)){
			double maxVel = Physics.maxVelocity(driveTime, initialVelocity, car.boost);
			if(maxVel > finalVelocity) acceleration /= Math.max(1, (maxVel - finalVelocity) / Utils.lerp(350, 800, finalVelocity / Constants.MAXCARSPEED));
		}
		double throttle = produceAcceleration(car, acceleration); 
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}

	private static ControlsOutput turnOnSpot(CarData car, Vector3 ballPosition){
		ControlsOutput controls = driveDestination(car, ballPosition);
		double targetVelocity = 600;
		double acceleration = (targetVelocity - car.forwardVelocity) / 0.05;
		double throttle = produceAcceleration(car, acceleration);
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static boolean canHandbrake(CarData car){
		Vector2 localVel = Utils.toLocalFromRelative(car, car.velocity).flatten();
		if(localVel.isZero()) return false;
		return localVel.normalized().dotProduct(forwardVector) > 0.9 && localVel.magnitude() > 250;
	}

}
