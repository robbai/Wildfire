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
		return aim(car, point.withZ(Constants.CARHEIGHT));
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
	
	public static ControlsOutput arriveDestination(CarData car, Vector2 point, boolean rush){
		double steer = Handling.aim(car, point);
		
		double throttle = (rush ? 1 : Math.signum(Math.cos(steer)));
		
		boolean reverse = (throttle < 0);
		if(reverse) steer = -Utils.invertAim(steer);
		
		double distance = car.position.distanceFlat(point);
		double velocity = car.velocity.magnitude();
		
		return new ControlsOutput().withThrottle(throttle)
				.withBoost(!reverse && Math.abs(steer) < 0.2 && !car.isSupersonic && distance > (rush ? 1200 : 2000))
				.withSteer(-steer * 3F).withSlide(rush && Math.abs(steer) > Math.PI * 0.3 && Handling.canHandbrake(car) && velocity > 600);
	}
	
	public static ControlsOutput driveDestination(CarData car, Vector3 destination){
		double velocityForward = car.forwardVelocity;
		double radians = aim(car, destination);
		
		return new ControlsOutput().withThrottle(1)
				.withBoost(Math.abs(radians) < 0.2 && !car.isSupersonic)
				.withSteer(-radians * 3)
				.withSlide(Math.abs(radians) > 1.1 && Handling.canHandbrake(car) && Math.abs(velocityForward) > 500);
	}
	
	public static ControlsOutput driveDestination(CarData car, Vector2 destination){
		return driveDestination(car, destination.withZ(Constants.CARHEIGHT));
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
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, Slice candidate, WRenderer renderer){
		Vector3 carPosition = car.position;
		Vector3 ballPosition = candidate.getPosition();
		
		ControlsOutput controls = driveDestination(car, ballPosition);
		
		double jumpHeight = candidate.getPosition().minus(car.position).dotProduct(car.orientation.roofVector);
		double peakTime = JumpPhysics.getFastestTimeZ(jumpHeight);
		
		// Drive calculations.
		double driveTime = Math.max(0.00001, candidate.getTime() - peakTime - FollowSmartDodgeMechanic.earlyTime);
		double fullDistance = carPosition.distanceFlat(ballPosition);
		double initialVelocity = car.velocityDir(ballPosition.minus(carPosition).flatten());
		double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
		double acceleration = ((finalVelocity - initialVelocity) / driveTime);
		
		if(renderer != null){
			renderer.drawCrosshair(car, ballPosition, Color.RED, 70);
//			renderer.drawString2d("Value: " + Utils.round(jumpHeight) + ", " + Utils.round(peakTime), Color.WHITE, new Point(0, 40), 2, 2);
			renderer.drawString2d("Initial Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			renderer.drawString2d("Final Vel.: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			renderer.drawString2d("Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		
		// Controls.
//		if(Math.abs(controls.getSteer()) > 0.5) return controls;
		if(Math.abs(controls.getSteer()) > 0.5) return turnOnSpot(car, ballPosition);
		if(acceleration > 0 && Behaviour.correctSideOfTarget(car, ballPosition)){
			double maxVel = DrivePhysics.maxVelocity(driveTime, initialVelocity, car.boost);
			if(maxVel > finalVelocity) acceleration /= Math.max(1, (maxVel - finalVelocity) / Utils.lerp(350, 800, finalVelocity / Constants.MAXCARSPEED));
		}
		double throttle = produceAcceleration(car, acceleration); 
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, Slice candidate){
		return arriveAtSmartDodgeCandidate(car, candidate, null);
	}

	public static ControlsOutput turnOnSpot(CarData car, Vector3 destination){
		ControlsOutput controls = driveDestination(car, destination);
		double targetVelocity = 600;
		double acceleration = (targetVelocity - car.forwardVelocity) / 0.05;
		double throttle = produceAcceleration(car, acceleration);
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static ControlsOutput turnOnSpot(CarData car, Vector2 destination){
		return turnOnSpot(car, destination.withZ(Constants.CARHEIGHT));
	}
	
	public static boolean canHandbrake(CarData car){
		Vector2 localVel = Utils.toLocalFromRelative(car, car.velocity).flatten();
		if(localVel.isZero()) return false;
		return localVel.normalized().dotProduct(forwardVector) > 0.9 && localVel.magnitude() > 250;
	}

}
