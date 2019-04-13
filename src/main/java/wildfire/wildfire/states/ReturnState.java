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
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.obj.State;

public class ReturnState extends State {
	
	/*How small the angle between the attacker and the ball has to be*/
	private final double maxAttackingAngle = 0.5 * Math.PI;
	
	/*How small the difference of the angle from the attacker to the ball and the attacker to the goal has to be*/
	private final double maxShootingAngle = 0.45 * Math.PI;
	
//	private final double homeZoneSize = 3650D;

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
		
		//Just hit it instead
		if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < (input.car.isSupersonic ? 2000 : 1200) && !Utils.isTowardsOwnGoal(input.car, wildfire.impactPoint.getPosition())){
			return false;
		}
		
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, input.car.team);
		
		Vector2 homeGoal = Utils.homeGoal(input.car.team);
		if(input.car.position.distanceFlat(homeGoal) < 3600){
			if(!onTarget && !Utils.isOpponentBehindBall(input)) return false;
			if(Utils.isTeammateCloser(input)) return wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 5500;
		}
		
//		//Ball is rapidly approaching our half
//		if(Utils.teamSign(input.car.team) * input.ball.velocity.y > 2400 || Utils.teamSign(input.car.team) * input.ball.position.y > -2900){
//			return false;
//		}
//		
//		return wildfire.impactPoint.getPosition().distanceFlat(homeGoal) > 3500 && !onTarget;
		
		if(!Utils.isOpponentBehindBall(input) || Utils.closestOpponentDistance(input, input.ball.position) > 4000) return false;
		return Utils.teamSign(input.car) * input.car.position.y < -2750 && wildfire.impactPoint.getTime() > 2.4;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		boolean wall = Utils.isOnWall(input.car);
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
		double aimImpact = Utils.aim(input.car, this.wildfire.impactPoint.getPosition().flatten());
				
		//Dodge or half-flip into the ball
		if(!hasAction() && input.car.position.distanceFlat(input.ball.position) < 400){
			if(Math.abs(aimImpact) < 0.75 * Math.PI){
				currentAction = new DodgeAction(this, aimImpact, input);
			}else{
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(!currentAction.failed) return currentAction.getOutput(input);
		}

		//Block the attack!
		CarData attacker = getAttacker(input);

		if(attacker != null){
			wildfire.renderer.drawString2d("Attacker: '" + attacker.name + "'", Color.WHITE, new Point(0, 20), 2, 2);

			Vector2 target = Utils.getTarget(attacker, input.ball);
			target = target.withY(Utils.teamSign(input.car) * -4900);

			wildfire.renderer.drawLine3d(Color.RED, attacker.position.flatten().toFramework(), target.toFramework());
			wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.RED, 125);

			//Rush them
			if(wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 2000 || input.ball.position.minus(attacker.position).flatten().angle(input.car.position.minus(attacker.position).flatten()) < 0.25){
				wildfire.sendQuickChat(QuickChatSelection.Information_Incoming);
				wildfire.renderer.drawString2d("Rush", Color.WHITE, new Point(0, 40), 2, 2);
				wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 125);
				return drivePoint(input, wildfire.impactPoint.getPosition().flatten(), true);
			}else{
				//Get in the way of their predicted shot
				wildfire.renderer.drawString2d("Align", Color.WHITE, new Point(0, 40), 2, 2);
				if(target.distance(input.car.position.flatten()) < 300){
					//Already there!
					return stayStill(input); 
				}else{
					wildfire.renderer.drawLine3d(Color.RED, input.car.position.flatten().toFramework(), target.toFramework());
					
					//Front flip for speed
					if(!hasAction() && Utils.homeGoal(input.car.team).distance(input.car.position.flatten()) > 2800 && !input.car.isSupersonic && input.car.boost < 45){
						if(Math.abs(Utils.aim(input.car, Utils.homeGoal(input.car.team))) < 0.4 && input.car.velocity.magnitude() > 1000){
							currentAction = new DodgeAction(this, 0, input);
							if(currentAction == null || currentAction.failed){
								currentAction = null;
							}else{
								return currentAction.getOutput(input);
							}
						}
					}
					
					//We better get there!
					return drivePoint(input, target.withX(Math.max(-500, Math.min(500, target.x))), false); 
				}
			}
		}

		//Get back to goal
		Vector2 homeGoal = Utils.homeGoal(input.car.team);
		if(homeGoal.distance(input.car.position.flatten()) < 200 && input.car.hasWheelContact){
			if(Math.abs(aimImpact) > 0.4 * Math.PI && input.car.velocity.magnitude() < 800 && wildfire.impactPoint.getTime() > 1.8){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
				if(!currentAction.failed) return currentAction.getOutput(input);
			}
			return stayStill(input);
		}
		return drivePoint(input, homeGoal, false);
	}

	private CarData getAttacker(DataPacket input){
		double shortestDistance = 4500;
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
		
		float throttle = (rush ? 1 : (float)Math.signum(Math.cos(steer)));
		double distance = input.car.position.distanceFlat(point);
		
		boolean reverse = (throttle < 0);
		if(reverse) steer = (float)-Utils.invertAim(steer);
		
		return new ControlsOutput().withThrottle(throttle).withBoost(!reverse && Math.abs(steer) < 0.325 && !input.car.isSupersonic && distance > (rush ? 1200 : 2000)).withSteer(-steer * 3F).withSlide(rush && Math.abs(steer) > Math.PI * 0.5);
	}
	
	private ControlsOutput stayStill(DataPacket input){
		return new ControlsOutput().withThrottle((float)-input.car.forwardMagnitude() / 2500).withBoost(false);
	}

}
