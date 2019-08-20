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
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class BoostTestState extends State {
	
	/*
	 * This state is solely for the purpose of testing
	 */

	private final double maxBoost = 60;
	
	private BoostPad boost = null;

	public BoostTestState(Wildfire wildfire){
		super("Boost", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(input.car.boost > maxBoost) return false;
		boost = getBoost(input);
		return boost != null;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		// Drive down the wall.
		if(Behaviour.isOnWall(car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
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
		if(car.hasWheelContact && distance > 1900 && !car.isSupersonic){	
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
		
		return Handling.arriveDestination(car, boostLocation, true);
	}

	private BoostPad getBoost(InfoPacket input){
		Impact impact = input.info.impact;
		double impactTime = impact.getTime();
		if(Math.abs(impact.getPosition().y) > (Constants.PITCHLENGTH - 1100)) impactTime *= 1.15;
		if(Math.abs(impact.getPosition().x) > (Constants.PITCHWIDTH - 950)) impactTime *= 1.2;
		if(impact.getPosition().z > 260) impactTime *= 1.4;
		
		boolean carCorrectSide = Behaviour.correctSideOfTarget(input.car, impact.getBallPosition());
		
		BoostPad bestBoost = null;
		double bestBoostTime = 0;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			if(!boost.isActive()) continue;
			
			boolean boostCorrectSide = ((boost.getLocation().y - impact.getBallPosition().y) * Utils.teamSign(input.car) > 0);
			
			double distance = boost.getLocation().distance(input.car.position);
			double maxVel = DrivePhysics.maxVelocityDist(input.car.velocityDir(boost.getLocation().minus(input.car.position)), input.car.boost, distance);
			double travelTime = DrivePhysics.minTravelTime(input.car, distance);
			Impact newImpact = Behaviour.getEarliestImpactPoint(boost.getLocation(), 100, boost.getLocation().minus(input.car.position).scaledToMagnitude(maxVel), input.car.elapsedSeconds + travelTime, wildfire.ballPrediction);
			if(newImpact == null) continue;
			travelTime += newImpact.getTime();
			
			if(!boostCorrectSide) travelTime *= (carCorrectSide ? 1.3 : 1.05);
			
			if(travelTime < impactTime && (bestBoost == null || travelTime < bestBoostTime - 0.05)){
				bestBoost = boost;
				bestBoostTime = travelTime;
			}
		}
		
		return bestBoost;
	}

}