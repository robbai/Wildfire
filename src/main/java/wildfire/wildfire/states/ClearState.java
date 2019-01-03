package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

public class ClearState extends State {
	
	private final double homeZoneSize = 3500D;

	public ClearState(Wildfire wildfire){
		super("Clear", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		if(Utils.isKickoff(input) || Utils.isCarAirborne(input.car) || !Utils.correctSideOfTarget(input.car, input.ball.position)) return false;

		//Check if we have a shot opportunity
		if(!Utils.isOpponentBehindBall(input) && wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 2000){
			double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.4){
				if(Utils.canShoot(input.car, wildfire.impactPoint.getPosition())) return false;
			}
		}
		
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, input.car.team);
		if(!onTarget && Utils.teamSign(input.car.team) * input.ball.velocity.y > -1000) return false;
		
		return Utils.defendNotReturn(input, wildfire.impactPoint.getPosition(), homeZoneSize, onTarget);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		boolean wall = Utils.isOnWall(input.car);
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
		double steer = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
				
		//Dodge or half-flip into the ball
		if(!hasAction() && input.car.position.distanceFlat(input.ball.position) < 400){
			if(Math.abs(steer) < 0.75 * Math.PI){
				currentAction = new DodgeAction(this, steer, input);
			}else{
				currentAction = new HalfFlipAction(this);
			}
			if(!currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Aerial
		double ballSpeedAtCar = input.ball.velocity.magnitude() * Math.cos(input.ball.velocity.flatten().correctionAngle((input.car.position.minus(input.ball.position).flatten()))); 
		if(!hasAction() && wildfire.impactPoint.getPosition().z > (ballSpeedAtCar > 700 ? 230 : 400) && Utils.isEnoughBoostForAerial(input.car, wildfire.impactPoint.getPosition()) && input.car.hasWheelContact && Math.abs(steer) < 0.3 && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < -1200){
			double maxRange = wildfire.impactPoint.getPosition().z * 5;
			double minRange = wildfire.impactPoint.getPosition().z * 1;
			if(Utils.isPointWithinRange(input.car.position.flatten(), wildfire.impactPoint.getPosition().flatten(), minRange, maxRange)){
				currentAction = new AerialAction(this, input, wildfire.impactPoint.getPosition().z > 800);
			}
		}
		
		double yDifference = input.car.position.y - input.ball.position.y;
		boolean behindBall = (Math.signum(yDifference) != Utils.teamSign(input.car));

		//We are in position for the ball to hit us (and we can't quickly turn towards the ball)
		if(behindBall){ 
			wildfire.renderer.drawCircle(Color.GREEN, Utils.homeGoal(input.car.team), homeZoneSize);
			//63 degrees is the minimum
			if(input.car.position.y * Utils.teamSign(input.car) > -5050 && Math.abs(steer) > 0.35 * Math.PI){
				//We don't want to wait too long for the ball to reach us
				double ballTime = Math.abs(yDifference / input.ball.velocity.y);
				if(ballTime < 1.4){
					Vector2 intersect = Utils.traceToY(input.ball.position.flatten(), input.ball.velocity.flatten(), input.car.position.y);

					//Check if our X-coordinate is close-by when we should intersect with the ball's path
					boolean closeby = Math.abs(input.car.position.x - intersect.x) < 140;
					wildfire.renderer.drawLine3d(closeby ? Color.CYAN : Color.BLUE, input.ball.position.flatten().toFramework(), intersect.toFramework());
					if(closeby){
						wildfire.sendQuickChat(QuickChatSelection.Information_InPosition);
						wildfire.renderer.drawString2d("Stop (" + (int)Math.abs(input.car.position.x - intersect.x) + ")", Color.WHITE, new Point(0, 20), 2, 2);
						return stayStill(input);
					}
				}
			}
		}

		wildfire.renderer.drawString2d("Smack", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 125);
		return drivePoint(input, wildfire.impactPoint.getPosition().flatten(), true);
	}
	
	private ControlsOutput drivePoint(DataPacket input, Vector2 point, boolean rush){
		float steer = (float)Utils.aim(input.car, point);
		float throttle = rush ? 1F : (float)Math.signum(Math.cos(steer));
		boolean reverse = throttle < 0;
		return new ControlsOutput().withThrottle(throttle).withBoost(!reverse && Math.abs(steer) < 0.325 && !input.car.isSupersonic).withSteer(-(reverse ? (float)Utils.invertAim(steer) : steer) * 2F).withSlide(rush && Math.abs(steer) > Math.PI * 0.5);
	}
	
	private ControlsOutput stayStill(DataPacket input){
		return new ControlsOutput().withThrottle((float)-input.car.forwardMagnitude() / 100).withBoost(false);
	}

}
