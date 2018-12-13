package rlbotexample.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.DodgeAction;
import rlbotexample.wildfire.actions.HalfFlipAction;
import rlbotexample.wildfire.actions.HopAction;
import rlbotexample.wildfire.actions.RecoveryAction;

public class ShootState extends State {
	
	Vector2 left, right;

	public ShootState(Wildfire wildfire){
		super("Shoot", wildfire);
		left = new Vector2(Utils.teamSign(wildfire.team) * (Utils.GOALHALFWIDTH - Utils.BALLRADIUS), Utils.teamSign(wildfire.team) * Utils.PITCHLENGTH);
		right = new Vector2(Utils.teamSign(wildfire.team) * (-Utils.GOALHALFWIDTH + Utils.BALLRADIUS), Utils.teamSign(wildfire.team) * Utils.PITCHLENGTH);
	}

	@Override
	public boolean ready(DataPacket input){
		//Not during kickoff
		if(Utils.isKickoff(input)) return false;
		
		//Not while in goal
		if(Utils.isOnTarget(wildfire.ballPrediction, input.car.team) && Math.abs(input.car.position.y) > 4500) return false;
		
		//Not while the ball is being awkward
		if(input.ball.position.z > 350 || Math.signum(wildfire.impactPoint.y - input.car.position.y) != Utils.teamSign(input.car)) return false;
		
		//Not during a weird dribble
		if(input.ball.position.distanceFlat(input.car.position) < Utils.BALLRADIUS && input.ball.position.z > 80 && input.ball.position.distanceFlat(Utils.enemyGoal(wildfire.team)) > 7000) return false;
		
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.flatten());
		if(Math.abs(aimBall) > Math.PI * 0.8 && input.car.velocity.magnitude() > 1100) return false;
		return Utils.canShoot(input.car, wildfire.impactPoint);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double aimBall = Utils.aim(input.car, wildfire.impactPoint.flatten());
		double distance = input.ball.position.distance(input.car.position);
		
		if(!hasAction()){
			if(input.car.hasWheelContact){
				double steerBall = Utils.aim(input.car, input.ball.position.flatten());
				if(Math.abs(aimBall) > Math.PI * 0.7 && distance < 500){
					currentAction = new HalfFlipAction(this);
				}else if(Math.abs(aimBall) > Math.PI * 0.6 && distance > 400 && input.car.velocity.magnitude() < 600){
					currentAction = new HopAction(this, wildfire.impactPoint.flatten(), input.car.velocity);
				}else if(distance < 500 || (distance > 3000 && Math.abs(steerBall) < 0.08)){
					currentAction = new DodgeAction(this, steerBall, input);
				}
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		float throttle = (float)Math.abs(Math.cos(aimBall));
		
		Utils.drawCrosshair(wildfire.renderer, input.car, wildfire.impactPoint, Color.MAGENTA, 125);
		
		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), left.toFramework());
		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), right.toFramework());
		
		wildfire.renderer.drawString2d("Aim: " + Utils.round(aimBall), Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Throttle: " + Utils.round(throttle), Color.WHITE, new Point(0, 40), 2, 2);
		
        return new ControlsOutput().withSteer((float)-aimBall * 2F).withThrottle(throttle).withBoost(Math.abs(aimBall) < 0.15F).withSlide(Math.abs(aimBall) > 2F);
	}

}
