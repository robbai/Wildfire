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
		return new ControlsOutput().withThrottle(1).withBoost(false).withSteer((float)(Math.signum(input.car.orientation.eularRoll) * Math.abs(Math.cos(input.car.orientation.noseVector.z))))
				.withSlide(!input.car.isDrifting() && input.car.orientation.noseVector.z > -0.2);
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

}
