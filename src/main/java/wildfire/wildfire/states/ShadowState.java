package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

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
		
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition());
		
		//Zooming at the ball
		double velocityImpactCorrection = input.car.velocity.flatten().correctionAngle(wildfire.impactPoint.getPosition().minus(input.car.position).flatten());
		if(Math.abs(velocityImpactCorrection) < 0.1 && correctSide && input.car.forwardMagnitude() > 1200 && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < -3200) return false;
		
		//Ball must not be close to our net
		if(input.ball.position.flatten().distance(homeGoal) < 3000) return false; // || Utils.teamSign(input.car) * input.ball.position.y < -4700
		if(Utils.teamSign(input.car) * input.ball.velocity.y < -2900) return false;
		
		//The ball must not be centralised
		if(Math.abs(input.ball.position.x) < (Behaviour.isOpponentBehindBall(input) ? 1600 : 1400)) return false;
		
		//We're on the wrong side of the ball
		if(Math.signum(input.car.position.y - input.ball.position.y) == Utils.teamSign(input.car) && input.ball.velocity.y * Utils.teamSign(input.car) < 800){
			return Utils.teamSign(input.car) * input.car.velocity.y > -1500 || Behaviour.isTowardsOwnGoal(input.car, wildfire.impactPoint.getPosition());
		}
		
		//There is no defender
		if(!Behaviour.isOpponentBehindBall(input)) return false;
		
		//Outside of the "useful hitting arc"
		if(Math.abs(input.ball.position.y) < 3500 && new Vector3(0, -Utils.teamSign(input.car), 0).angle(input.car.position.minus(wildfire.impactPoint.getPosition())) > Math.PI * 0.55){
			if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) > 2800) return true;
		}
		
		return Math.abs(Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten())) > Math.PI * 0.6 && Utils.teamSign(input.car.team) * input.ball.velocity.y > -800;
	}
	
	@Override
	public boolean expire(DataPacket input){		
		//Avoid stopping forever
		if(avoidStoppingForever(input)) return true;
		
		double distance = wildfire.impactPoint.getPosition().distanceFlat(input.car.position);
				
		//Aiming very close to the ball, and close-by		
		if(Math.abs(Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten())) < 0.2 && distance < 3200) return true;
		
		//Ball is centralised
		if(Math.abs(input.ball.position.x) < (Behaviour.hasTeammate(input) ? 1400 : 1200) && distance < 7000) return true;
		
		//Ball is close to our net
		if(input.ball.position.flatten().distance(homeGoal) < 4500 || Utils.teamSign(input.car) * input.ball.position.y < -4800) return true;
		if(Utils.teamSign(input.car) * input.ball.velocity.y < -2400) return true;		
		
		//Beating the opponent
		double ourDistance = input.car.position.distance(input.ball.position);
		if(ourDistance < (Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition().flatten()) ? 1400 : 1650)) return true;
		double closestOpponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		return !Behaviour.isTeammateCloser(input) && (closestOpponentDistance > Math.max(1800, ourDistance * 1.35D) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850);
	}

	private boolean avoidStoppingForever(DataPacket input){
		return input.ball.velocity.magnitude() < 500 && Math.signum(input.ball.position.y) != Utils.teamSign(input.car);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Update the home goal every three seconds
		if(input.elapsedSeconds % 3 < 1) updateHomeGoal(input.car.team);
		
		//Drive down the wall
		if(Behaviour.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		//Recovery
		if(!hasAction() && Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 target = getTarget(input);
		
		double steerRadians = Handling.aim(input.car, target);
		double distance = target.distance(input.car.position.flatten());
		boolean reverse = false;
		
		if(!hasAction() && input.car.hasWheelContact && distance > 2350 && !input.car.isSupersonic && input.car.position.z < 100){
			if(input.car.velocity.magnitude() > (input.car.boost == 0 ? 1250 : 1500) && Math.abs(steerRadians) < 0.4){
				currentAction = new DodgeAction(this, input.ball.position.distanceFlat(input.car.position) > 600 ? steerRadians : Handling.aim(input.car, input.ball.position.flatten()), input);
			}else if(Math.abs(steerRadians) > Math.PI * 2.7){
				double forwardVelocity = input.car.forwardMagnitude();
				reverse = (forwardVelocity < 0);
				if(reverse && forwardVelocity < -1000) currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		float throttle;
		if(distance < 3000 && input.car.velocity.magnitude() > distance * 2){
			throttle = (reverse ? 1 : -1);
			wildfire.sendQuickChat(QuickChatSelection.Information_AllYours);
		}else{
			throttle = (float)distance / 4000F * (reverse ? -1 : 1);
			if(distance > 3500 && input.car.isSupersonic) wildfire.sendQuickChat(QuickChatSelection.Information_Defending);
		}
		
		if(reverse){
			steerRadians = Utils.invertAim(steerRadians);
		}else{
			steerRadians = -steerRadians;
		}
		
        return new ControlsOutput().withSteer((float)steerRadians * 3F).withThrottle(throttle)
        		.withBoost(distance > 1500 && Math.abs(steerRadians) < 0.2F && !Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()))
        		.withSlide(input.car.velocity.magnitude() < 1600 && Math.abs(steerRadians) > 1.3F);
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
		
		wildfire.renderer.drawCircle(Color.BLACK, target, (bestBoost == null ? Constants.BALLRADIUS : 45));
		if(bestBoost == null) return target;
		
		wildfire.renderer.drawCircle(Color.BLUE, bestBoost.getLocation().flatten(), 70);
		return bestBoost.getLocation().flatten();
	}
	
	/**
	 * I don't know why I made this a method
	 */
	private void updateHomeGoal(int team){
		homeGoal = Constants.homeGoal(team);
	}

}