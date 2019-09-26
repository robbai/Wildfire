package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class ShadowState extends State {

	private Vector2 homeGoal;

	public ShadowState(String name, Wildfire wildfire){
		super(name, wildfire);
	}

	public ShadowState(Wildfire wildfire){
		this("Shadow (Old)", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		this.homeGoal = Constants.homeGoal(input.car.team);
		
		// Avoid stopping forever.
		if(avoidStoppingForever(input)) return false;
		
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition());
		
		// Zooming at the ball.
		double velocityImpactCorrection = input.car.velocity.flatten().correctionAngle(input.info.impact.getPosition().minus(input.car.position).flatten());
		if(Math.abs(velocityImpactCorrection) < 0.25 && input.info.impact.getTime() < (correctSide ? 1.6 : 1.2)
				&& input.car.forwardVelocity > 900 && input.info.impact.getPosition().y * input.car.sign < -1500) return false;
		
		// Ball must not be close to our net.
		if(input.ball.position.flatten().distance(homeGoal) < 3500) return false; // || input.car.sign * input.ball.position.y < -4700
		if(input.car.sign * input.ball.velocity.y < -2900) return false;
		
		// We're on the wrong side of the ball.
		if(!correctSide && input.ball.velocity.y * input.car.sign < 1100){
			double opponentDist = Behaviour.closestOpponentDistance(input, input.ball.position);
			return opponentDist > Math.max(1500, input.info.impact.getTime() * 1200) || Behaviour.isTowardsOwnGoal(input.car, input.info.impact.getPosition());
		}
		
		// The ball must not be centralised.
		if(Math.abs(input.ball.position.x) < (Behaviour.isOpponentBehindBall(input) ? 1600 : 1400)) return false;
		
		// Outside of the "useful hitting arc".
		if(Math.abs(input.ball.position.y) < 3500 && new Vector3(0, -input.car.sign, 0).angle(input.car.position.minus(input.info.impact.getPosition())) > Math.PI * 0.55){
			if(input.info.impact.getPosition().distanceFlat(input.car.position) > 2800) return true;
		}
		
		// There is no defender.
		if(!Behaviour.isOpponentBehindBall(input)) return false;
		
		return Math.abs(Handling.aim(input.car, input.info.impact.getPosition().flatten())) > Math.PI * 0.6 && input.car.sign * input.ball.velocity.y > -800;
	}
	
	@Override
	public boolean expire(InfoPacket input){		
		// Avoid stopping forever.
		if(avoidStoppingForever(input)) return true;
		
		double distance = input.info.impact.getPosition().distanceFlat(input.car.position);
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition());
				
		// Aiming very close to the ball, and close-by.		
		if(Math.abs(Handling.aim(input.car, input.info.impact.getPosition().flatten())) < 0.225 && distance < 3300) return true;
		
		// Ball is centralised.
		if(correctSide && Math.abs(input.ball.position.x) < (Behaviour.hasTeammate(input) ? 1400 : 1200) && distance < 7000) return true;
		
		// Ball is close to our net.
		if(input.ball.position.flatten().distance(homeGoal) < 3300 || input.car.sign * input.ball.position.y < -4500) return true;
		if(input.car.sign * input.ball.velocity.y < -2400) return true;		
		
		// Beating the opponent.
		double ourDistance = input.car.position.distance(input.ball.position);
		if(ourDistance < (Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition().flatten()) ? 1400 : 1650)) return true;
		double closestOpponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		return !Behaviour.isTeammateCloser(input) && (closestOpponentDistance > Math.max(1800, ourDistance * 1.35D) || input.car.sign * input.ball.velocity.y < -850);
	}

	private boolean avoidStoppingForever(InfoPacket input){
		return input.ball.velocity.magnitude() < 500 && Math.signum(input.ball.position.y) != input.car.sign;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		// Drive down the wall.
		if(Behaviour.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		// Recovery.
		if(Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 target = getTarget(input);
		
		double radians = Handling.aim(input.car, target);
		double distance = target.distance(input.car.position.flatten());
		boolean reverse = false;
		
		// Actions.
		if(input.car.hasWheelContact && distance > 2350 && !input.car.isSupersonic && input.car.position.z < 100){
			double velocityNoseComponent = input.car.velocity.normalized().dotProduct(input.car.orientation.noseVector);
			if(input.car.velocity.magnitude() > (input.car.boost == 0 ? 1250 : 1500) && Math.abs(radians) < 0.3 && velocityNoseComponent > 0.95){
				double componentTowardsBall = input.car.velocity.normalized().dotProduct(input.info.impact.getPosition().minus(input.car.position).normalized());
				double dodgeRadians = (input.info.impact.getPosition().distance(input.car.position) > (componentTowardsBall > 0.9 ? 500 : 250) ? radians : Handling.aim(input.car, input.ball.position.flatten()));
				currentAction = new DodgeAction(this, dodgeRadians, input);
			}else if(Math.abs(radians) > Math.toRadians(120)){
				double forwardVelocity = input.car.forwardVelocity;
				reverse = (forwardVelocity < 0);
				if(forwardVelocity < -950 && velocityNoseComponent < -0.9) currentAction = new HalfFlipAction(this, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		double throttle;
		if(distance < 3000 && input.car.velocityDir(target.withZ(input.car.position.z).minus(input.car.position)) > distance * 2){
			throttle = (reverse ? 1 : -1);
			wildfire.sendQuickChat(QuickChatSelection.Information_AllYours);
		}else{
			throttle = (distance / 4000 * (reverse ? -1 : 1));
			if(distance > 3500 && input.car.isSupersonic) wildfire.sendQuickChat(QuickChatSelection.Information_Defending);
		}
		
		ControlsOutput controls = (reverse ? Handling.steeringBackwards(input.car, target) : Handling.steering(input.car, target));
		return controls.withThrottle(throttle)
				.withBoost(controls.holdBoost() && throttle > 0.9 && distance > 1300 && !Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition()));
	}

	/**
	 * Get a point halfway between the ball and our goal
	*/
	private Vector2 getTarget(InfoPacket input){
		Vector2 target = homeGoal.plus(input.ball.position.flatten().minus(homeGoal).scaled(0.5D));
		
		BoostPad bestBoost = null;
		if(input.car.boost <= 70 && input.ball.position.y * input.car.sign > 1000){
			double bestBoostDistance = 0;
			for(BoostPad b : BoostManager.getSmallBoosts()){
				if(!b.isActive() || Math.signum(b.getLocation().y - target.y) == input.car.sign) continue;
				double distance = b.getLocation().distanceFlat(target);
				if(bestBoost == null || distance < bestBoostDistance){
					bestBoost = b;
					bestBoostDistance = distance;
				}
			}
		}
		
		wildfire.renderer.drawCircle(Color.BLACK, target, (bestBoost == null ? Constants.BALL_RADIUS : 45));
		if(bestBoost == null) return target;
		
		wildfire.renderer.drawCircle(Color.BLUE, bestBoost.getLocation().flatten(), 70);
		return bestBoost.getLocation().flatten();
	}

}