package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.State;

public class ShootState extends State {
	
	private final boolean smartDodgeEnabled = true;

	public ShootState(Wildfire wildfire){
		super("Shoot", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		//Not during kickoff or inside their net
		if(Utils.isKickoff(input) || Utils.teamSign(input.car) * input.car.position.y > Utils.PITCHLENGTH) return false;
		
		//Not while in goal
		if(Utils.isOnTarget(wildfire.ballPrediction, input.car.team) && Math.abs(input.car.position.y) > 4500) return false;
		
		//Not while the ball is being awkward
		if(wildfire.impactPoint.getPosition().z > 200) return false;
		
		//Not during a weird dribble
		if(input.ball.position.distanceFlat(input.car.position) < Utils.BALLRADIUS && input.ball.position.z > 110 && input.ball.position.distanceFlat(Utils.enemyGoal(input.car.team)) > 6000){
			return false;
		}
		
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		if(Math.abs(aimBall) > Math.PI * 0.7 && input.car.velocity.magnitude() > 1100) return false;
		return Utils.isInCone(input.car, wildfire.impactPoint.getPosition(), -120);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double distance = input.ball.position.distance(input.car.position);
		
		if(!hasAction()){
			if(input.car.hasWheelContact){
				double steerImpact = Utils.aim(input.car, input.ball.position.flatten());
				if(Math.abs(aimBall) > Math.PI * 0.7 && distance < 550){
					currentAction = new HalfFlipAction(this, input.elapsedSeconds);
				}else if(Math.abs(aimBall) > Math.PI * 0.6 && distance > 500 && input.car.velocity.magnitude() < 600 && input.ball.velocity.magnitude() < 1200){
					currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
				}else if((distance < 550 && Math.abs(aimBall) < Math.PI * 0.4) || (distance > 2400 && Math.abs(steerImpact) < 0.2 && !input.car.isSupersonic)){
					if(!input.ball.velocity.isZero()) wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Reactions_Whew);
					
					double forwardVelocity = input.car.forwardMagnitude();
					if(forwardVelocity >= 0 && (wildfire.impactPoint.getPosition().z - input.car.position.z) > 220 && smartDodgeEnabled){
						currentAction = new SmartDodgeAction(this, input, true);
					}else if(forwardVelocity > 1200){
						currentAction = new DodgeAction(this, steerImpact, input);
					}
				}
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle = (float)Math.abs(Math.cos(aimBall));
		
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.BLACK, 0, wildfire.impactPoint.getFrame());
		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH).toFramework());
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.LIGHT_GRAY, 125);
		
        return new ControlsOutput().withSteer((float)-aimBall * 2F).withThrottle(throttle).withBoost(Math.abs(aimBall) < 0.15F).withSlide(Math.abs(aimBall) > 1.4F);
	}

}
