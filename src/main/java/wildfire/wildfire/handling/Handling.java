package wildfire.wildfire.handling;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Handling {
	
	public static double aim(CarData car, Vector3 point){
		return Vector2.Y.correctionAngle(Utils.toLocal(car, point).flatten());
	}
	
	public static double aim(CarData car, Vector2 point){
		return aim(car, point.withZ(Constants.RIPPER_RESTING));
	}

	public static double aimFromPoint(Vector2 carPosition, Vector2 carDirection, Vector2 point){
		return carDirection.correctionAngle(point.minus(carPosition));
	}

	public static ControlsOutput driveDownWall(DataPacket input){
		double radians = aim(input.car, input.car.position.plus(input.car.orientation.up.scaledToMagnitude(70)).flatten());
		return new ControlsOutput()
				.withThrottle(1)
				.withBoost(input.car.forwardVelocity < 1600 && Math.abs(radians) < Math.toRadians(40) && input.car.position.z > 300)
				.withSteer(-4 * radians)
				.withSlide(input.car.forwardVelocityAbs > 500 && Math.abs(radians) > Math.toRadians(70) && canHandbrake(input.car) && input.car.velocity.normalised().dotProduct(Vector3.Z) < 0.4); 
	}

	public static ControlsOutput atba(DataPacket input, Vector3 target){
		return new ControlsOutput().withSteer(-Math.signum(aim(input.car, target.flatten()))).withBoost(false).withSlide(false).withThrottle(1);
	}
	
	public static boolean insideTurningRadius(CarData car, Vector2 point){
    	double turningRadius = DrivePhysics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector2 right = car.position.plus(car.orientation.right.withZ(0).scaledToMagnitude(turningRadius)).flatten();
    	Vector2 left = car.position.plus(car.orientation.right.withZ(0).scaledToMagnitude(-turningRadius)).flatten();
    	return Math.min(point.distance(right), point.distance(left)) < turningRadius;
	}
	
	public static boolean insideTurningRadius(CarData car, Vector3 point){
    	double turningRadius = DrivePhysics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector3 localPoint = Utils.toLocal(car, point);
    	Vector3 right = car.orientation.right.scaledToMagnitude(turningRadius);
    	Vector3 left = car.orientation.right.scaledToMagnitude(-turningRadius);
    	return Math.min(localPoint.distance(right), localPoint.distance(left)) < turningRadius;
	}
	
	public static ControlsOutput chaosDrive(CarData car, Vector3 destination, boolean rush){
		boolean reverse = (Math.cos(Handling.aim(car, destination)) < 0);
		
		ControlsOutput controls = (reverse ? steeringBackwards(car, destination) : steering(car, destination));
		
		double distance = car.position.distanceFlat(destination);
		
		return controls.withThrottle(reverse ? -1 : 1)
				.withBoost(controls.holdBoost() && distance > (rush ? 0 : 1500))
				.withSlide(controls.holdHandbrake() && rush);
	}
	
	public static ControlsOutput chaosDrive(CarData car, Vector2 destination, boolean rush){
		return chaosDrive(car, destination.withZ(Constants.RIPPER_RESTING), rush);
	}
	
	public static ControlsOutput forwardDrive(CarData car, Vector3 destination){
//		// Power-turn.
//		if(car.forwardVelocity < 450){
//			double angle = Handling.aim(car, destination);
//			if(Math.abs(angle) > Math.toRadians(car.forwardVelocity < 0 ? 70 : 90)){
//				angle = Utils.invertAim(angle);
//				return new ControlsOutput().withBoost(false).withThrottle(-1).withSlide(true).withSteer(-Math.signum(angle));
//			}
//		}
		
		ControlsOutput controls = steering(car, destination);
		return controls.withThrottle(1);
	}
	
	public static ControlsOutput forwardDrive(CarData car, Vector2 destination){
		return forwardDrive(car, destination.withZ(Constants.RIPPER_RESTING));
	}
	
	public static ControlsOutput backwardDrive(CarData car, Vector3 destination){
		ControlsOutput controls = steeringBackwards(car, destination);
		return controls.withThrottle(-1);
	}
	
	public static ControlsOutput backwardDrive(CarData car, Vector2 destination){
		return backwardDrive(car, destination.withZ(Constants.RIPPER_RESTING));
	}
	
	public static ControlsOutput stayStill(CarData car){
		return new ControlsOutput().withThrottle(Handling.produceAcceleration(car, car.forwardVelocity * -60)).withBoost(false);
	}
	
	/*
	 * https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L112-L160
	 */
	public static double produceAcceleration(CarData car, double acceleration){
		acceleration = Math.signum(acceleration) * Math.min(Math.abs(acceleration), Constants.BRAKE_ACCELERATION + Constants.BOOST_GROUND_ACCELERATION);
		
		double velocityForward = car.forwardVelocity;
		double throttleAcceleration = DrivePhysics.determineAcceleration(velocityForward, 1, false);
		
		double brakeCoastTransition = -0.45 * Constants.BRAKE_ACCELERATION - 0.55 * Constants.COAST_ACCELERATION;
		double coastingThrottleTransition = -0.5 * Constants.COAST_ACCELERATION;
		double throttleBoostTransition = throttleAcceleration + 0.5 * Constants.BOOST_GROUND_ACCELERATION;
		
		// Sliding down the wall.
		if(car.orientation.up.z < 0.7){
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
		
		double jumpHeight = candidate.getPosition().minus(car.position).dotProduct(car.orientation.up);
		double peakTime = JumpPhysics.getFastestTimeZ(jumpHeight);
		
		// Drive calculations.
		double driveTime = Math.max(0, candidate.getTime() - peakTime);
		double fullDistance = carPosition.distanceFlat(ballPosition);
		double initialVelocity = car.velocityDir(ballPosition.minus(carPosition).flatten());
		double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
		double acceleration = ((finalVelocity - initialVelocity) / driveTime);
		double maxVelForTurn = DrivePhysics.maxVelForTurn(car, ballPosition);
		if(Math.abs(finalVelocity) > maxVelForTurn){
			acceleration = (Math.copySign(maxVelForTurn, finalVelocity) - initialVelocity) / 0.1;
		}
		
		// Render.
		if(renderer != null){
			renderer.drawCrosshair(car, ballPosition, Color.RED, 70);
			renderer.drawString2d("Drive Time: " + Utils.round(driveTime) + "s, Peak Time: " + Utils.round(peakTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);
			renderer.drawString2d("Initial Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			renderer.drawString2d("Final Vel.: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			renderer.drawString2d("Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		
		// Controls.
		double radians = Handling.aim(car, ballPosition);
		if(Math.abs(radians) > Math.toRadians(15)){
			if(renderer != null) renderer.drawString2d("Turn", Color.WHITE, new Point(0, 120), 2, 2);
			return turnOnSpot(car, ballPosition);
		}
		if(fullDistance > 0/* && Behaviour.correctSideOfTarget(car, ballPosition)*/){
			double maxVel = DrivePhysics.maxVelocity(Math.max(0, driveTime - 3D / 120), 0, car.boost);
			if(maxVel > finalVelocity){
				acceleration /= Math.max(1, (maxVel - finalVelocity) / Utils.lerp(250, 550, finalVelocity / Constants.MAX_CAR_VELOCITY));
//				acceleration = -initialVelocity / 0.15;
			}
		}
		double throttle = produceAcceleration(car, acceleration); 
		return controls.withThrottle(throttle).withBoost(throttle > 1);
	}
	
	public static ControlsOutput arriveAtSmartDodgeCandidate(CarData car, Slice candidate){
		return arriveAtSmartDodgeCandidate(car, candidate, null);
	}

	public static ControlsOutput turnOnSpot(CarData car, Vector3 destination){
		ControlsOutput controls = forwardDrive(car, destination);
//		double targetVelocity = Math.abs(controls.getSteer()) * 500;
		double targetVelocity = 700 * Math.max(0.3, 1 - Math.abs(controls.getSteer()));
//		double initialVelocity = car.velocityDir(destination.minus(car.position));
		double initialVelocity = car.forwardVelocityAbs;
		double acceleration = (targetVelocity - initialVelocity) / 0.1;
		double throttle = produceAcceleration(car, acceleration);
		return controls.withThrottle(throttle).withBoost(throttle > 1); //.withSlide(Math.abs(controls.getSteer()) < 0.5);
	}
	
	public static ControlsOutput turnOnSpot(CarData car, Vector2 destination){
		return turnOnSpot(car, destination.withZ(Constants.RIPPER_RESTING));
	}
	
	public static boolean canHandbrake(CarData car){
		Vector2 localVel = Utils.toLocalFromRelative(car, car.velocity).flatten();
		if(localVel.isZero()) return false;
		return localVel.normalised().dotProduct(Vector2.Y) > 0.9 && localVel.magnitude() > 250;
	}
	
	public static ControlsOutput steering(CarData car, Vector3 destination){
		double yawError = Handling.aim(car, destination);
		double yawVelocity = car.angularVelocity.yaw;
		
		ControlsOutput controls = steeringBlind(yawError, yawVelocity);
		return controls
				.withBoost(controls.holdBoost() && car.hasWheelContact && !car.isSupersonic)
				.withSlide(controls.holdHandbrake() && car.hasWheelContact && canHandbrake(car));
	}
	
	public static ControlsOutput steering(CarData car, Vector2 destination){
		return steering(car, destination.withZ(Constants.RIPPER_RESTING));
	}
	
	public static ControlsOutput steeringBackwards(CarData car, Vector3 destination){
		double yawError = -Utils.invertAim(Handling.aim(car, destination));
		double yawVelocity = -car.angularVelocity.yaw;
		
		ControlsOutput controls = steeringBlind(yawError, yawVelocity);
		return controls
				.withBoost(false)
				.withSlide(controls.holdHandbrake() && car.hasWheelContact && canHandbrake(car) && car.forwardVelocity < 0);
	}
	
	public static ControlsOutput steeringBackwards(CarData car, Vector2 destination){
		return steeringBackwards(car, destination.withZ(Constants.RIPPER_RESTING));
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
