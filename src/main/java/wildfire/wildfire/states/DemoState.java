package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class DemoState extends State {
	
	private final static double step = (1D / 60), predictionSeconds = 5D;
	
	private CarData target = null;

	public DemoState(Wildfire wildfire){
		super("Demo", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(Math.abs(input.ball.position.x) < 1200) return false;
		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
		if(!isFastEnough(input.car)) return false;
		if(Behaviour.hasTeammate(input) ? Math.abs(input.ball.position.y) < 2000 : Utils.teamSign(input.car) * input.ball.position.y < 1000) return false;
		
		if(Behaviour.isTeammateCloser(input)){
			if(Behaviour.isInCone(input.car, input.info.impact.getPosition())) return false;
		}else if(Behaviour.correctSideOfTarget(input.car, input.ball.position) && Utils.teamSign(input.car) * input.ball.position.y < 4000){
			return false;
		}
		 
		target = getTarget(input);
		
		if(target != null){
			if(target.position.isOutOfBounds()) return false;
			
			//Run-up
			if(target.position.distance(input.car.position) < 3000) return false;
			
			//Vulnerable target (slow, or returning back to their half)
			if(target.velocity.magnitude() > 1200 && Utils.teamSign(input.car) * target.velocity.y > -800) return false;
			
			//Face them
			if(Math.abs(Handling.aim(input.car, target.position.flatten())) > Math.toRadians(85)) return false;
		}
		
		return target != null && isValidTarget(input, target);
	}

	@Override
	public boolean expire(InfoPacket input){
		if(target != null && target.isDemolished) wildfire.sendQuickChat(QuickChatSelection.Apologies_Oops, QuickChatSelection.Apologies_Whoops);
		boolean expire = target == null || !isValidTarget(input, target) || !isFastEnough(input.car);
		if(expire) target = null;
		return expire;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		//Drive down the wall
		if(Behaviour.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		//Recovery
		if(Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(!currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Target handling
		if(target == null) target = getTarget(input);
		if(target == null){
			wildfire.renderer.drawString2d("No Target", Color.WHITE, new Point(0, 20), 2, 2);
			return new ControlsOutput();
		}
				
		//Update target info
//		Vector3 lastTargetPosition = target.position;
		Vector3 lastTargetVelocity = target.velocity;
		float lastTargetElapsed = target.elapsedSeconds;
		target = input.cars[target.index];
		
		//Infer info
		double time = (target.elapsedSeconds - lastTargetElapsed);
		Vector3 acceleration = target.velocity.minus(lastTargetVelocity).scaled(1D / time); //a = (v - u) / t
		wildfire.renderer.drawString2d("Target Velocity: " + (int)input.car.velocity.magnitude() + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Target Acceleration: " + (int)acceleration.magnitude() + "uu/s^2", Color.WHITE, new Point(0, 40), 2, 2);
		
		//Prediction of the opponent
		ArrayList<Vector3> prediction = new ArrayList<Vector3>(); 
		Vector3 velocity = new Vector3(target.velocity);
		Vector3 position = new Vector3(target.position);
		for(int i = 0; i < (predictionSeconds / step); i++){
			prediction.add(position);
			if(i != 0){
				wildfire.renderer.drawLine3d(Color.YELLOW, prediction.get(i - 1).flatten().toFramework(), position.flatten().toFramework());
			}
			
			//Acceleration handling
			//https://samuelpmish.github.io/notes/RocketLeague/ground_control/#throttle
			double currentVelocity = velocity.magnitude();
			double maxAcceleration;
			if(currentVelocity < 1400){
				maxAcceleration = 160 + (1600 - 160) * (1400 - currentVelocity);
			}else if(currentVelocity < 1410){
				maxAcceleration = 160 * ((1410 - currentVelocity) - 1400);
			}else{
				maxAcceleration = 0;
			}
			maxAcceleration = Math.min(2300, maxAcceleration + 991.67); //Boost
			if(acceleration.magnitude() > maxAcceleration) acceleration = acceleration.scaledToMagnitude(maxAcceleration);
			
			//Velocity handling
			velocity = velocity.plus(acceleration.scaled(step));
			if(velocity.magnitude() > 2300) velocity = velocity.scaledToMagnitude(2300);
			
			//Position handling
			position = position.plus(velocity.scaled(step));
			if(!target.position.isOutOfBounds()) position.confine();
		}
		
		//Impact
		Vector3 impact = getImpact(input.car, prediction);
		double impactDistance = impact.distance(input.car.position);
		wildfire.renderer.drawCrosshair(input.car, impact, input.car.isSupersonic ? Color.RED : Color.ORANGE, 100);
		
		//Quick-chat
		if(input.car.isSupersonic){
			if(Behaviour.hasTeammate(input)){
				wildfire.sendQuickChat(true, QuickChatSelection.Custom_Useful_Demoing, QuickChatSelection.Custom_Useful_Bumping);
			}else if(impactDistance < 1000){
				wildfire.sendQuickChat(QuickChatSelection.Information_Incoming);
			}
		}
		
		//Stuck in goal
		Vector2 target = impact.flatten();
		if(Math.max(Math.abs(target.y), Math.abs(input.car.position.y)) > Constants.PITCH_LENGTH){
			target = new Vector2(Utils.clamp(target.x, -600, 600), target.y);
		}
		
		//Controls
		double steer = Handling.aim(input.car, target);
        return new ControlsOutput().withSteer((float)-steer * 3F).withThrottle(1)
        		.withBoost((Math.abs(steer) < 0.3F || input.car.forwardVelocity > 1300) && !input.car.isSupersonic && input.car.hasWheelContact).withSlide(Math.abs(steer) > 1.5F && input.car.forwardVelocity > 0 && impactDistance > 500);
	}
	
	private CarData getTarget(InfoPacket input){
		CarData closestCar = null;
		double closestCarDistance = 0;
		for(CarData car : input.cars){
			if(car == null || car.team == input.car.team || car.isDemolished) continue;
			double carDistance = car.position.distanceFlat(input.ball.position);
			if(closestCar == null || carDistance < closestCarDistance){
				closestCarDistance = carDistance;
				closestCar = car;
			}
		}
		return closestCar;
	}
	
	private boolean isFastEnough(CarData car){
		return car.boost > 18 || car.isSupersonic;
	}
	
	private boolean isValidTarget(InfoPacket input, CarData enemy){
		Vector2 carEnemy = enemy.position.minus(input.car.position).flatten();
		Vector2 carBall = input.ball.position.minus(input.car.position).flatten();
		Vector2 enemyBall = input.ball.position.minus(enemy.position).flatten();
		return !enemy.isDemolished && carEnemy.magnitude() < Math.min(enemyBall.magnitude() * 1.5, 4500) && carBall.angle(carEnemy) > 0.1 && enemy.position.z < 250;
	}
	
	public static Vector3 getImpact(CarData car, ArrayList<Vector3> prediction){
		Vector3 carPosition = car.position; 
		double initialVelocity = car.velocity.flatten().magnitude();
		double maxVelocity = DrivePhysics.maxVelocity(initialVelocity, car.boost);
		
		for(int i = 0; i < prediction.size(); i++){
			Vector3 location = prediction.get(i);
			
			double displacement = carPosition.distanceFlat(location) - 80;
			double timeLeft = (double)i * step;
			double acceleration = 2 * (displacement - initialVelocity * timeLeft) / Math.pow(timeLeft, 2);
			
			if(initialVelocity + acceleration * timeLeft < Math.max(initialVelocity, maxVelocity)){
				return location;
			}
		}
		
//		return prediction.get(prediction.size() - 1);
		return prediction.get(0);
	}

}
