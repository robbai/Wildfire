package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.curve.BezierCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class BoostState extends State {

	private final double maxBoost = 40, maxBoostMega = 72;
	
	protected BoostPad boost = null;
	private boolean steal = false, possession = false;
	
	public BoostState(String name, Wildfire wildfire){
		super(name, wildfire);
	}

	public BoostState(Wildfire wildfire){
		this("Boost (Old)", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		Vector2 impactFlat = input.info.impact.getPosition().flatten(); 
		steal = (Utils.teamSign(input.car) * impactFlat.y > 4000 && Constants.enemyGoal(input.car.team).distance(impactFlat) > 2000 && !Behaviour.isInCone(input.car, impactFlat.withZ(0), 700));
		possession = (Behaviour.closestOpponentDistance(input, input.ball.position) > Math.max(2600, input.car.position.distanceFlat(impactFlat)));
		
		// World's longest line.
		boolean teammateAtBall = Behaviour.isTeammateCloser(input);
		if(input.car.boost > maxBoost || Behaviour.isKickoff(input) || (input.info.impactDistanceFlat < 2400 && !(steal || possession)) || input.info.impact.getPosition().distanceFlat(Constants.homeGoal(input.car.team)) < (teammateAtBall ? 2200 : 4500) || (Math.abs(input.info.impact.getPosition().x) < 1500 && !possession) || ((Behaviour.isInCone(input.car, input.info.impact.getPosition(), 200) && !(steal || possession)))){
			return false;
		}
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(InfoPacket input){
		if(Behaviour.isKickoff(input) || boost == null || !boost.isActive() || input.car.boost > maxBoostMega) return true;
		if(boost.getLocation().distanceFlat(input.car.position) < 2000) return false;
		return input.car.boost > maxBoost || input.ball.velocity.magnitude() > 5000 || input.info.impact.getPosition().distanceFlat(input.car.position) < 1600 || input.info.impact.getPosition().distanceFlat(Constants.homeGoal(input.car.team)) < (Math.abs(input.info.impact.getPosition().x) < 1200 ? 3000 : 2100);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		// Drive down the wall.
		if(Behaviour.isOnWall(car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		if(this.steal) wildfire.renderer.drawString2d("Steal", Color.WHITE, new Point(0, 20), 2, 2);
		
		// Recovery.
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 boostLocation = boost.getLocation().flatten();
		double distance = car.position.distanceFlat(boostLocation);
		double radians = Handling.aim(car, boostLocation);
		
		if(distance > 2000 && car.velocity.magnitude() < 1000) wildfire.sendQuickChat(QuickChatSelection.Information_NeedBoost);
		
		// Actions.
		double forwardVelocity = car.forwardVelocity;
		if(car.hasWheelContact && distance > 2000 && !car.isSupersonic){	
			if(Behaviour.isOnWall(car)){
				currentAction = new HopAction(this, input, boostLocation);
			}else if(Math.abs(radians) < 0.12 && forwardVelocity > 1100){
				currentAction = new DodgeAction(this, radians, input);
			}else if(Math.abs(radians) > 0.95 * Math.PI && forwardVelocity < -850){
				currentAction = new HalfFlipAction(this, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		// Render.
		double circleRadius = 100;
		Vector2 carPosition = car.position.flatten();
		Vector2 cross = boostLocation.minus(carPosition).cross();
		BezierCurve bezier = new BezierCurve(carPosition, 
				carPosition.plus(boostLocation.minus(carPosition).scaled(0.25)).plus(cross.scaledToMagnitude(distance / 8)), 
				carPosition.plus(boostLocation).scaled(0.5),
				carPosition.plus(boostLocation.minus(carPosition).scaled(0.75)).plus(cross.scaledToMagnitude(distance / -16)),
				boostLocation.plus(car.position.flatten().minus(boostLocation).scaledToMagnitude(circleRadius)));
		bezier.render(wildfire.renderer, Color.BLUE);
		wildfire.renderer.drawCircle(Color.blue, boostLocation, circleRadius);
		
		// Stuck in goal.
		boolean stuckInGoal = Math.abs(car.position.y) > Constants.PITCHLENGTH;
		if(stuckInGoal){
			boostLocation = new Vector2(Utils.clamp(boostLocation.x, -700, 700), Utils.clamp(boostLocation.y, -Constants.PITCHLENGTH + 500, Constants.PITCHLENGTH - 500));
		}
		
//		boolean reverse = (forwardVelocity < -400 && !stuckInGoal && distance < 2600);
		return Handling.arriveDestination(car, boostLocation, true);
	}

	private BoostPad getBoost(InfoPacket input){
		boolean discardSpeed = (input.car.velocity.magnitude() < 800);
		boolean teammateAtBall = Behaviour.isTeammateCloser(input);
		double maxDistance = (input.info.impactDistanceFlat + 800);		
		
		double bestDistance = 0;
		BoostPad bestBoost = null;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			double distance = boost.getLocation().distanceFlat(teammateAtBall ? input.car.position : input.info.impact.getPosition());
			if(distance > maxDistance || !boost.isActive()) continue;
			if(boost.getLocation().y * Utils.teamSign(input.car) > input.info.impact.getPosition().y * Utils.teamSign(input.car) - 1000) continue;
			if(steal && boost.getLocation().y * Utils.teamSign(input.car) < 4500) continue; // Steal their boost.
			
			if(!discardSpeed && input.car.velocityDir(boost.getLocation().minus(input.car.position).flatten()) < -250) continue;
			
			if(bestBoost == null || distance < bestDistance){
				bestBoost = boost;
				bestDistance = distance;
			}
		}
		return bestBoost;
	}

}