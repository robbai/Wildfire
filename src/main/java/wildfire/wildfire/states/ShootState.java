package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
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
	public boolean ready(InfoPacket input){
		CarData car = input.car;
		
		// Not during kickoff or inside their net.
		if(Behaviour.isKickoff(input) || car.sign * car.position.y > Constants.PITCH_LENGTH) return false;
		
		// Not while in goal.
		if(Behaviour.isOnTarget(wildfire.ballPrediction, car.team) && Math.abs(car.position.y) > 4500) return false;
		
		// Not while the ball is being awkward.
		if(input.info.impact.getPosition().z > 200) return false;
		
		// Not during a weird dribble.
		if(input.ball.position.distanceFlat(car.position) < Constants.BALL_RADIUS && input.ball.position.z > 110){ // && input.ball.position.distanceFlat(Utils.enemyGoal(car.team)) > 6000
			return false;
		}
		
		// Don't commit to a shot if it will take a long time.
		if(input.info.impact.getTime() > 3 && !car.isSupersonic) return false;
		
		double aimBall = Handling.aim(car, input.info.impact.getPosition().flatten());
		if(Math.abs(aimBall) > Math.PI * 0.7 && car.velocity.magnitude() > 1100) return false;
		return Behaviour.isInCone(car, input.info.impact.getPosition(), -200);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		double impactRadians = Handling.aim(car, input.info.impact.getPosition());
		double ballDistance = input.ball.position.distance(car.position);
		
		// Smart-dodge.
		if(input.info.jumpImpactHeight > 100){
			SmartDodgeAction smartDodge = new SmartDodgeAction(this, input, false);
			if(!smartDodge.failed){
				return this.startAction(smartDodge, input);
			}
			return Handling.arriveAtSmartDodgeCandidate(car, input.info.impact, wildfire.renderer);
		}
				
		// Actions.
		if(car.hasWheelContact){
// 			double ballVelocityTowards = input.ball.velocity.normalized().dotProduct(car.position.minus(input.ball.position).normalized());
			if(Math.abs(impactRadians) > Math.PI * 0.7 && ballDistance < 560){
				currentAction = new HalfFlipAction(this, input);
			}else if(Math.abs(impactRadians) > Math.PI * 0.6 && ballDistance > 500 && car.velocity.magnitude() < 600 && input.ball.velocity.magnitude() < 1200){
				currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
			}else if((input.info.impact.getTime() < 0.3 && Math.abs(impactRadians) < 0.3) || (input.info.impact.getTime() > 2 && Math.abs(impactRadians) < 0.25 && car.velocity.magnitude() > 1800)){
				if(!input.ball.velocity.isZero()) wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Reactions_Whew);

				double forwardVelocity = car.forwardVelocity;
				if(forwardVelocity > 1300) currentAction = new DodgeAction(this, impactRadians, input);
			}
		}else if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
		}
		if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		currentAction = null;
		
		// Rendering.
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.BLACK, 0, input.info.impact.getFrame());
		wildfire.renderer.drawLine3d(Color.WHITE, car.position.flatten(), Utils.traceToY(car.position.flatten(), input.info.impact.getPosition().minus(car.position).flatten(), car.sign * Constants.PITCH_LENGTH));
		wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition(), Color.LIGHT_GRAY, 125);
		
		// Controls.
		double throttle = (Math.abs(Math.cos(impactRadians)) * (1D - minThrottle) + minThrottle);
		if(car.orientation.up.z < 0.7) throttle = Math.signum(throttle);
        ControlsOutput controls = Handling.forwardDrive(car, input.info.impact.getPosition(), false);
        controls.withThrottle(throttle).withBoost(controls.holdBoost() && throttle > 0.9);
        return controls;
	}

}
