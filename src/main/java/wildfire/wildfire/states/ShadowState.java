package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

public class ShadowState extends State {
	
	private final double homeZoneSize = 4500D;
	Vector2 homeGoal;

	public ShadowState(Wildfire wildfire){
		super("Shadow", wildfire);
		updateHomeGoal(wildfire.team);
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
		
		//Outside of the "useful hitting arc"
		if(new Vector3(0, -Utils.teamSign(input.car), 0).angle(input.car.position.minus(wildfire.impactPoint.getPosition())) > Math.PI * 0.4){
			if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) > 1500) return true;
		}
		
//		if(Utils.isTeammateCloser(input)) return true;
		
		return Math.abs(Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten())) > Math.PI * 0.6 && Utils.teamSign(input.car.team) * input.ball.velocity.y > -800;
	}
	
	@Override
	public boolean expire(DataPacket input){
		double distance = wildfire.impactPoint.getPosition().distanceFlat(input.car.position);
		
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return true;
				
		//Aiming very close to the ball, and closeby
		if(Math.abs(Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten())) < 0.13 && distance < 3500){
			return true;
		}
		
		//Ball is close to our net
		if(input.ball.position.flatten().distance(homeGoal) < homeZoneSize) return true;
		
		//Ball is centralised
		if(Math.abs(input.ball.position.x) < 1200 && distance < 7000) return true;
		
		//Beating the opponent
		double ourDistance = input.car.position.distance(input.ball.position);
		if(ourDistance < 800) return true;
		double closestOpponentDistance = Utils.closestOpponentDistance(input);
		return !Utils.isTeammateCloser(input) && (closestOpponentDistance > Math.max(2250, ourDistance * 1.75D) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850);
	}

	private boolean avoidStoppingForever(DataPacket input){
		return input.ball.velocity.isZero() && Math.signum(input.ball.position.y) != Utils.teamSign(input.car);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Update the home goal every three seconds
		if(input.gameInfo.gameTimeRemaining() % 3 < 1) updateHomeGoal(input.car.team);
		
		Vector2 target = getTarget(input);
		wildfire.renderer.drawCircle(Color.BLACK, target, Utils.BALLRADIUS);
		double steerCorrectionRadians = Utils.aim(input.car, target);
		double distance = target.distance(input.car.position.flatten());
		
		if(!hasAction() && input.car.hasWheelContact && distance > 2350){
			if(input.car.velocity.magnitude() > 1000 && Math.abs(steerCorrectionRadians) < Math.PI * 0.25){
				currentAction = new DodgeAction(this, input.ball.position.distanceFlat(input.car.position) > 600 ? steerCorrectionRadians : Utils.aim(input.car, input.ball.position.flatten()), input);
			}else if(Math.abs(steerCorrectionRadians) > Math.PI * 0.85 && input.car.position.z < 100){
				currentAction = new HalfFlipAction(this);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle;
		if(distance < 3000 && input.car.velocity.magnitude() > distance * 2){
			throttle = -1;
			wildfire.sendQuickChat(QuickChatSelection.Information_AllYours);
		}else{
			throttle = (float)distance / 4800F;
			if(distance > 3000) wildfire.sendQuickChat(QuickChatSelection.Information_Defending);
		}
		
        return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(throttle).withBoost(distance > 1500 && Math.abs(steerCorrectionRadians) < 0.2F && !Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()));
	}

	/**
	 * Get a point halfway between the ball and our goal
	*/
	private Vector2 getTarget(DataPacket input){
		Vector2 difference = input.ball.position.flatten().minus(homeGoal);
		return homeGoal.plus(difference.scaled(0.5D));
	}
	
	/**
	 * I don't know why I made this a method
	 */
	private void updateHomeGoal(int team){
		homeGoal = Utils.homeGoal(team);
	}

}
