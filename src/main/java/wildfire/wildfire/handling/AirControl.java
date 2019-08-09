package wildfire.wildfire.handling;

import wildfire.input.CarData;
import wildfire.input.CarOrientation;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class AirControl {

	public static final Vector3 worldUp = new Vector3(0, 0, 1);

	/**
	 * https://github.com/DomNomNom/RocketBot/blob/32e69df4f2841501c5f1da97ce34673dccb670af/NomBot_v1.5/NomBot_v1_5.py#L56-L103
	 */
	public static double[] getPitchYawRoll(CarData car, Vector3 forward, Vector3 up){
		CarOrientation orient = car.orientation;
		Vector3 angVel = car.angularVelocity;
		
		forward = forward.normalized();
		up = up.normalized();
		
	    Vector3 desiredFacingAngVel = orient.noseVector.crossProduct(forward); //.scaled(-1);
	    Vector3 desiredUpVel = orient.roofVector.crossProduct(up); //.scaled(-1);

	    double pitch = desiredFacingAngVel.dotProduct(orient.rightVector);
	    double yaw = -desiredFacingAngVel.dotProduct(orient.roofVector);
	    double roll = desiredUpVel.dotProduct(orient.noseVector);

	    double pitchVel = angVel.dotProduct(orient.rightVector);
	    double yawVel = -angVel.dotProduct(orient.roofVector);
	    double rollVel = angVel.dotProduct(orient.noseVector);

	    // Avoid getting stuck in directly-opposite states
	    if(orient.roofVector.dotProduct(up) < -0.8 && orient.noseVector.dotProduct(forward) > 0.8){
	        if(roll == 0) roll = 1;
	        roll *= Math.pow(10, 10);
	    }
	    if(orient.noseVector.dotProduct(forward) < -0.8){
	    	if(pitch == 0) pitch = 1;
	    	pitch *= Math.pow(10, 10);
	    }

	    if(orient.noseVector.dotProduct(forward) < 0){
	    	pitchVel *= -1;
	    }

	    // PID control to stop overshooting.
	    roll = 3 * roll + 0.30 * rollVel;
	    yaw = 3 * yaw + 0.70 * yawVel;
	    pitch = 3 * pitch + 0.90 * pitchVel;

	    // Only start adjusting roll once we're roughly facing the right way.
	    if(orient.noseVector.dotProduct(forward) < 0){
	        roll = 0;
	    }

	    return new double[] {pitch, yaw, roll};
	}
	
	public static double[] getPitchYawRoll(CarData car, Vector2 forward, Vector3 up){
		double upZ = up.normalized().z;
		return getPitchYawRoll(car, forward.withZ(Math.max(1 - upZ, -1 + upZ)), up);
	}
	
	public static double[] getPitchYawRoll(CarData car, Vector2 forward){
		return getPitchYawRoll(car, forward, worldUp);
	}
	
	public static double[] getPitchYawRoll(CarData car, Vector3 forward){
		return getPitchYawRoll(car, forward, worldUp);
	}

}

