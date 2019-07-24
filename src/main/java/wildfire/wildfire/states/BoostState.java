package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.curve.BezierCurve;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class BoostState extends State {

	private final double maxBoost = 40, maxBoostMega = 72;
	
	private BoostPad boost = null;
	private boolean steal = false, possession = false;

	public BoostState(Wildfire wildfire){
		super("Boost", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		Vector2 impactFlat = wildfire.impactPoint.getPosition().flatten(); 
		steal = (Utils.teamSign(input.car) * impactFlat.y > 4000 && Constants.enemyGoal(input.car.team).distance(impactFlat) > 1800 && !Behaviour.isInCone(input.car, impactFlat.withZ(0), 700));
		possession = (Behaviour.closestOpponentDistance(input, input.ball.position) > Math.max(2400, input.car.position.distanceFlat(impactFlat)));
		
		//World's longest line
		boolean teammateAtBall = Behaviour.isTeammateCloser(input);
		if(input.car.boost > maxBoost || Behaviour.isKickoff(input) || (input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < 2400 && !(steal || possession)) || wildfire.impactPoint.getPosition().distanceFlat(Constants.homeGoal(input.car.team)) < (teammateAtBall ? 2200 : 4500) || (Math.abs(wildfire.impactPoint.getPosition().x) < 1500 && !possession) || ((Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition(), 200) && !(steal || possession)))){
			return false;
		}
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(DataPacket input){
		if(Behaviour.isKickoff(input) || boost == null || !boost.isActive() || input.car.boost > maxBoostMega) return true;
		if(boost.getLocation().distanceFlat(input.car.position) < 1800) return false;
		return input.car.boost > maxBoost || input.ball.velocity.magnitude() > 5000 || wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1800 || wildfire.impactPoint.getPosition().distanceFlat(Constants.homeGoal(input.car.team)) < (Math.abs(wildfire.impactPoint.getPosition().x) < 1200 ? 3300 : 2400);
//		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		if(Behaviour.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		if(this.steal) wildfire.renderer.drawString2d("Steal", Color.WHITE, new Point(0, 20), 2, 2);
		
		//Recovery
		if(!hasAction() && Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 boostLocation = boost.getLocation().flatten();
		double distance = input.car.position.distanceFlat(boostLocation);
		double steer = Handling.aim(input.car, boostLocation);
		
		if(distance > 2000 && input.car.velocity.magnitude() < 1000) wildfire.sendQuickChat(QuickChatSelection.Information_NeedBoost);
		
		double forwardVelocity = input.car.forwardMagnitude();
		if(!hasAction() && input.car.hasWheelContact && distance > 1500 && !input.car.isSupersonic){	
			if(Behaviour.isOnWall(input.car)){
				currentAction = new HopAction(this, input, boostLocation);
			}else if(Math.abs(steer) < 0.12 && forwardVelocity > 1100){
				currentAction = new DodgeAction(this, steer, input);
			}else if(Math.abs(steer) > 0.95 * Math.PI && forwardVelocity < -850){
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Render
		double circleRadius = 100;
		Vector2 carPosition = input.car.position.flatten();
		BezierCurve bezier = new BezierCurve(carPosition, 
				carPosition.plus(boostLocation.minus(carPosition).scaled(0.25)).plus(input.car.velocity.flatten()), 
				carPosition.plus(boostLocation).scaled(0.5),
				carPosition.plus(boostLocation.minus(carPosition).scaled(0.75)).plus(input.car.velocity.flatten().scaled(-0.5)),
				boostLocation.plus(input.car.position.flatten().minus(boostLocation).scaledToMagnitude(circleRadius)));
		bezier.render(wildfire.renderer, Color.BLUE);
		wildfire.renderer.drawCircle(Color.blue, boostLocation, circleRadius);
		
		//Stuck in goal
		boolean stuckInGoal = Math.abs(input.car.position.y) > Constants.PITCHLENGTH;
		if(stuckInGoal){
			boostLocation = new Vector2(Utils.clamp(boostLocation.x, -700, 700), Utils.clamp(boostLocation.y, -Constants.PITCHLENGTH + 500, Constants.PITCHLENGTH - 500));
		}
		
		boolean reverse = (forwardVelocity < -200 && !stuckInGoal);
		
		double throttle = (Handling.insideTurningRadius(input.car, boostLocation) ? 0 : (reverse ? -1 : 1));
		
		if(reverse){
			return new ControlsOutput().withSteer(Utils.invertAim(steer) * 3).withThrottle(throttle).withBoost(false);
		}else{
			return new ControlsOutput().withSteer(steer * -3).withThrottle(throttle)
					.withBoost(Math.abs(steer) < 0.1 && (distance > 1200 || forwardVelocity < 800))
					.withSlide(Math.abs(steer) > 1.2 && distance < 1200 && !input.car.isDrifting());
		}
	}

	private BoostPad getBoost(DataPacket input){
		boolean discardSpeed = (input.car.velocity.magnitude() < 800);
		boolean teammateAtBall = Behaviour.isTeammateCloser(input);
		double maxDistance = (input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) + 800);
		
		
		double bestDistance = 0;
		BoostPad bestBoost = null;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			double distance = boost.getLocation().distanceFlat(teammateAtBall ? input.car.position : wildfire.impactPoint.getPosition());
			if(distance > maxDistance || !boost.isActive()) continue;
			if(boost.getLocation().y * Utils.teamSign(input.car) > wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) - 1000) continue;
			if(steal && boost.getLocation().y * Utils.teamSign(input.car) < 4500) continue; //Steal their boost
			
			if(!discardSpeed && input.car.magnitudeInDirection(boost.getLocation().minus(input.car.position).flatten()) < -250) continue;
			
			if(bestBoost == null || distance < bestDistance){
				bestBoost = boost;
				bestDistance = distance;
			}
		}
		return bestBoost;
	}

}