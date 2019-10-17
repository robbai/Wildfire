package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.ball.BallData;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.Impact;
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

	private final double goalScale = 0.9;
	
	public ReturnState(String name, Wildfire wildfire){
		super(name, wildfire);
	}

	public ReturnState(Wildfire wildfire){
		super("Return", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		if(Behaviour.isKickoff(input) || !input.car.onFlatGround || input.gameInfo.isMatchEnded()) return false;

		CarData car = input.car;
		Vector2 carPosition = car.position.flatten();
		Impact impact = input.info.impact;
		boolean opponentBehind = Behaviour.isOpponentBehindBall(input);
		boolean centered = (Math.abs(impact.getPosition().x) < Constants.PITCH_WIDTH - 1200);
		double impactY = (car.sign * car.position.y);
		
		// Just hit it instead.
		if((impact.getPosition().distanceFlat(car.position) < Math.max(1200, car.velocity.magnitude() * 0.75) || impact.getTime() < 0.8) &&
				!Behaviour.isTowardsOwnGoal(car, impact.getPosition())){
			if(impactY < -2000) return false;
//			if(impactY > 4000 && centered) return true;
		}
		
		// Don't own goal.
		if(impactY < carPosition.y * car.sign){
			Vector2 inverseTarget = Behaviour.getTarget(car, impact.getBallPosition().flatten());
			inverseTarget = inverseTarget.withY(-inverseTarget.y);
			if(impact.getBallPosition().minus(car.position).flatten().angle(inverseTarget.minus(carPosition)) < Math.toRadians(10)){
				return false;
			}
		}
		
		if(impact.getPosition().distanceFlat(Constants.homeGoal(car)) < 2400){
			return false;
		}

		if(centered){
			if(impact.getTime() < input.info.enemyImpactTime - (opponentBehind ? 0.15 : 0.45)){
				return !Behaviour.correctSideOfTarget(car, impact.getBallPosition());
			}
		}
		else{
			if(car.sign * car.position.y > Math.max(4000, car.sign * impact.getPosition().y)){
				return true;
			}
		}
		
		// Check if we have a shot opportunity.
		if(impact.getPosition().distanceFlat(car.position) < 3000){
			if(impact.getTime() < input.info.enemyImpactTime + 0.1){
				if(Behaviour.isInCone(car, impact.getPosition(), 200)) return false;
			}
		}
		
		if(!centered && impactY > (input.info.enemyImpactTime > 3 ? -1000 : 1200)){
			Vector2 teamSignVec = new Vector2(0, -car.sign);
			double yAngle = teamSignVec.angle(car.position.minus(impact.getPosition()).flatten());
			if(yAngle > Math.toRadians(70)){
				return true;
			}
//			else{
//				return false;
//			}
		}
		
		Vector2 homeGoal = Constants.homeGoal(car.team);
		if(car.position.distanceFlat(homeGoal) < 2800){
			boolean onTarget = Behaviour.isOnTarget(wildfire.ballPrediction, car.team);
			if(!onTarget && !opponentBehind) return false;
			if(Behaviour.isTeammateCloser(input)){
				return impact.getTime() < (6D - DrivePhysics.maxVelocity(car.velocity.magnitude(), car.boost) / 1400D)
						&& impact.getTime() > 1.5;
			}
		}

		if(centered){
			if(!opponentBehind || Behaviour.closestOpponentDistance(input, input.ball.position) > 3400) return false;
		}
//		return car.sign * car.position.y > -3000/* || impact.getTime() > 1.4*/;
		
		if(!opponentBehind || Behaviour.closestOpponentDistance(input, input.ball.position) > 3400) return false;
		return car.sign * car.position.y > 3000/* && impact.getTime() > 1.3*/;
	}
	
	@Override
	public boolean expire(InfoPacket input){
		CarData car = input.car;
		Impact impact = input.info.impact;
		Vector2 homeGoal = Constants.homeGoal(car);
		double carGoalDistance = car.position.distanceFlat(homeGoal);
		double impactGoalDistance = impact.getPosition().distanceFlat(homeGoal);
		
		if(impact.getTime() < input.info.enemyImpactTime - 0.05){
//			if(Behaviour.isInCone(car, impact.getPosition(), 200)) return true;
			if(Math.abs(impact.getBallPosition().x) < 1300) return true;
		}
		if(carGoalDistance < impactGoalDistance * 0.6 || impactGoalDistance < 2600){
			return true;
		}
		
		return carGoalDistance < 2900;
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
			target = target.withY(input.car.sign * -Constants.PITCH_LENGTH * goalScale);
			target = target.withX(Utils.clamp(target.x, Constants.GOAL_WIDTH - 250, -Constants.GOAL_WIDTH + 250));

			wildfire.renderer.drawLine3d(Color.RED, attacker.position.flatten(), target);
			wildfire.renderer.drawCrosshair(input.car, input.info.impact.getPosition(), Color.RED, 125);

			// Rush them.
//			double impactDistance = input.info.impact.getPosition().distanceFlat(input.car.position);
//			if(impactDistance < 1800 || (impactDistance < 2500 && input.ball.position.minus(attacker.position).flatten().angle(input.car.position.minus(attacker.position).flatten()) < 0.28)){
//				wildfire.sendQuickChat(QuickChatSelection.Information_Incoming);
//				wildfire.renderer.drawString2d("Rush", Color.WHITE, new Point(0, 40), 2, 2);
//				wildfire.renderer.drawCrosshair(input.car, input.info.impact.getPosition(), Color.MAGENTA, 125);
//				return Handling.chaosDrive(input.car, input.info.impact.getPosition().flatten(), true);
//			}else{
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
					target = target.withX(Utils.clamp(target.x, -600, 600));
					
					wildfire.renderer.drawLine3d(Color.RED, input.car.position.flatten(), target);

					Action action = proposeAction(input, target);
					if(action != null) return this.startAction(action, input);

					// We better get there!
					ControlsOutput controls = Handling.chaosDrive(input.car, target, false);
					return controls.withBoost(controls.holdBoost() && shouldBoostToGoal(input, target)); 
				}
//			}
		}

		// Get back to goal.
		Vector2 homeGoal = Constants.homeGoal(input.car.team).scaled(goalScale);
		if(homeGoal.distance(input.car.position.flatten()) < 200 && input.info.impact.getTime() > 1.3){
			if(doHop(input, impactRadians)){
				currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
				if(!currentAction.failed) return currentAction.getOutput(input);
			}
			return Handling.stayStill(input.car);
		}
		Action action = proposeAction(input, homeGoal);
		if(action != null) return this.startAction(action, input);
		ControlsOutput controls = Handling.chaosDrive(input.car, homeGoal, false);
		return controls.withBoost(controls.holdBoost() && shouldBoostToGoal(input, homeGoal));
	}

	private Action proposeAction(InfoPacket input, Vector2 target){
		CarData car = input.car;
				
		// Front flip or half-flip.
		Action action = null;
		if(Constants.homeGoal(car.team).distance(car.position.flatten()) > Behaviour.dodgeDistance(car) && !car.isSupersonic){
			double radiansGoal = Handling.aim(car, target);
			if(Math.abs(radiansGoal) < 0.3 && car.forwardVelocity > (car.boost < 1 ? 1200 : 1500)){
				action = new DodgeAction(this, 0, input);
			}else if(Math.abs(radiansGoal) > Math.toRadians(155) && car.forwardVelocity < -500){
				action = new HalfFlipAction(this, input);
			}
		}
		if(action != null && !action.failed) return action;
		
		return null;
	}

	private boolean doHop(InfoPacket input, double impactRadians){
		return input.car.hasWheelContact && Math.abs(impactRadians) > Math.toRadians(20) && input.car.velocity.magnitude() < 800 && input.info.impact.getTime() > 1.5;
	}

	private CarData getAttacker(InfoPacket input){
		double shortestDistance = 4000;
		CarData attacker = null;
		for(int i = 0; i < input.cars.length; i++){
			CarData car = input.cars[i];
			Impact impact = input.info.impacts[i];
			if(car == null || car.team == input.car.team || impact == null) continue;
			
			Vector2 target = Behaviour.getTarget(car, impact.getBallPosition().flatten()); //This represents the part of the goal that they're shooting at
			double distance = car.position.distanceFlat(impact.getPosition());

			double attackingAngle = Handling.aim(car, impact.getBallPosition().flatten());
			double shootingAngle = Math.abs(attackingAngle - Handling.aim(car, target));
			attackingAngle = Math.abs(attackingAngle);

			if(attackingAngle < maxAttackingAngle && shootingAngle < maxShootingAngle && distance < shortestDistance){
				shortestDistance = distance;
				attacker = car;
			}
		}
		return attacker;
	}

	private boolean shouldBoostToGoal(InfoPacket input, Vector2 goal){
		CarData car = input.car;
		BallData ball = input.ball;
		
		if(car.forwardVelocity < 1600) return true;
		
		if(input.info.enemyImpactTime < input.info.impact.getTime() + 0.1 && !Behaviour.correctSideOfTarget(car, input.info.impact.getPosition())){
			return true;
		}
		
		Vector2 carPositionFlat = car.position.flatten();
		double driveTime = DrivePhysics.minTravelTime(car.velocityDir(goal.minus(carPositionFlat)), 0, goal.distance(carPositionFlat));
		double ballTime = (goal.y - ball.position.y) / ball.velocity.y;
		return ballTime > 0 && driveTime > ballTime; 
	}

}
