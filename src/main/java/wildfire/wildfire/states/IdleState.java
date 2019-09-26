package wildfire.wildfire.states;

import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;

public class IdleState extends State {
	
	/*
	 * Stays still when the game is not in play
	 */

	public IdleState(Wildfire wildfire){
		super("Idle", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(Behaviour.isKickoff(input)) return false;
		if(!input.gameInfo.isRoundActive()) return true;
		
		if(input.cars.length == 1) return false;
		
		boolean onTarget = Behaviour.isOnTarget(wildfire.ballPrediction, 1 - input.car.team);
		if(!onTarget) return false;
		
		boolean noIntersect = Behaviour.nobodyElseIntersect(input.car.index, input.cars, wildfire.ballPrediction);
		if(!noIntersect) return false;
		
		boolean block = Behaviour.blocksPrediction(input.car, wildfire.ballPrediction) && !Behaviour.correctSideOfTarget(input.car, input.ball.position);
		return !block;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		if(Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		// ATBA
		if(!input.gameInfo.isRoundActive()){
			CarData opponent = Behaviour.closestOpponent(input, input.car.position);
			if(opponent != null) return Handling.atba(input, opponent.position);
		}
		
		return Handling.stayStill(input.car);
	}

}
