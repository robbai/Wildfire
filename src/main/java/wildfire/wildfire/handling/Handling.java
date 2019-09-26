package wildfire.wildfire.handling;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.mechanics.FollowSmartDodgeMechanic;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Handling {

	private static Vector2 forwardVector = new Vector2(0, 1)/**, sideVector = new Vector2(1, 0)*/;
	
	public static double aim(CarData car, Vector3 point){
		return forwardVector.correctionAngle(Utils.toLocal(car, point).flatten());
	}
	
	public static double aim(CarData car, Vector2 point){
		return aim(car, point.withZ(Constants.CAR_HEIGHT));
	}

	public static double aimFromPoint(Vector2 carPosition, Vector2 carDirection, Vector2 point){
		return carDirection.correctionAngle(point.minus(carPosition));
	}

	public static ControlsOutput driveDownWall(DataPacket input){
		double radians = aim(input.car, input.car.position.plus(input.car.orientation.roofVector.scaledToMagnitude(70)).flatten());
		return new ControlsOutput().withThrottle(input.car.forwardVelocity > 500 && input.car.orientation.noseVector.z > 0.7 ? -1 : 1)
				.withBoost(input.car.forwardVelocity < 1600 && Math.abs(radians) < Math.toRadians(40) && input.car.position.z > 300)
				.withSteer(-3 * radians)
				.withSlide(input.car.velocity.magnitude() > 300 && Math.abs(radians) > Math.toRadians(70) && canHandbrake(input.car)); 
	}

	public static ControlsOutput atba(DataPacket input, Vector3 target){
		return new ControlsOutput().withSteer(-Math.signum(aim(input.car, target.flatten()))).withBoost(false).withSlide(false).withThrottle(1);
	}
	
	public static boolean insideTurningRadius(CarData car, Vector2 point){
    	double turningRadius = DrivePhysics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector2 right = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten();
    	Vector2 left = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten();
    	return Math.min(point.distance(right), point.distance(left)) < turningRadius;
	}
	
	public static boolean insideTurningRadius(CarData car, Vector3 point){
    	double turningRadius = DrivePhysics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector3 localPoint = Utils.toLocal(car, point);
    	Vector3 right = car.orientation.rightVector.scaledToMagnitude(turningRadius);
    	Vector3 left = car.orientation.rightVector.scaledToMagnitude(-turningRadius);
    	return Math.min(localPoint.distance(right), localPoint.distance(left)) < turningRadius;
	}
	
	public static ControlsOutput chaosDrive(CarData car, Vector3 destination, boolean rush){
		boolean reverse = (Math.cos(Handling.aim(car, destination)) < 0);
		
		ControlsOutput controls = (reverse ? steeringBackwards(car, destination) : steering(car, destination));
		
		double distance = car.position.distanceFlat(destination);
		
		return controls.withThrottle(reverse ? -1 : 1)
				.withBoost(controls.holdBoost() && distance > (rush ? 1200 : 2000))
				.withSlide(controls.holdHandbrake() && rush);
	}
	
	public static ControlsOutput chaosDrive(CarData car, Vector2 destination, boolean rush){
		return chaosDrive(car, destination.withZ(Constants.CAR_HEIGHT), rush);
	}
	
	public static ControlsOutput forwardDrive(CarData car, Vector3 destination){
		ControlsOutput controls = steering(car, destination);
		return controls.withThrottle(1);
	}
	
	public static ControlsOutput forwardDrive(CarData car, Vector2 destination){
		return forwardDrive(car, destination.withZ(Constants.CAR_HEIGHT));
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
		double throttleAcceleration = DrivePhysics.determineAcceleration(velocityForward, 1, false);
		
		double brakeCoastTransition = -0.45 * Constants.BRAKE_ACCELERATION - 0.55 * Constants.COAST_ACCELERATION;
		double coastingThrottleTransition = -0.5 * Constants.COAST_ACCELERATION;
		double throttleBoostTransition = throttleAcceleration + 0.5 * Constants.BOOST_GROUND_ACCELERATION;
		
		// Sliding down the wall.
		if(car.orientation.roofVector.z < 0.7){
			brakeCoastTransition = -0.5 * Constants.BRAKE_ACCELERATION;
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
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, Slice candidate, WRenderer renderer){
		Vector3 carPosition = car.position;
		Vector3 ballPosition = candidate.getPosition();
		
		ControlsOutput controls = forwardDrive(car, ballPosition);
		
		double jumpHeight = candidate.getPosition().minus(car.position).dotProduct(car.orientation.roofVector);
		double peakTime = JumpPhysics.getFastestTimeZ(jumpHeight);
		
		// Drive calculations.
		double driveTime = Math.max(0.00001, candidate.getTime() - peakTime - FollowSmartDodgeMechanic.earlyTime);
		double fullDistance = carPosition.distanceFlat(ballPosition);
		double initialVelocity = car.velocityDir(ballPosition.minus(carPosition).flatten());
		double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
		double acceleration = ((finalVelocity - initialVelocity) / driveTime);
		
		// Render.
		if(renderer != null){
			renderer.drawCrosshair(car, ballPosition, Color.RED, 70);
//			renderer.drawString2d("Value: " + Utils.round(jumpHeight) + ", " + Utils.round(peakTime), Color.WHITE, new Point(0, 40), 2, 2);
			renderer.drawString2d("Initial Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			renderer.drawString2d("Final Vel.: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			renderer.drawString2d("Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		
		// Controls.
		if(Math.abs(controls.getSteer()) > 0.5) return turnOnSpot(car, ballPosition);
		if(acceleration > 0 && Behaviour.correctSideOfTarget(car, ballPosition)){
			double maxVel = DrivePhysics.maxVelocity(driveTime, initialVelocity, car.boost);
			if(maxVel > finalVelocity) acceleration /= Math.max(1, (maxVel - finalVelocity) / Utils.lerp(350, 600, finalVelocity / Constants.MAX_CAR_VELOCITY));
		}
		double throttle = produceAcceleration(car, acceleration); 
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, Slice candidate){
		return arriveAtSmartDodgeCandidate(car, candidate, null);
	}

	public static ControlsOutput turnOnSpot(CarData car, Vector3 destination){
		ControlsOutput controls = forwardDrive(car, destination);
		double targetVelocity = 600;
		double acceleration = (targetVelocity - car.forwardVelocity) / 0.05;
		double throttle = produceAcceleration(car, acceleration);
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static ControlsOutput turnOnSpot(CarData car, Vector2 destination){
		return turnOnSpot(car, destination.withZ(Constants.CAR_HEIGHT));
	}
	
	public static boolean canHandbrake(CarData car){
		Vector2 localVel = Utils.toLocalFromRelative(car, car.velocity).flatten();
		if(localVel.isZero()) return false;
		return localVel.normalized().dotProduct(forwardVector) > 0.9 && localVel.magnitude() > 250;
	}
	
	public static ControlsOutput steering(CarData car, Vector3 destination){
		double yawError = Handling.aim(car, destination);
		double yawVelocity = car.angularVelocity.dotProduct(car.orientation.roofVector);
		
		ControlsOutput controls = steeringBlind(yawError, yawVelocity);
		return controls
				.withBoost(controls.holdBoost() && car.hasWheelContact && !car.isSupersonic)
				.withSlide(controls.holdHandbrake() && car.hasWheelContact && canHandbrake(car));
	}
	
	public static ControlsOutput steering(CarData car, Vector2 destination){
		return steering(car, destination.withZ(Constants.CAR_HEIGHT));
	}
	
	public static ControlsOutput steeringBackwards(CarData car, Vector3 destination){
		double yawError = -Utils.invertAim(Handling.aim(car, destination));
		double yawVelocity = -car.angularVelocity.dotProduct(car.orientation.roofVector);
		
		ControlsOutput controls = steeringBlind(yawError, yawVelocity);
		return controls
				.withBoost(false)
				.withSlide(controls.holdHandbrake() && car.hasWheelContact && canHandbrake(car));
	}
	
	public static ControlsOutput steeringBackwards(CarData car, Vector2 destination){
		return steeringBackwards(car, destination.withZ(Constants.CAR_HEIGHT));
	}
	
	public static ControlsOutput steeringBlind(double yawError, double yawVelocity){
		double steer;
		if(Math.abs(yawError) > Math.toRadians(30)){
			steer = -Math.signum(yawError);
		}else{
			steer = (-yawError * 6 - yawVelocity * 0.25);
		}
		
		return new ControlsOutput().withSteer(steer)
				.withBoost(Math.abs(yawError) < Math.toRadians(15))
				.withSlide(Math.abs(yawError) > Math.toRadians(55) && yawError * steer < 0);
	}

}
