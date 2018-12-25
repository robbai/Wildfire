package wildfire.wildfire;

import java.util.Random;

import rlbot.flat.BallPrediction;
import wildfire.input.BallData;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PredictionSlice;

public class Utils {
	
	public static final float BALLRADIUS = 92.75F;
	public static final float GRAVITY = 650F;
	public static final float PITCHWIDTH = 4096F;
	public static final float PITCHLENGTH = 5120F;
	public static final float CEILING = 2044F;
	public static final float GOALHEIGHT = 642.775F;
	public static final float GOALHALFWIDTH = 892.755F;
	
	public static Vector2 homeGoal(int team){
		return new Vector2(0, teamSign(team) * -PITCHLENGTH);
	}
	public static Vector2 enemyGoal(int team){
		return new Vector2(0, teamSign(team) * PITCHLENGTH);
	}
	
	public static double aim(CarData car, Vector2 point){
		Vector2 carPosition = car.position.flatten();
        Vector2 carDirection = car.orientation.noseVector.flatten();
		return carDirection.correctionAngle(point.minus(carPosition));
	}
	
	public static double aimFromPoint(Vector2 carPosition, Vector2 carDirection, Vector2 point){
		return carDirection.correctionAngle(point.minus(carPosition));
	}
	
	/*
	 * Flip the aim to face backwards
	 */
	public static double invertAim(double aim){
		return aim != 0 ? aim + -Math.signum(aim) * Math.PI : Math.PI;
	}
	
	public static int teamSign(int team){
		return (team == 0 ? 1 : -1);
	}
	public static int teamSign(CarData car){
		return teamSign(car.team);
	}

	public static boolean isKickoff(DataPacket input){
		return input.ball.velocity.isZero() && input.ball.position.flatten().isZero();
	}
	
	public static boolean isCarAirborne(CarData car){
		return !car.hasWheelContact && (car.position.z > 150 || Math.abs(car.velocity.z) > 280);
	}
	
	public static boolean isBallAirborne(BallData ball){
		return ball.position.z > 310 || Math.abs(ball.velocity.z) > 280;
	}
	
