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
		if(input.car.boost > 40 || Utils.isKickoff(input) || input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < 2000 || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(wildfire.team)) < 4700 || Math.abs(wildfire.impactPoint.getPosition().x) < 1550){
			return false;
		}
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(DataPacket input){
		return boost == null || !boost.isActive() || Utils.isKickoff(input) || input.car.boost > 40 || input.ball.velocity.magnitude() > 5000 || wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 1400 || wildfire.impactPoint.getPosition().distanceFlat(Utils.homeGoal(wildfire.team)) < 4700;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//This shouldn't happen, but let's check anyway!
		if(boost == null) return Utils.driveBall(input);
		
		//Drive down the wall
		if(Utils.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}else{
			wildfire.sendQuickChat(QuickChatSelection.Information_NeedBoost);
		}
		
		double distance = boost.getLocation().distanceFlat(input.car.position);
		double steer = Utils.aim(input.car, boost.getLocation().flatten());
		double throttle = Math.signum(Math.cos(steer));
		
		if(!hasAction() && input.car.hasWheelContact && distance > 1500){
			if(input.car.magnitudeInDirection(boost.getLocation().minus(input.car.position).flatten()) > 700 && Math.abs(steer) < 0.3 * Math.PI){
				currentAction = new DodgeAction(this, steer, input);
			}else if(Math.abs(steer) > 0.7 * Math.PI){
				currentAction = new HalfFlipAction(this);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.flatten().toFramework(), boost.getLocation().flatten().toFramework());
		wildfire.renderer.drawCircle(Color.BLUE, boost.getLocation().flatten(), 110);
		return new ControlsOutput().withSteer((float)-(throttle > 0 ? steer : Utils.invertAim(steer)) * 2F).withThrottle((float)throttle).withBoost(Math.abs(steer) < 0.1);
	}

	private BoostPad getBoost(DataPacket input){
		final double maxDistance = input.ball.position.distanceFlat(input.ball.position) + 1500;
		double bestDistance = 0;
		BoostPad bestBoost = null;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			double distance = boost.getLocation().distanceFlat(input.car.position);
			if(distance > maxDistance || !boost.isActive() || Math.signum(boost.getLocation().y - input.ball.position.y) == Utils.teamSign(input.car)){
				continue;
			}
			if(bestBoost == null || distance < bestDistance){
				bestBoost = boost;
				bestDistance = distance;
			}
		}
		return bestBoost;
	}

}
