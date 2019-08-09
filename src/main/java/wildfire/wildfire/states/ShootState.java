package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class ShootState extends State {
	
	private final double minThrottle = 0.2;

	public ShootState(Wildfire wildfire){
		super("Shoot", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		//Not during kickoff or inside their net
		if(Behaviour.isKickoff(input) || Utils.teamSign(input.car) * input.car.position.y > Constants.PITCHLENGTH) return false;
		
		//Not while in goal
		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team) && Math.abs(input.car.position.y) > 4500) return false;
		
		//Not while the ball is being awkward
		if(wildfire.impactPoint.getPosition().z > 200) return false;
		
		//Not during a weird dribble
		if(input.ball.position.distanceFlat(input.car.position) < Constants.BALLRADIUS && input.ball.position.z > 110){ // && input.ball.position.distanceFlat(Utils.enemyGoal(input.car.team)) > 6000
			return false;
		}
		
		//Don't commit to a shot if it will take a long time
		if(wildfire.impactPoint.getTime() > 3 && !input.car.isSupersonic) return false;
		
		double aimBall = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		if(Math.abs(aimBall) > Math.PI * 0.7 && input.car.velocity.magnitude() > 1100) return false;
		return Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition(), -180);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double impactRadians = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double ballDistance = input.ball.position.distance(input.car.position);
				
		/*
		 * Actions.
		 */
		if(input.car.hasWheelContact){
			double ballVelocityTowards = input.ball.velocity.normalized().dotProduct(input.car.position.minus(input.ball.position).normalized());
			boolean dodgeBallDist = (ballDistance < Utils.lerp(270, 350, Math.abs(input.car.forwardVelocity) / Constants.MAXCARSPEED) + 150 * ballVelocityTowards);
			if(Math.abs(impactRadians) > Math.PI * 0.7 && ballDistance < 560){
				currentAction = new HalfFlipAction(this, input.car);
			}else if(Math.abs(impactRadians) > Math.PI * 0.6 && ballDistance > 500 && input.car.velocity.magnitude() < 600 && input.ball.velocity.magnitude() < 1200){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
			}else if((dodgeBallDist && Math.abs(impactRadians) < 0.3) || (wildfire.impactPoint.getTime() > 2 && Math.abs(impactRadians) < 0.25 && input.car.velocity.magnitude() > 1800)){
				if(!input.ball.velocity.isZero()) wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Reactions_Whew);

				double forwardVelocity = input.car.forwardVelocity;
				if(forwardVelocity > 1300) currentAction = new DodgeAction(this, (dodgeBallDist ? 2 : 1) * impactRadians, input);
			}
		}else if(Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
		}
		if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		currentAction = null;
		
		double throttle = (Math.abs(Math.cos(impactRadians)) * (1D - minThrottle) + minThrottle);
		
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.BLACK, 0, wildfire.impactPoint.getFrame());
		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH).toFramework());
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.LIGHT_GRAY, 125);
		
        return new ControlsOutput().withSteer((float)-impactRadians * 3F).withThrottle(throttle)
        		.withBoost(Math.abs(impactRadians) < 0.14F && (!input.car.isSupersonic || !Behaviour.isOpponentBehindBall(input)))
        		.withSlide(Math.abs(impactRadians) > 1.4F);
	}

}
