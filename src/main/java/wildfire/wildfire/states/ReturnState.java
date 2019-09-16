package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class ReturnState extends State {
	
	// How small the angle between the attacker and the ball has to be
	private final double maxAttackingAngle = 0.55 * Math.PI;
	
	// How small the difference of the angle from the attacker to the ball and the attacker to the goal has to be
	private final double maxShootingAngle = 0.4 * Math.PI;
	
	private final double goalScale = 0.95;

	public ReturnState(Wildfire wildfire){
		super("Return", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(Behaviour.isKickoff(input) || Behaviour.isCarAirborne(input.car)) return false;
		
		boolean opponentBehind = Behaviour.isOpponentBehindBall(input);
		
		if(input.info.impact.getTime() < input.info.enemyImpactTime - (opponentBehind ? 0.15 : 0.45)) return false;

		// Check if we have a shot opportunity.
		if(input.info.impact.getPosition().distanceFlat(input.car.position) < 2500){
			double aimBall = Handling.aim(input.car, input.info.impact.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.4){
				if(Behaviour.isInCone(input.car, input.info.impact.getPosition())) return false;
			}
		}		
		
		// Just hit it instead.
		if(input.info.impact.getPosition().distanceFlat(input.car.position) < Math.max(1100, input.car.velocity.magnitude() * 0.75)
				&& !Behaviour.isTowardsOwnGoal(input.car, input.info.impact.getPosition())){
			return false;
		}
		
		Vector2 homeGoal = Constants.homeGoal(input.car.team);
		if(input.car.position.distanceFlat(homeGoal) < 2800){
			boolean onTarget = Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team);
			if(!onTarget && !opponentBehind) return false;
			if(Behaviour.isTeammateCloser(input)){
				return input.info.impact.getTime() < (6D - DrivePhysics.maxVelocity(input.car.velocity.magnitude(), input.car.boost) / 1400D)
						&& input.info.impact.getTime() > 1.5;
			}
		}
		
		if(!opponentBehind || Behaviour.closestOpponentDistance(input, input.ball.position) > 3400) return false;
		return Utils.teamSign(input.car) * input.car.position.y < -2750 && input.info.impact.getTime() > 1.4;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		// Drive down the wall.
		boolean wall = Behaviour.isOnWall(input.car);
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		double impactRadians = Handling.aim(input.car, input.info.impact.getPosition().flatten());
				
//		// Dodge or half-flip into the ball.
//		if(input.car.position.distanceFlat(input.ball.position) < 400){
//			if(Math.abs(impactRadians) < 0.75 * Math.PI){
//				currentAction = new DodgeAction(this, impactRadians, input);
//			}else{
//				currentAction = new HalfFlipAction(this, input);
//			}
//			if(!currentAction.failed) return currentAction.getOutput(input);
//			currentAction = null;
//		}

		// Block the attack!
		CarData attacker = getAttacker(input);

		if(attacker != null){
			wildfire.renderer.drawString2d("Attacker: '" + attacker.name + "'", Color.WHITE, new Point(0, 20), 2, 2);

			Vector2 target = Behaviour.getTarget(attacker, input.ball);
			target = target.withY(Utils.teamSign(input.car) * -Constants.PITCH_LENGTH * goalScale);
			target = target.withX(Utils.clamp(target.x, Constants.GOAL_WIDTH - 250, -Constants.GOAL_WIDTH + 250));

			wildfire.renderer.drawLine3d(Color.RED, attacker.position.flatten().toFramework(), target.toFramework());
			wildfire.renderer.drawCrosshair(input.car, input.info.impact.getPosition(), Color.RED, 125);

			// Rush them.
			double impactDistance = input.info.impact.getPosition().distanceFlat(input.car.position);
			if(impactDistance < 1800 || (impactDistance < 2500 && input.ball.position.minus(attacker.position).flatten().angle(input.car.position.minus(attacker.position).flatten()) < 0.28)){
				wildfire.sendQuickChat(QuickChatSelection.Information_Incoming);
				wildfire.renderer.drawString2d("Rush", Color.WHITE, new Point(0, 40), 2, 2);
				wildfire.renderer.drawCrosshair(input.car, input.info.impact.getPosition(), Color.MAGENTA, 125);
				return Handling.chaosDrive(input.car, input.info.impact.getPosition().flatten(), true);
			}else{
				// Get in the way of their predicted shot.
				wildfire.renderer.drawString2d("Align", Color.WHITE, new Point(0, 40), 2, 2);
				
				if(target.distance(input.car.position.flatten()) < 300){
					// Already there!					
					if(doHop(input, impactRadians)){
						currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
						if(!currentAction.failed) return currentAction.getOutput(input);
					}
					return Handling.stayStill(input.car); 
				}else{
					wildfire.renderer.drawLine3d(Color.RED, input.car.position.flatten().toFramework(), target.toFramework());
					
					// Front flip or half-flip.
					if(Constants.homeGoal(input.car.team).distance(input.car.position.flatten()) > 2800 && !input.car.isSupersonic){
						double radiansGoal = Handling.aim(input.car, Constants.homeGoal(input.car.team));
						if(Math.abs(radiansGoal) < 0.3 && input.car.forwardVelocity > (input.car.boost < 1 ? 1200 : 1500)){
							currentAction = new DodgeAction(this, 0, input);
						}else if(Math.abs(radiansGoal) > Math.PI * 0.9 && input.car.forwardVelocity < -900){
							currentAction = new HalfFlipAction(this, input);
						}
					}
					if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
					currentAction = null;
					
					// We better get there!
					return Handling.chaosDrive(input.car, target.withX(Math.max(-500, Math.min(500, target.x))), false); 
				}
			}
		}

		// Get back to goal.
		Vector2 homeGoal = Constants.homeGoal(input.car.team).scaled(goalScale);
		if(homeGoal.distance(input.car.position.flatten()) < 200 && input.info.impact.getTime() > 1){
			if(doHop(input, impactRadians)){
				currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
				if(!currentAction.failed) return currentAction.getOutput(input);
			}
			return Handling.stayStill(input.car);
		}
		return Handling.chaosDrive(input.car, homeGoal, false);
	}
	
	private boolean doHop(InfoPacket input, double impactRadians){
		return input.car.hasWheelContact && Math.abs(impactRadians) > Math.toRadians(20) && input.car.velocity.magnitude() < 800 && input.info.impact.getTime() > 1.5;
	}

	private CarData getAttacker(InfoPacket input){
		double shortestDistance = 4000;
		CarData attacker = null;
		for(CarData c : input.cars){
			if(c == null || c.team == input.car.team) continue;
			Vector2 target = Behaviour.getTarget(c, input.ball); //This represents the part of the goal that they're shooting at
			double distance = c.position.distanceFlat(input.ball.position);
			
			double attackingAngle = Handling.aim(c, input.ball.position.flatten());
			double shootingAngle = Math.abs(attackingAngle - Handling.aim(c, target));
			attackingAngle = Math.abs(attackingAngle);
			
			if(attackingAngle < maxAttackingAngle && shootingAngle < maxShootingAngle && distance < shortestDistance){
				shortestDistance = distance;
				attacker = c;
			}
		}
		return attacker;
	}

}
