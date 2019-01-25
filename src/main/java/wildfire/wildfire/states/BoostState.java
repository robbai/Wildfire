package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

public class BoostState extends State {

	private BoostPad boost = null;

	public BoostState(Wildfire wildfire){
		super("Boost", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		if(input.car.boost > 30 || Utils.isKickoff(input) || input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < 2150 || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(input.car.team)) < 3800 || Math.abs(wildfire.impactPoint.getPosition().x) < 1300 || input.car.magnitudeInDirection(wildfire.impactPoint.getPosition().minus(input.car.position).flatten()) > 1800 || Utils.isInCone(input.car, wildfire.impactPoint.getPosition())){
			return false;
		}
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(DataPacket input){
		return boost == null || !boost.isActive() || Utils.isKickoff(input) || input.car.boost > 30 || input.ball.velocity.magnitude() > 5000 || wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1800 || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(input.car.team)) < 3500;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//This shouldn't happen, but let's check anyway!
		if(boost == null) return Utils.driveBall(input);
		
		//Drive down the wall
		if(Utils.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
		wildfire.sendQuickChat(QuickChatSelection.Information_NeedBoost);
		
		double distance = boost.getLocation().distanceFlat(input.car.position);
		double steer = Utils.aim(input.car, boost.getLocation().flatten());
		
		double forwardVelocity = input.car.forwardMagnitude();
		if(!hasAction() && input.car.hasWheelContact && distance > 2200 && !input.car.isSupersonic){			
			if(Math.abs(steer) < 0.2 && forwardVelocity > 800){
				currentAction = new DodgeAction(this, steer, input);
			}else if(Math.abs(steer) > 0.7 * Math.PI && forwardVelocity < -800){
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Render
		wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.flatten().toFramework(), boost.getLocation().flatten().toFramework());
		wildfire.renderer.drawCircle(Color.BLUE, boost.getLocation().flatten(), 110);
		
		if(forwardVelocity >= 0){
			return new ControlsOutput().withSteer((float)steer * -3).withThrottle(1F).withBoost(Math.abs(steer) < 0.1);
		}else{
			return new ControlsOutput().withSteer((float)Utils.invertAim(steer) * 3).withThrottle(-1F).withBoost(false);
		}
	}

	private BoostPad getBoost(DataPacket input){
		final double maxDistance = input.car.position.distanceFlat(input.ball.position) + 1000;
		double bestDistance = 0;
		BoostPad bestBoost = null;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			double distance = boost.getLocation().distanceFlat(input.car.position);
			if(distance > maxDistance || !boost.isActive()) continue;
			if(Math.signum(boost.getLocation().y - input.ball.position.y) == Utils.teamSign(input.car)) continue;
			if(input.car.magnitudeInDirection(boost.getLocation().minus(input.car.position).flatten()) > -200) continue;
			if(bestBoost == null || distance < bestDistance){
				bestBoost = boost;
				bestDistance = distance;
			}
		}
		return bestBoost;
	}

}
