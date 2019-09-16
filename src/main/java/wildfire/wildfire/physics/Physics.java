package wildfire.wildfire.physics;

import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.wildfire.utils.Constants;

public class Physics {
	
	public static double timeToHitGround(CarData car){
		double s = car.position.z - 17;
		if(s <= 0) return 0;

		double a = Constants.GRAVITY;
		double u = -car.velocity.z;

		double t = Math.max((-u + Math.sqrt(u * u + 2 * a * s)) / a, (-u - Math.sqrt(u * u + 2 * a * s)) / a);
		return t;
	}

	/**
	 * Ignore this, doesn't actually work, probably.
	 */
	public static double dodgeImpulse(double velocityForward, double forwards, double sideways){
		Vector2 direction = new Vector2(Math.abs(sideways), forwards).normalized();
		double forwardsComponent = Math.acos(direction.y);
		double sidewaysComponent = Math.asin(direction.x);
		
		return Math.abs(forwardsComponent) * (forwardsComponent > 0 ? 500D : 533D) + sidewaysComponent * (500D * (1D + 0.9D * Math.abs(velocityForward / Constants.MAX_CAR_VELOCITY)));
	}

}
