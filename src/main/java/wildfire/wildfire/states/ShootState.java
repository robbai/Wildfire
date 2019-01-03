package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class ShootState extends State {

	public ShootState(Wildfire wildfire){
		super("Shoot", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		//Not during kickoff
		if(Utils.isKickoff(input)) return false;
		
		//Not while in goal
		if(Utils.isOnTarget(wildfire.ballPrediction, input.car.team) && Math.abs(input.car.position.y) > 4500) return false;
		
		//Not while the ball is being awkward
		if(input.ball.position.z > 350 || Math.signum(wildfire.impactPoint.getPosition().y - input.car.position.y) != Utils.teamSign(input.car)) return false;
		
		//Not during a weird dribble
		if(input.ball.position.distanceFlat(input.car.position) < Utils.BALLRADIUS && input.ball.position.z > 80 && input.ball.position.distanceFlat(Utils.enemyGoal(input.car.team)) > 7000) return false;
		
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		if(Math.abs(aimBall) > Math.PI * 0.8 && input.car.velocity.magnitude() > 1100) return false;
		return Utils.canShoot(input.car, wildfire.impactPoint.getPosition());
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double distance = input.ball.position.distance(input.car.position);
		
		if(!hasAction()){
			if(input.car.hasWheelContact){
				double steerBall = Utils.aim(input.car, input.ball.position.flatten());
				if(Math.abs(aimBall) > Math.PI * 0.7 && distance < 500){
					currentAction = new HalfFlipAction(this);
				}else if(Math.abs(aimBall) > Math.PI * 0.6 && distance > 400 && input.car.velocity.magnitude() < 600){
					currentAction = new HopAction(this, wildfire.impactPoint.getPosition().flatten(), input.car.velocity);
				}else if(distance < 500 || (distance > 3000 && Math.abs(steerBall) < 0.08)){
					if(!input.ball.velocity.isZero()) wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Reactions_Whew);
					currentAction = new DodgeAction(this, steerBall, input);
				}
//				else if(wildfire.impactPoint.getPosition().z > 400 && Utils.isEnoughBoostForAerial(input.car, wildfire.impactPoint.getPosition())){
//					double maxRange = wildfire.impactPoint.getPosition().z * 4;
//					double minRange = wildfire.impactPoint.getPosition().z * 1;
//					wildfire.renderer.drawCircle(Color.ORANGE, wildfire.impactPoint.getPosition().flatten(), minRange);
//					wildfire.renderer.drawCircle(Color.ORANGE, wildfire.impactPoint.getPosition().flatten(), maxRange);
//					if(Utils.isPointWithinRange(input.car.position.flatten(), wildfire.impactPoint.getPosition().flatten(), minRange, maxRange)){
//						currentAction = new AerialAction(this, input, wildfire.impactPoint.getPosition().z > 600);
//						return currentAction.getOutput(input);
//					}
//				}
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle = (float)Math.abs(Math.cos(aimBall));
		
		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH).toFramework());
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.LIGHT_GRAY, 125);
		
		wildfire.renderer.drawString2d("Aim: " + Utils.round(aimBall), Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Throttle: " + Utils.round(throttle), Color.WHITE, new Point(0, 40), 2, 2);
		
        return new ControlsOutput().withSteer((float)-aimBall * 2F).withThrottle(throttle).withBoost(Math.abs(aimBall) < 0.15F).withSlide(Math.abs(aimBall) > 1.4F);
	}

}
