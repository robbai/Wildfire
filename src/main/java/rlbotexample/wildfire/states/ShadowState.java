package rlbotexample.wildfire.states;

import java.awt.Color;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.DodgeAction;
import rlbotexample.wildfire.actions.HalfFlipAction;

public class ShadowState extends State {
	
	private final double homeZoneSize = 4500D;
	Vector2 homeGoal;

	public ShadowState(Wildfire wildfire){
		super("Shadow", wildfire);
		homeGoal = Utils.homeGoal(wildfire.team);
	}

	@Override
	public boolean ready(DataPacket input){
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return false;
		
		//The ball must not be centralised
		if(Math.abs(input.ball.position.x) < 1500) return false;
		
		//If we're on the wrong side of the ball
		if(Math.signum(input.car.position.y - input.ball.position.y) == Utils.teamSign(input.car) && Math.signum(input.ball.velocity.y) != Utils.teamSign(input.car)){
			return true;
		}
		
		if(Utils.isTeammateCloser(input)) return true;
		
		return Math.abs(Utils.aim(input.car, wildfire.impactPoint.flatten())) > Math.PI * 0.6 && Utils.teamSign(wildfire.team) * input.ball.velocity.y > -800;
	}
	
	@Override
	public boolean expire(DataPacket input){
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return true;
				
		//Aiming very close to the ball
		if(Math.abs(Utils.aim(input.car, wildfire.impactPoint.flatten())) < 0.13) return true;
		
		//Ball is close to our net
		if(input.ball.position.flatten().distance(homeGoal) < homeZoneSize) return true;
		
		//Ball is centralised
		if(Math.abs(input.ball.position.x) < 1200) return true;
		
		//Beating the opponent
		double ourDistance = input.car.position.distance(input.ball.position);
		if(ourDistance < 800) return true;
		double closestOpponentDistance = Utils.closestOpponentDistance(input);
		return !Utils.isTeammateCloser(input) && (closestOpponentDistance > Math.max(2250, ourDistance * 1.75D) || Utils.teamSign(wildfire.team) * input.ball.velocity.y < -850);
	}

	private boolean avoidStoppingForever(DataPacket input) {
		return input.ball.velocity.isZero() && Math.signum(input.ball.position.y) != Utils.teamSign(input.car);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		Vector2 target = getTarget(input);
		Utils.drawCircle(wildfire.renderer, Color.BLACK, target, Utils.BALLRADIUS);
		double steerCorrectionRadians = Utils.aim(input.car, target);
		double distance = target.distance(input.car.position.flatten());
		
		if(!hasAction() && input.car.hasWheelContact && distance > 2350){
			if(input.car.velocity.magnitude() > 1000 && Math.abs(steerCorrectionRadians) < Math.PI * 0.25){
				currentAction = new DodgeAction(this, input.ball.position.distanceFlat(input.car.position) > 600 ? steerCorrectionRadians : Utils.aim(input.car, input.ball.position.flatten()), input);
			}else if(Math.abs(steerCorrectionRadians) > Math.PI * 0.85){
				currentAction = new HalfFlipAction(this);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle;
		if(distance < 3000 && input.car.velocity.magnitude() > distance * 2){
			throttle = -1;
		}else{
			throttle = (float)distance / 4800F;
		}
		
        return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(throttle).withBoost(distance > 1500 && Math.abs(steerCorrectionRadians) < 0.2F);
	}

	/**
	 * Get a point halfway between the ball and our goal
	*/
	private Vector2 getTarget(DataPacket input){
		Vector2 difference = input.ball.position.flatten().minus(homeGoal);
		return homeGoal.plus(difference.scaled(0.5D));
	}

}
