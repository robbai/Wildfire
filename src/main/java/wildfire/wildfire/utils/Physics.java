package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;

public class Physics {
	
	/**
	 * Piecewise-linear
	 * https://samuelpmish.github.io/notes/RocketLeague/ground_control/#turning
	 */
	private static double[][] speedCurvature = new double[][] {{0.0, 0.00690}, {500.0, 0.00398}, {1000.0, 0.00235}, {1500.0, 0.00138}, {1750.0, 0.00110}, {2300.0, 0.00088}};
	
	/**
	 * Piecewise-linear
	 * https://samuelpmish.github.io/notes/RocketLeague/ground_control/#throttle
	 */
	private static double[][] throttleAcceleration = new double[][] {{0, 1600}, {1400, 160}, {1410, 0}, {2300, 0}};
	
	public static double timeToHitGround(CarData car){
		double s = car.position.z - 17;
		if(s <= 0) return 0;

		double a = Constants.GRAVITY;
		double u = -car.velocity.z;

		double t = Math.max((-u + Math.sqrt(u * u + 2 * a * s)) / a, (-u - Math.sqrt(u * u + 2 * a * s)) / a);
		return t;
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

	public static double getTurnRadius(double v){
		if(v == 0) return 0;
	    return 1.0 / curvature(v);
	}

	// https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L34-L53
	private static double curvature(double v){
		v = Utils.clamp(Math.abs(v), 0, 2300);

		for(int i = 0; i < 5; i++){
			if(speedCurvature[i][0] <= v && v < speedCurvature[i + 1][0]){
				double u = (v - speedCurvature[i][0]) / (speedCurvature[i + 1][0] - speedCurvature[i][0]);
				return Utils.lerp(speedCurvature[i][1], speedCurvature[i + 1][1], u);
			}
		}

		return 0;
	}
	
	// https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L55-L74
	public static double getSpeedFromRadius(double r){
		double k = (1D / r);
		k = Utils.clamp(k, speedCurvature[speedCurvature.length - 1][1], speedCurvature[0][1]);
		
		for(int i = 5; i > 0; i--){
			if(speedCurvature[i][1] <= k && k < speedCurvature[i - 1][1]){
				double u = (k - speedCurvature[i][1]) / (speedCurvature[i - 1][1] - speedCurvature[i][1]);
				return Utils.lerp(speedCurvature[i][0], speedCurvature[i - 1][0], u);
			}
		}
		
		return 0;
	}
	
	// https://samuelpmish.github.io/notes/RocketLeague/ground_control/#acceleration
	public static double determineAcceleration(double velocityForward, double throttle, boolean boost){
		if(boost) throttle = 1;
		
		double boostAcceleration = (boost ? Constants.BOOSTACC : 0);
		
		// Coasting and braking.
		boolean coast = (Math.abs(throttle) < 0.01);
		boolean brake = (!coast && velocityForward * throttle < 0);
		if(coast){
			return -Math.signum(velocityForward) * Constants.COASTACC;
		}else if(brake){
			return -Math.signum(velocityForward) * (Constants.BRAKEACC + boostAcceleration);
		}
		
		// Throttle.
		velocityForward = Utils.clamp(Math.abs(velocityForward), 0, 2300);
		for(int i = 0; i < 3; i++){
			if(throttleAcceleration[i][0] <= velocityForward && velocityForward < throttleAcceleration[i + 1][0]){
				double u = (velocityForward - throttleAcceleration[i][0]) / (throttleAcceleration[i + 1][0] - throttleAcceleration[i][0]);
				return Utils.lerp(throttleAcceleration[i][1], throttleAcceleration[i + 1][1], u) + boostAcceleration;
			}
		}

		return 0;
	}

}
