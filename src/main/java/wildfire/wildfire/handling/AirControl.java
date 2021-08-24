package wildfire.wildfire.handling;

import wildfire.input.Rotator;
import wildfire.input.car.CarData;
import wildfire.input.car.CarOrientation;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.utils.Utils;

public class AirControl {

	private static boolean signum = false;

	/**
	 * https://github.com/DomNomNom/RocketBot/blob/32e69df4f2841501c5f1da97ce34673dccb670af/NomBot_v1.5/NomBot_v1_5.py#L56-L103
	 */
	public static double[] getPitchYawRoll(CarOrientation orientation, Rotator angularVelocity, Vector3 forward,
			Vector3 up){
		double pitchVel = angularVelocity.pitch;
		double yawVel = -angularVelocity.yaw;
		double rollVel = angularVelocity.roll;

		forward = forward.normalised();
		up = up.normalised();

		Vector3 desiredFacingAngVel = orientation.forward.crossProduct(forward);
		Vector3 desiredUpVel = orientation.up.crossProduct(up);

		double pitch = desiredFacingAngVel.dotProduct(orientation.right);
		double yaw = -desiredFacingAngVel.dotProduct(orientation.up);
		double roll = desiredUpVel.dotProduct(orientation.forward);

		// Avoid getting stuck in directly-opposite states
		if(orientation.up.dotProduct(up) < -0.8 && orientation.forward.dotProduct(forward) > 0.8){
			if(roll == 0)
				roll = 1;
			roll *= 1e10;
		}
		if(orientation.forward.dotProduct(forward) < -0.8){
			if(pitch == 0)
				pitch = 1;
			pitch *= 1e10;
		}

		// if(orient.forward.dotProduct(forward) < 0){
		// pitchVel *= -1;
		// }

		// PID control to stop overshooting.
		roll = 3 * roll + 0.30 * rollVel;
		yaw = 3 * yaw + 0.70 * yawVel;
		pitch = 3 * pitch + 0.90 * pitchVel;

		// Only start adjusting roll once we're roughly facing the right way.
		if(orientation.forward.dotProduct(forward) < 0){
			roll = 0;
		}

		if(signum){
			final double threshold = 0.14;
			return new double[] { Math.abs(pitch) > threshold ? Math.signum(pitch) : Utils.clamp(pitch, -1, 1),
					Math.abs(yaw) > threshold ? Math.signum(yaw) : Utils.clamp(yaw, -1, 1),
					Math.abs(roll) > threshold ? Math.signum(roll) : Utils.clamp(roll, -1, 1) };
		}else{
			return new double[] { pitch, yaw, roll };
		}
	}

	public static double[] getPitchYawRoll(CarData car, Vector3 forward, Vector3 up){
		return getPitchYawRoll(car.orientation, car.angularVelocity, forward, up);
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
