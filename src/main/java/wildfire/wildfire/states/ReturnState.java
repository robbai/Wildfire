package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

public class ReturnState extends State {
	
	/*How small the angle between the attacker and the ball has to be*/
	private final double maxAttackingAngle = 0.5 * Math.PI;
	
	/*How small the difference of the angle from the attacker to the ball and the attacker to the goal has to be*/
	private final double maxShootingAngle = 0.4 * Math.PI;
	
	private final double homeZoneSize = 3500D;

	public ReturnState(Wildfire wildfire){
		super("Return", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		if(Utils.isKickoff(input) || Utils.isCarAirborne(input.car)) return false;

		//Check if we have a shot opportunity
		if(!Utils.isOpponentBehindBall(input) && wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 2000){
			double aimBall = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.4){
				if(Utils.isInCone(input.car, wildfire.impactPoint.getPosition())) return false;
			}
		}
		
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, input.car.team);
		if(!onTarget && Utils.teamSign(input.car.team) * input.ball.velocity.y > -1000) return false;
		
		//Just hit it instead
		if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1200 && !Utils.isTowardsOwnGoal(input.car, wildfire.impactPoint.getPosition().flatten())){
			return false;
		}
		
		return !Utils.defendNotReturn(input, wildfire.impactPoint.getPosition(), homeZoneSize, onTarget);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		boolean wall = Utils.isOnWall(input.car);
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
				
		//Dodge or half-flip into the ball
		if(!hasAction() && input.car.position.distanceFlat(input.ball.position) < 400){
			double aim = Utils.aim(input.car, this.wildfire.impactPoint.getPosition().flatten());
			if(Math.abs(aim) < 0.75 * Math.PI){
				currentAction = new DodgeAction(this, aim, input);
			}else{
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(!currentAction.failed) return currentAction.getOutput(input);
		}


		//Block the attack!
		CarData attacker = getAttacker(input);

		if(attacker != null){
			wildfire.renderer.drawString2d("Attacker " + attacker.toString(), Color.WHITE, new Point(0, 20), 2, 2);

			Vector2 target = Utils.getTarget(attacker, input.ball);
			target = target.withY(Utils.teamSign(input.car) * -5000);

			wildfire.renderer.drawLine3d(Color.RED, attacker.position.flatten().toFramework(), target.toFramework());
			wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.RED, 125);

			//Rush them
			if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1200 || Math.abs(input.ball.position.minus(attacker.position).flatten().correctionAngle(input.car.position.minus(attacker.position).flatten())) < 0.225){
				wildfire.sendQuickChat(QuickChatSelection.Information_Incoming);
				wildfire.renderer.drawString2d("Block", Color.WHITE, new Point(0, 40), 2, 2);
				wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 125);
				return drivePoint(input, wildfire.impactPoint.getPosition().flatten(), true);
			}else{
				//Get in the way of their predicted shot
				wildfire.renderer.drawString2d("Align", Color.WHITE, new Point(0, 40), 2, 2);
				if(target.distance(input.car.position.flatten()) < 200){
					return stayStill(input); //We there
				}else{
					wildfire.renderer.drawLine3d(Color.RED, input.car.position.flatten().toFramework(), target.toFramework());
					return drivePoint(input, target, false); //We better get there
				}
			}
		}

		//Get back to goal
		Vector2 homeGoal = Utils.homeGoal(input.car.team);
		wildfire.renderer.drawString2d("Return", Color.WHITE, new Point(0, 20), 2, 2);
		return homeGoal.distance(input.car.position.flatten()) < 200 ? stayStill(input) : drivePoint(input, homeGoal, false);
	}

	private CarData getAttacker(DataPacket input){
		double shortestDistance = 4000;
		CarData attacker = null;
		for(CarData c : input.cars){
			if(c == null || c.team == input.car.team) continue;
			Vector2 target = Utils.getTarget(c, input.ball); //This represents the part of the goal that they're shooting at
			double distance = c.position.distanceFlat(input.ball.position);
			
			double attackingAngle = Utils.aim(c, input.ball.position.flatten());
			double shootingAngle = Math.abs(attackingAngle - Utils.aim(c, target));
			attackingAngle = Math.abs(attackingAngle);
			
			if(attackingAngle < maxAttackingAngle && shootingAngle < maxShootingAngle && distance < shortestDistance){
				shortestDistance = distance;
				attacker = c;
			}
		}
		return attacker;
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
