package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class BoostState extends State {

	private double maxBoost = 36, maxBoostMega = 72;
	private BoostPad boost = null;
	private boolean steal = false;

	public BoostState(Wildfire wildfire){
		super("Boost", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		Vector2 impactFlat = wildfire.impactPoint.getPosition().flatten(); 
		steal  = (Utils.teamSign(input.car) * impactFlat.y > 4000 && Utils.enemyGoal(input.car.team).distance(impactFlat) > 1800 && !Utils.isInCone(input.car, impactFlat.withZ(0), 700));
		
		//World's longest line
		if(input.car.boost > maxBoost || Utils.isKickoff(input) || (input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < 2400 && !steal) || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(input.car.team)) < 4300 || Math.abs(wildfire.impactPoint.getPosition().x) < 1500 || input.car.magnitudeInDirection(wildfire.impactPoint.getPosition().minus(input.car.position).flatten()) > 2000 || ((Utils.isInCone(input.car, wildfire.impactPoint.getPosition(), 200) && !steal) && wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 6000) || Math.abs(input.car.position.y) > Utils.PITCHLENGTH){
			return false;
		}
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(DataPacket input){
		if(Utils.isKickoff(input) || boost == null || !boost.isActive() || input.car.boost > maxBoostMega) return true;
		if(boost.getLocation().distanceFlat(input.car.position) < 1800) return false;
		return input.car.boost > maxBoost || input.ball.velocity.magnitude() > 5000 || wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1800 || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(input.car.team)) < 3600 || Math.abs(wildfire.impactPoint.getPosition().x) < 1200;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		if(Utils.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		if(this.steal) wildfire.renderer.drawString2d("Steal", Color.WHITE, new Point(0, 20), 2, 2);
		
		//Recovery
		if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		Vector2 boostLocation = boost.getLocation().flatten();
		double distance = input.car.position.distanceFlat(boostLocation);
		double steer = Utils.aim(input.car, boostLocation);
		
		if(distance > 2000 && input.car.velocity.magnitude() < 1000) wildfire.sendQuickChat(QuickChatSelection.Information_NeedBoost);
		
		double forwardVelocity = input.car.forwardMagnitude();
		if(!hasAction() && input.car.hasWheelContact && distance > 2200 && !input.car.isSupersonic){			
			if(Math.abs(steer) < 0.2 && forwardVelocity > 800){
				currentAction = new DodgeAction(this, steer, input);
			}else if(Math.abs(steer) > 0.95 * Math.PI && forwardVelocity < -850){
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Render
		wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.flatten().toFramework(), boostLocation.toFramework());
		wildfire.renderer.drawCircle(Color.BLUE, boostLocation, 110);
		
		//Stuck in goal
		if(Math.abs(input.car.position.y) > Utils.PITCHLENGTH){
			boostLocation = new Vector2(Utils.clamp(boostLocation.x, -600, 600), Utils.clamp(boostLocation.y, -Utils.PITCHLENGTH + 200, Utils.PITCHLENGTH - 200));
		}
		
		if(forwardVelocity >= 0){
			return new ControlsOutput().withSteer((float)steer * -3).withThrottle(1F).withBoost(Math.abs(steer) < 0.1 && (distance > 1200 || forwardVelocity < 800)).withSlide(Math.abs(steer) > 1.2 && distance < 900);
		}else{
			return new ControlsOutput().withSteer((float)Utils.invertAim(steer) * 3).withThrottle(-1F).withBoost(false);
		}
	}

	private BoostPad getBoost(DataPacket input){
//		double maxDistance = input.car.position.distanceFlat(input.ball.position) + 1000;
		double maxDistance = (input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) + 800);
		boolean discardSpeed = (input.car.velocity.magnitude() < 800);
		
		double bestDistance = 0;
		BoostPad bestBoost = null;
		for(BoostPad boost : BoostManager.getFullBoosts()){
//			double distance = boost.getLocation().distanceFlat(input.car.position);
			double distance = boost.getLocation().distanceFlat(wildfire.impactPoint.getPosition());
			if(distance > maxDistance || !boost.isActive()) continue;
			if(boost.getLocation().y * Utils.teamSign(input.car) > wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car)) continue;
			if(steal && boost.getLocation().y * Utils.teamSign(input.car) < 4000) continue; //Steal their boost
			
			if(!discardSpeed && input.car.magnitudeInDirection(boost.getLocation().minus(input.car.position).flatten()) < -250) continue;
			
			if(bestBoost == null || distance < bestDistance){
				bestBoost = boost;
				bestDistance = distance;
			}
		}
		return bestBoost;
	}

}