package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.car.CarData;
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

public class ReturnStateOld extends ReturnState {

	public ReturnStateOld(Wildfire wildfire){
		super("Return (Old)", wildfire);
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
		return input.car.sign * input.car.position.y < -2750 && input.info.impact.getTime() > 1.4;
	}

}