	/**
	 * Acceleration = 2(Displacement - Initial Velocity * Time) / Time^2
	 */
	public static PredictionSlice getEarliestImpactPoint(DataPacket input, BallPrediction ballPrediction){
		Vector2 carPosition = input.car.position.flatten(); 
		double initialVelocity = input.car.velocity.flatten().magnitude();
		
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			double displacement = location.flatten().distance(carPosition) - BALLRADIUS;
			double timeLeft = (double)i / 60D;
			double acceleration = 2 * (displacement - initialVelocity * timeLeft) / Math.pow(timeLeft, 2);
			
			if(initialVelocity + acceleration * timeLeft < Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, input.car.boost))){
				return new PredictionSlice(location.plus(carPosition.minus(location.flatten()).normalized().scaled(BALLRADIUS).withZ(0)), i);
			}
		}
		
		//Return final position as fallback
		return new PredictionSlice(Vector3.fromFlatbuffer(ballPrediction.slices(ballPrediction.slicesLength() - 1).physics().location()), ballPrediction.slicesLength() - 1);
	}
	
	public static boolean isTeammateCloser(DataPacket input, Vector3 target){
		double ourDistance = target.distance(input.car.position);
		for(byte i = 0; i < input.cars.length; i++){
			CarData c = input.cars[i];
			if(i == input.playerIndex || c == null || c.team != input.car.team) continue;
			if(ourDistance > target.distance(c.position)) return true;
		}
		return false;
	}
	
	public static boolean isTeammateCloser(DataPacket input){
		return isTeammateCloser(input, input.ball.position);
	}
	
	public static boolean isTeammateCloser(DataPacket input, Vector2 target){
		return isTeammateCloser(input, target.withZ(0));
	}
	
	public static boolean hasTeammate(DataPacket input){
		for(byte i = 0; i < input.cars.length; i++){
			if(i != input.playerIndex && input.cars[i].team == input.car.team) return true;
		}
		return false;
	}
	
	public static CarData closestOpponent(DataPacket input){
		CarData best = null;
		double bestDistance = Double.MAX_VALUE;
		for(CarData c : input.cars){
			if(c.team == input.car.team) continue;
			double distance = c.position.distance(input.ball.position);
			if(distance < bestDistance){
				best = c;
				bestDistance = distance;
			}
		}
		return best;
	}
	
	public static double closestOpponentDistance(DataPacket input){
		CarData opponent = closestOpponent(input);
		return opponent == null ? Double.MAX_VALUE : opponent.position.distance(input.ball.position);
	}

	public static Vector3 getBounce(BallPrediction ballPrediction){
	    for(int i = 0; i < ballPrediction.slicesLength(); i++){
	    	Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
	    	if(location.z <= Utils.BALLRADIUS + 15) return location;
	    }
	    return null;
	}
	
	/**
	 * Time from now for the time to touch the floor, 
	 * measured in seconds
	 */
	public static double getBounceTime(BallPrediction ballPrediction){
	    for(int i = 0; i < ballPrediction.slicesLength(); i++){
	    	if(Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location()).z <= Utils.BALLRADIUS + 15){
	    		return (double)i / 60D;
	    	}
	    }
	    return -1;
	}
	
	public static float round(float value){
		return Math.round(value * 100F) / 100F;
	}
	
	public static double round(double value){
		return Math.round(value * 100D) / 100D;
	}
	
	public static boolean hasOpponent(DataPacket input){
		for(byte i = 0; i < input.cars.length; i++){
			if(input.cars[i].team != input.car.team) return true;
		}
		return false;
	}
	
	/*
	 * Awful heuristic for the max velocity given an amount of boost,
	 * requires an update
	 */
	public static double boostMaxSpeed(double initialVelocity, double boost){
		return Math.min(2300, initialVelocity + 900D * Math.min(1D, boost / 35D));
	}
	
	// t = (-u +/- sqrt(u^2 + 2as)) / a
	public static double timeToHitGround(CarData car){
		double s = car.position.z - 60;
		if(s <= 0) return 0;
		
		double a = 650;
		double u = -car.velocity.z;
		
		double t = Math.max((-u + Math.sqrt(u * u + 2 * a * s)) / a, (-u - Math.sqrt(u * u + 2 * a * s)) / a);
		return t;
	}
	
	/*
	 * Inspired by the wonderful Darxeal
	 */
	public static ControlsOutput driveDownWall(DataPacket input){
		return new ControlsOutput().withThrottle(1).withBoost(false).withSteer((float)input.car.orientation.eularRoll * 10F);
	}
	
	public static boolean isOnWall(CarData car){
		return car.position.z > 200 && car.hasWheelContact;
	}
	
	public static void transferAction(Action one, Action two){
		one.state.currentAction = two;
	}
	
	/*
	 * ATBA controller (no wiggle)
	 */
	public static ControlsOutput driveBall(DataPacket input){
		return new ControlsOutput().withSteer((float)-aim(input.car, input.ball.position.flatten()) * 2F).withBoost(false);
	}
	
	/*
	 * Whether the ball will go in this team's goal
	 */
	public static boolean isOnTarget(BallPrediction ballPrediction, int team){
	    for(int i = 0; i < ballPrediction.slicesLength(); i++){
	    	Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
	    	if(Math.abs(location.y) >= PITCHLENGTH) return Math.signum(location.y) != teamSign(team);
	    }
	    return false;
	}
	
	/*
	 * Returns a 2D vector of a point inside of the enemy's goal,
	 * it should be a good place to shoot relative to this car
	 */
	public static Vector2 getTarget(CarData car, BallData ball){
    	final double goalSafeZone = 770D;
    	Vector2 target = null;
    	Vector2 ballDifference = ball.position.minus(car.position).flatten();
    	ballDifference = ballDifference.scaled(1D / Math.abs(ballDifference.y)); //Make the Y-value 1
    	if(car.team == 0 && ballDifference.y > 0){
    		double distanceFromGoal = Utils.PITCHLENGTH - ball.position.y;
    		ballDifference = ballDifference.scaled(distanceFromGoal);
    		target = ball.position.flatten().plus(ballDifference);
    	}else if(car.team == 1 && ballDifference.y < 0){
    		double distanceFromGoal = Utils.PITCHLENGTH + ball.position.y;
    		ballDifference = ballDifference.scaled(distanceFromGoal);
    		target = ball.position.flatten().plus(ballDifference);
    	}
    	if(target != null){
    		target = new Vector2(Math.max(-goalSafeZone, Math.min(goalSafeZone, target.x)), target.y);
    		return target;
    	}
		return Utils.enemyGoal(car.team);
    }
	
	/*
	 * Terrible heuristic, needs to be changed
	 */
	public static boolean isEnoughBoostForAerial(CarData car, Vector3 target){
		return car.boost > 18 && target.distance(car.position) < 135D * car.boost;  
	}
	
	public static boolean isPointWithinRange(Vector2 point, Vector2 target, double minRange, double maxRange){
		double range = point.distance(target);
		return range < maxRange && range > minRange;
	}
	
	/*
	 * Via DomNomNom's tests
	 */
	public static double getTurnRadius(double speed){
	    return 156D + 0.1D * speed + 0.000069D * Math.pow(speed, 2) + 0.000000164D * Math.pow(speed, 3) -0.0000000000562D * Math.pow(speed, 4);
	}
	
	public static boolean canShoot(CarData car, Vector3 ball){
		Vector2 trace = traceToY(car.position.flatten(), ball.minus(car.position).flatten(), Utils.teamSign(car) * Utils.PITCHLENGTH);
		if(trace == null) return false; //Facing the wrong way
		return Math.abs(trace.x) < GOALHALFWIDTH;
	}
	
	public static boolean isOpponentBehindBall(DataPacket input){
		for(byte i = 0; i < input.cars.length; i++){
			CarData car = input.cars[i];
			if(car == null || car.team == input.car.team) continue;
			if(correctSideOfTarget(car, input.ball.position)) return true;
		}
		return false;
	}
	
	public static float randomRotation(){
    	Random r = new Random();
    	return (float)(r.nextFloat() * Math.PI * 2 - Math.PI);
    }
	
	public static float random(double min, double max){
		Random r = new Random();
		return (float)(r.nextFloat() * Math.abs(max - min) + Math.min(min, max));
	}

	public static double wrapAngle(double d){
		if(d < -Math.PI) d += 2 * Math.PI;
		if(d > Math.PI) d -= 2 * Math.PI;
		return d;
	}
	
	public static double clampSign(double value){
		return clamp(value, -1, 1);
	}

	public static double clamp(double value, double min, double max){
		return Math.max(Math.min(min, max), Math.min(Math.max(min, max), value));
	}
	
	public static double distanceToWall(Vector3 position){
		return Math.min(PITCHWIDTH - Math.abs(position.x), Math.abs(position.y) > PITCHLENGTH ? 0 : PITCHLENGTH - Math.abs(position.y));
	}
	
	public static boolean correctSideOfTarget(CarData car, Vector2 target){
		return Math.signum(target.y - car.position.y) == Utils.teamSign(car);
	}
	
	public static boolean correctSideOfTarget(CarData car, Vector3 target){
		return correctSideOfTarget(car, target.flatten());
	}
	
	public static Vector2 traceToX(Vector2 start, Vector2 direction, double x){
		//The direction is pointing away from the X
		if(Math.signum(direction.x) != Math.signum(x - start.x)) return null;

		//Scale up the direction so the X meets the requirement
		double xChange = x - start.x;
		direction = direction.normalized();
		direction = direction.scaled(Math.abs(xChange / direction.x));
		
		return start.plus(direction);
	}
	
	public static Vector2 traceToY(Vector2 start, Vector2 direction, double y){
		//The direction is pointing away from the X
		if(Math.signum(direction.y) != Math.signum(y - start.y)) return null;

		//Scale up the direction so the X meets the requirement
		double yChange = y - start.y;
		direction = direction.normalized();
		direction = direction.scaled(Math.abs(yChange / direction.y));
		
		return start.plus(direction);
	}
	
	public static Vector2 traceToWall(Vector2 start, Vector2 direction){
		Vector2 trace;
		
		if(!direction.isZero()){
			trace = traceToX(start, direction, Math.signum(direction.x) * PITCHWIDTH);
			if(trace != null && Math.abs(trace.y) <= PITCHLENGTH) return trace;
			
			trace = traceToY(start, direction, Math.signum(direction.y) * PITCHLENGTH);
			if(trace != null && Math.abs(trace.x) <= PITCHWIDTH) return trace;
		}
		
		//Direction is zero or the start is outside of the map
		return start.confine();
	}
	
	/*
	 * Returns whether the trace goes into our own goal
	 */
	public static boolean isTowardsOwnGoal(CarData car, Vector2 ball){
		Vector2 trace = traceToY(car.position.flatten(), ball.minus(car.position.flatten()), teamSign(car) * -PITCHLENGTH);
		return trace != null && Math.abs(trace.x) < GOALHALFWIDTH - 50;
	}
	
	/*
	 * Returns whether the trace goes into the opponent's goal
	 */
	public static boolean isTowardsEnemyGoal(CarData car, Vector2 ball){
		Vector2 trace = traceToY(car.position.flatten(), ball.minus(car.position.flatten()), teamSign(car) * PITCHLENGTH);
		return trace != null && Math.abs(trace.x) < GOALHALFWIDTH - 50;
	}
	
	// https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
	public static double distanceFromLine(Vector2 point, Vector2 lineA, Vector2 lineB){
		return Math.abs(point.x * (lineB.y - lineA.y) - point.y * (lineB.x - lineA.x) + lineB.x * lineA.y - lineB.y * lineA.x) / Math.sqrt(Math.pow(lineB.y - lineA.y, 2) + Math.pow(lineB.x - lineA.x, 2));
	}

}
