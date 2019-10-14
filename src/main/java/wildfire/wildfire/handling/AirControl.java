package wildfire.wildfire.handling;

import wildfire.input.car.CarData;
import wildfire.input.car.CarOrientation;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class AirControl {

	/**
	 * https://github.com/DomNomNom/RocketBot/blob/32e69df4f2841501c5f1da97ce34673dccb670af/NomBot_v1.5/NomBot_v1_5.py#L56-L103
	 */
	public static double[] getPitchYawRoll(CarData car, Vector3 forward, Vector3 up){
		CarOrientation orient = car.orientation.step(1D / 60, car.angularVelocity);
		//		CarOrientation orient = car.orientation;

		forward = forward.normalised();
		up = up.normalised();

		Vector3 desiredFacingAngVel = orient.forward.crossProduct(forward);
		Vector3 desiredUpVel = orient.up.crossProduct(up);

		double pitch = desiredFacingAngVel.dotProduct(orient.right);
		double yaw = -desiredFacingAngVel.dotProduct(orient.up);
		double roll = desiredUpVel.dotProduct(orient.forward);

		double pitchVel = car.angularVelocity.pitch;
		double yawVel = -car.angularVelocity.yaw;
		double rollVel = car.angularVelocity.roll;

		// Avoid getting stuck in directly-opposite states
		if(orient.up.dotProduct(up) < -0.8 && orient.forward.dotProduct(forward) > 0.8){
			if(roll == 0) roll = 1;
			roll *= 1e10;
		}
		if(orient.forward.dotProduct(forward) < -0.8){
			if(pitch == 0) pitch = 1;
			pitch *= 1e10;
		}

		//	    if(orient.forward.dotProduct(forward) < 0){
		//	    	pitchVel *= -1;
		//	    }

		// PID control to stop overshooting.
		roll = 3 * roll + 0.30 * rollVel;
		yaw = 3 * yaw + 0.70 * yawVel;
		pitch = 3 * pitch + 0.90 * pitchVel;

		// Only start adjusting roll once we're roughly facing the right way.
		if(orient.forward.dotProduct(forward) < 0){
			roll = 0;
		}

		//	    return new double[] {pitch, yaw, roll};
		final double threshold = 0.14;
		return new double[] {
				Math.abs(pitch) > threshold ? Math.signum(pitch) : pitch, 
						Math.abs(yaw) > threshold ? Math.signum(yaw) : yaw, 
								Math.abs(roll) > threshold ? Math.signum(roll) : roll
		};
	}

	public static double[] getPitchYawRoll(CarData car, Vector2 forward, Vector3 up){
		double upZ = up.normalised().z;
		return getPitchYawRoll(car, forward.normalised().withZ(Math.max(1 - upZ, -1 + upZ)), up);
	}

	public static double[] getPitchYawRoll(CarData car, Vector2 forward){
		return getPitchYawRoll(car, forward, Vector3.Z);
	}

	public static double[] getPitchYawRoll(CarData car, Vector3 forward){
		return getPitchYawRoll(car, forward, Vector3.Z);
	}

}

