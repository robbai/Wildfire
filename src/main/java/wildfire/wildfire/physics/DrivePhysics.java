package wildfire.wildfire.physics;

import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class DrivePhysics {

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

	/*
	 *  https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L34-L53
	 */
	private static double curvature(double v){
		v = Utils.clamp(Math.abs(v), 0, Constants.MAX_CAR_VELOCITY);

		for(int i = 0; i < 5; i++){
			if(speedCurvature[i][0] <= v && (v < speedCurvature[i + 1][0] || (i == 4 && v == speedCurvature[5][0]))){
				double u = (v - speedCurvature[i][0]) / (speedCurvature[i + 1][0] - speedCurvature[i][0]);
				return Utils.lerp(speedCurvature[i][1], speedCurvature[i + 1][1], u);
			}
		}

		return 0;
	}

	/*
	 *  https://samuelpmish.github.io/notes/RocketLeague/ground_control/#acceleration
	 */
	public static double determineAcceleration(double velocityForward, double throttle, boolean boost){
		if(boost) throttle = 1;

		double boostAcceleration = (boost ? Constants.BOOST_GROUND_ACCELERATION : 0);

		// Coasting and braking.
		boolean coast = (Math.abs(throttle) < 0.01);
		boolean brake = (!coast && velocityForward * throttle < 0);
		if(coast){
			return -Math.signum(velocityForward) * Constants.COAST_ACCELERATION;
		}else if(brake){
			return -Math.signum(velocityForward) * (Constants.BRAKE_ACCELERATION + boostAcceleration);
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

	/*
	 *  https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/drive.cc#L55-L74
	 */
	public static double getSpeedFromRadius(double r){
		double k = (1D / r);
		k = Utils.clamp(k, speedCurvature[speedCurvature.length - 1][1], speedCurvature[0][1]);

		for(int i = 5; i > 0; i--){
			if(speedCurvature[i][1] <= k && (k < speedCurvature[i - 1][1] || (i == 1 && k == speedCurvature[0][1]))){
				double u = (k - speedCurvature[i][1]) / (speedCurvature[i - 1][1] - speedCurvature[i][1]);
				return Utils.lerp(speedCurvature[i][0], speedCurvature[i - 1][0], u);
			}
		}

		return 0;
	}

	public static double getTurnRadius(double v){
		if(v == 0) return 0;
		return 1.0 / curvature(v);
	}

	public static double maxVelForTurn(CarData car, Vector3 target){
		Vector2 local = Utils.toLocal(car, target).flatten();

		int step = 20;
		for(int v = 0; v <= Constants.MAX_CAR_VELOCITY; v += step){
			double turningRadius = getTurnRadius(v);

			Vector2 left = new Vector2(turningRadius, 0);
			Vector2 right = left.scaled(-1);

			double distance = Math.min(local.distance(left), local.distance(right));
			if(distance < turningRadius){
				return Utils.lerp(Math.max(v - step, 0), v, distance / turningRadius);
			}
		}

		return Constants.MAX_CAR_VELOCITY;
	}

	public static double maxVelocity(double velocityForward, double boost, double maxTime){
		final double step = (1D / 120);

		double velocity = velocityForward, time = 0;
		while(time < maxTime){
			double acceleration = determineAcceleration(velocity, 1, boost >= 1);
			if(Math.abs(acceleration) < 0.1) break;
			
			velocity += acceleration * step;
			
			if(Math.abs(velocity) > Constants.MAX_CAR_VELOCITY) return Constants.MAX_CAR_VELOCITY * Math.signum(velocity);
			
			boost -= (100D / 3) * step;
			
			time += step;
		}

		return velocity;
	}

	public static double maxVelocity(double velocityForward, double boost){
		return maxVelocity(velocityForward, boost, 10);
	}
	
	public static double maxVelocityDist(double velocityForward, double boost, double maxDistance){
		final double step = (1D / 120);

		double velocity = velocityForward, time = 0, distance = 0;
		while(time < 20 && distance < maxDistance){
			double acceleration = determineAcceleration(velocity, 1, boost >= 1);
			if(Math.abs(acceleration) < 0.1) break;
			
			velocity += acceleration * step;
			
			if(Math.abs(velocity) > Constants.MAX_CAR_VELOCITY) return Constants.MAX_CAR_VELOCITY * Math.signum(velocity);
			
			distance += velocity * step;
			
			boost -= (100D / 3) * step;
			
			time += step;
		}

		return velocity;
	}

	public static double maxDistance(double maxTime, double velocity, double boost){
		final double step = (1D / 120);
		
		double time = 0, displace = 0;
		
		while(time < maxTime){
			double acceleration = determineAcceleration(velocity, 1, boost >= 1);
			if(Math.abs(acceleration) < 0.1 && Math.abs(velocity) < 0.1) break;
			
			velocity += acceleration * step;
			velocity = Utils.clamp(velocity, -Constants.MAX_CAR_VELOCITY, Constants.MAX_CAR_VELOCITY);
			boost -= (100D / 3) * step;
			
			displace += velocity * step;
			
			time += step;
		}
		
		return displace;
	}

	public static double timeToReachVel(double currentVelocity, double boost, double targetVelocity){
		final double step = (1D / 120);
		
		boolean brake = (targetVelocity < currentVelocity);
		
		double time = 0;
		
		while(currentVelocity < targetVelocity && time < 10){
			currentVelocity += determineAcceleration(currentVelocity, brake ? -1 : 1, boost >= 1 && !brake) * step;
			if(!brake) boost -= (100D / 3) * step;
			time += step;
		}
		
		return time;
	}

	public static double minTravelTime(double forwardVelocity, double startBoost, double targetDistance, double maxVel){
		final double step = (1D / 120);
		
		double time = 0, distance = 0, boost = startBoost, velocity = Math.max(0, forwardVelocity);
		
		while(distance < targetDistance){
			velocity += determineAcceleration(velocity, 1, boost >= 1) * step;
			velocity = Math.min(velocity, maxVel);
			if(velocity != maxVel) boost -= (100D / 3) * step;
			distance += velocity * step;
			time += step;
		}
		
		return time;
	}
	
	public static double minTravelTime(double forwardVelocity, double startBoost, double targetDistance){
		return minTravelTime(forwardVelocity, startBoost, targetDistance, Constants.MAX_CAR_VELOCITY);
	}
	
	public static double minTravelTime(CarData car, double targetDistance){
		return minTravelTime(car.forwardVelocity, car.boost, targetDistance);
	}

}
