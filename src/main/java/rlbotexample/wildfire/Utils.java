package rlbotexample.wildfire;

import java.awt.Color;
import java.util.Random;

import rlbot.flat.BallPrediction;
import rlbot.render.Renderer;
import rlbotexample.input.BallData;
import rlbotexample.input.CarData;
import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.vector.Vector3;

public class Utils {
	
	public static final float BALLRADIUS = 92.75F;
	public static final float GRAVITY = 650F;
	public static final float PITCHWIDTH = 4096F;
	public static final float PITCHLENGTH = 5120F;
	public static final float CEILING = 2044F;
	public static final float GOALHEIGHT = 642.775F;
	public static final float GOALHALFWIDTH = 892.755F;
	
	public static void drawCrosshair(Renderer renderer, CarData car, Vector3 point, Color colour, double size){
    	renderer.drawLine3d(colour, point.withZ(point.z - size / 2).toFramework(), point.withZ(point.z + size / 2).toFramework());
    	Vector3 orthogonal = car.position.minus(point).scaledToMagnitude(size / 2).rotateHorizontal(Math.PI / 2).withZ(0);
    	renderer.drawLine3d(colour, point.plus(orthogonal).toFramework(), point.minus(orthogonal).toFramework());
    }
	
	public static void drawCircle(Renderer renderer, Color colour, Vector2 centre, double radius){
		drawCircle(renderer, colour, centre.withZ(0), radius);
	}
	
	public static void drawCircle(Renderer renderer, Color colour, Vector3 centre, double radius){
		Vector3 last = null;
		double pointCount = 100;
		for(double i = 0; i < pointCount; i += 1){
            double angle = 2 * Math.PI * i / pointCount;
            Vector3 latest = new Vector3(centre.x + radius * Math.cos(angle), centre.y + radius * Math.sin(angle), centre.z);
            if(last != null) renderer.drawLine3d(colour, last.toFramework(), latest.toFramework());
            last = latest;
        }
		
		//Connect the end to the start
		renderer.drawLine3d(colour, last.toFramework(), new Vector3(centre.x + radius, centre.y, centre.z).toFramework());
	}
	
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
	
	public static double invertAim(double aim){
		if(aim > 0){
			return aim - Math.PI;
		}else{
			return aim + Math.PI;
		}
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
	public static Vector3 getEarliestImpactPoint(DataPacket input, BallPrediction ballPrediction){
		Vector2 carPosition = input.car.position.flatten(); 
		double initialVelocity = input.car.velocity.flatten().magnitude();
		
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			double displacement = location.flatten().distance(carPosition) - BALLRADIUS;
			double timeLeft = (double)i / 60D;
			double acceleration = 2 * (displacement - initialVelocity * timeLeft) / Math.pow(timeLeft, 2);
			
			if(initialVelocity + acceleration * timeLeft < Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, input.car.boost))){
				return location.plus(carPosition.minus(location.flatten()).normalized().scaled(BALLRADIUS).withZ(0));
			}
		}
		
		//Return final position as fallback
		return Vector3.fromFlatbuffer(ballPrediction.slices(ballPrediction.slicesLength() - 1).physics().location());
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
	
	public static float clamp(float value){
		return Math.max(-1F, Math.min(1F, value));
	}
	public static double clamp(double value){
		return Math.max(-1D, Math.min(1D, value));
	}
	
	public static boolean hasOpponent(DataPacket input){
		for(byte i = 0; i < input.cars.length; i++){
			if(input.cars[i].team != input.car.team) return true;
		}
		return false;
	}
	
	/*
	 * Heuristic for the max velocity given an amount of boost
	 */
	public static double boostMaxSpeed(double initialVelocity, double boost){
		return Math.min(2300, initialVelocity + 900D * Math.min(1D, boost / 35D));
	}
	
	public static double timeToHitGround(CarData car){
		// t = (-u +/- sqrt(u^2 + 2as)) / a
		
		double s = car.position.z - 60;
		if(s <= 0) return 0;
		
		double a = 650;
		double u = -car.velocity.z;
		
		double t = Math.max((-u + Math.sqrt(u * u + 2 * a * s)) / a, (-u - Math.sqrt(u * u + 2 * a * s)) / a);
		return t;
	}
	
	public static ControlsOutput driveDownWall(DataPacket input){
		return new ControlsOutput().withThrottle(1).withBoost(false).withSteer((float)input.car.orientation.eularRoll * 10F);
	}
	
	public static boolean isOnWall(CarData car){
		return car.position.z > 200 && car.hasWheelContact;
	}
	
	public static void transferAction(Action one, Action two){
		one.state.currentAction = two;
	}
	
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
	
	/*
	 * Draw the turning radius
	 */
	public static void renderTurningRadius(Renderer renderer, CarData car){
    	double turningRadius = Utils.getTurnRadius(car.velocity.flatten().magnitude());
    	Utils.drawCircle(renderer, Color.PINK, car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten(), turningRadius);
    	Utils.drawCircle(renderer, Color.PINK, car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten(), turningRadius);
	}
	
	public static boolean canShoot(CarData car, Vector3 ball){
		return canShoot(car, ball, new Vector2(Utils.teamSign(car.team) * (Utils.GOALHALFWIDTH - Utils.BALLRADIUS), Utils.teamSign(car.team) * Utils.PITCHLENGTH), new Vector2(Utils.teamSign(car.team) * (-Utils.GOALHALFWIDTH + Utils.BALLRADIUS), Utils.teamSign(car.team) * Utils.PITCHLENGTH));
	}
	
	public static boolean canShoot(CarData car, Vector3 ball, Vector2 left, Vector2 right){
		double aimBall = Utils.aim(car, ball.flatten());
		double aimLeft = Utils.aim(car, left);
		double aimRight = Utils.aim(car, right);
		return aimBall > aimLeft && aimBall < aimRight;
	}
	
	public static boolean isOpponentBehindBall(DataPacket input){
		for(byte i = 0; i < input.cars.length; i++){
			CarData car = input.cars[i];
			if(car == null || car.team == input.car.team) continue;
			if(Math.signum(input.ball.position.y - car.position.y) == Utils.teamSign(input.car.team)) return true;
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

}
