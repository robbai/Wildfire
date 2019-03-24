package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class ShadowState extends State {

	Vector2 homeGoal;

	public ShadowState(Wildfire wildfire){
		super("Shadow", wildfire);
		updateHomeGoal(wildfire.team);
	}

	@Override
	public boolean ready(DataPacket input){
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return false;
		
		//Ball must not be close to our net
		if(input.ball.position.flatten().distance(homeGoal) < 2000 || Utils.teamSign(input.car) * input.ball.position.y < -4700) return false;
		
		//The ball must not be centralised
		if(Math.abs(input.ball.position.x) < (Utils.isOpponentBehindBall(input) ? 1600 : 1400)) return false;
		
		//We're on the wrong side of the ball
		if(Math.signum(input.car.position.y - input.ball.position.y) == Utils.teamSign(input.car) && Math.signum(input.ball.velocity.y) != Utils.teamSign(input.car)){
			return Utils.teamSign(input.car) * input.car.velocity.y > -950 || Utils.isTowardsOwnGoal(input.car, wildfire.impactPoint.getPosition());
		}
		
		//There is no defender
		if(!Utils.isOpponentBehindBall(input)) return false;
		
		//Outside of the "useful hitting arc"
		if(Math.abs(input.ball.position.y) < 4000 && new Vector3(0, -Utils.teamSign(input.car), 0).angle(input.car.position.minus(wildfire.impactPoint.getPosition())) > Math.PI * 0.45){
			if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) > 2300) return true;
		}
		
		return Math.abs(Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten())) > Math.PI * 0.5 && Utils.teamSign(input.car.team) * input.ball.velocity.y > -900;
	}
	
	@Override
	public boolean expire(DataPacket input){		
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return true;
		
		double distance = wildfire.impactPoint.getPosition().distanceFlat(input.car.position);
				
		//Aiming very close to the ball, and close-by		
		if(Math.abs(Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten())) < 0.16 && distance < 4000){
			return true;
		}
		
		//Ball is close to our net
		if(input.ball.position.flatten().distance(homeGoal) < 4500 || Utils.teamSign(input.car) * input.ball.position.y < -4800) return true;
		
		//Ball is centralised
		if(Math.abs(input.ball.position.x) < 1200 && distance < 7000) return true;
		
		//Beating the opponent
		double ourDistance = input.car.position.distance(input.ball.position);
		if(ourDistance < (Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition().flatten()) ? 1400 : 1650)) return true;
		double closestOpponentDistance = Utils.closestOpponentDistance(input, input.ball.position);
		return !Utils.isTeammateCloser(input) && (closestOpponentDistance > Math.max(1800, ourDistance * 1.35D) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850);
	}

	private boolean avoidStoppingForever(DataPacket input){
		return input.ball.velocity.magnitude() < 500 && Math.signum(input.ball.position.y) != Utils.teamSign(input.car);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Update the home goal every three seconds
		if(input.elapsedSeconds % 3 < 1) updateHomeGoal(input.car.team);
		
		//Recovery
		if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 target = getTarget(input);
		
		double steerCorrectionRadians = Utils.aim(input.car, target);
		double distance = target.distance(input.car.position.flatten());
		
		if(!hasAction() && input.car.hasWheelContact && distance > 2350){
			if(input.car.velocity.magnitude() > 1200 && Math.abs(steerCorrectionRadians) < Math.PI * 0.25){
				currentAction = new DodgeAction(this, input.ball.position.distanceFlat(input.car.position) > 600 ? steerCorrectionRadians : Utils.aim(input.car, input.ball.position.flatten()), input);
			}else if(Math.abs(steerCorrectionRadians) > Math.PI * 0.85 && input.car.position.z < 100){
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle;
		if(distance < 3000 && input.car.velocity.magnitude() > distance * 2){
			throttle = -1;
			wildfire.sendQuickChat(QuickChatSelection.Information_AllYours);
		}else{
			throttle = (float)distance / 4800F;
			if(distance > 3500 && input.car.isSupersonic) wildfire.sendQuickChat(QuickChatSelection.Information_Defending);
		}
		
        return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(throttle).withBoost(distance > 1500 && Math.abs(steerCorrectionRadians) < 0.2F && !Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()));
	}

	/**
	 * Get a point halfway between the ball and our goal
	*/
	private Vector2 getTarget(DataPacket input){
		Vector2 target = homeGoal.plus(input.ball.position.flatten().minus(homeGoal).scaled(0.5D));
		
		BoostPad bestBoost = null;
		if(input.car.boost <= 60){
			double bestBoostDistance = 0;
			for(BoostPad b : BoostManager.getSmallBoosts()){
				if(!b.isActive() || Math.signum(b.getLocation().y - target.y) == Utils.teamSign(input.car)) continue;
				double distance = b.getLocation().distanceFlat(target);
				if(bestBoost == null || distance < bestBoostDistance){
					bestBoost = b;
					bestBoostDistance = distance;
				}
			}
		}
		
		wildfire.renderer.drawCircle(Color.BLACK, target, (bestBoost == null ? Utils.BALLRADIUS : 45));
		if(bestBoost == null) return target;
		
		wildfire.renderer.drawCircle(Color.BLUE, bestBoost.getLocation().flatten(), 70);
		return bestBoost.getLocation().flatten();
	}
	
	/**
	 * I don't know why I made this a method
	 */
	private void updateHomeGoal(int team){
		homeGoal = Utils.homeGoal(team);
	}

}