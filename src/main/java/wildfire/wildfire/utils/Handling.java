package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class Handling {

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
				.withSlide(!input.car.isDrifting() && input.car.orientation.noseVector.z > -0.2 && input.car.velocity.magnitude() > 900);
	}

	/**
	 * ATBA controller (no wiggle)
	 */
	public static ControlsOutput driveBall(DataPacket input){
		return new ControlsOutput().withSteer((float)-aim(input.car, input.ball.position.flatten()) * 2.5F).withBoost(false).withThrottle(1);
	}

	public static ControlsOutput atba(DataPacket input, Vector3 target){
		return new ControlsOutput().withSteer((float)-Math.signum(aim(input.car, target.flatten()))).withBoost(false).withSlide(false).withThrottle(1);
	}
	
	public static boolean insideTurningRadius(CarData car, Vector2 point){
    	double turningRadius = Physics.getTurnRadius(car.velocity.flatten().magnitude());
    	Vector2 right = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten();
    	Vector2 left = car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten();
    	return Math.min(point.distance(right), point.distance(left)) < turningRadius;
	}
	
	public static ControlsOutput drivePoint(DataPacket input, Vector2 point, boolean rush){
		float steer = (float)Handling.aim(input.car, point);
		
		float throttle = (rush ? 1 : (float)Math.signum(Math.cos(steer)));
		double distance = input.car.position.distanceFlat(point);
		
		boolean reverse = (throttle < 0);
		if(reverse) steer = (float)-Utils.invertAim(steer);
		
		return new ControlsOutput().withThrottle(throttle)
				.withBoost(!reverse && Math.abs(steer) < 0.325 && !input.car.isSupersonic && distance > (rush ? 1200 : 2000))
				.withSteer(-steer * 3F).withSlide(rush && Math.abs(steer) > Math.PI * 0.5);
	}
	
	public static ControlsOutput stayStill(DataPacket input){
		return new ControlsOutput().withThrottle((float)-input.car.forwardMagnitude() / 2500).withBoost(false);
	}
	
	public static double aimLocally(CarData car, Vector3 point){
		return new Vector2(0, 1).correctionAngle(Utils.toLocal(car, point).flatten());
	}
	
	public static double aimLocally(CarData car, Vector2 point){
		return aimLocally(car, point.withZ(0));
	}

}
