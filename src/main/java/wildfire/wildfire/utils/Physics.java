package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;

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
	 * Via DomNomNom's tests
	 */
	public static double getTurnRadius(double speed){
		if(speed < 0) return 0;
	    return 156D + 0.1D * speed + 0.000069D * Math.pow(speed, 2) + 0.000000164D * Math.pow(speed, 3) -0.0000000000562D * Math.pow(speed, 4);
	}

	/**
	 * Estimates the max velocity for a car, given an amount of boost and current velocity
	 */
	public static double boostMaxSpeed(double initialVelocity, double boost){
		double boostTime = (boost / 33);
		double throttleTime = (1410 - initialVelocity) / 1000;
		if(throttleTime > 0) boostTime -= throttleTime;
		if(boostTime <= 0) return 1410;
		return Math.min(2300, 1410 + 1000 * boostTime);
	}
	
	public static double dodgeImpulse(double velocityForward, double forwards, double sideways){
		Vector2 direction = new Vector2(Math.abs(sideways), forwards).normalized();
		double forwardsComponent = Math.acos(direction.y);
		double sidewaysComponent = Math.asin(direction.x);
		
		return Math.abs(forwardsComponent) * (forwardsComponent > 0 ? 500D : 533D) + sidewaysComponent * (500D * (1D + 0.9D * Math.abs(velocityForward / 2300D)));
	}

}
