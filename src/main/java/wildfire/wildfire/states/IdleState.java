package wildfire.wildfire.states;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Handling;

public class IdleState extends State {
	
	/*
	 * Stays still when the game is not in play
	 */

	public IdleState(Wildfire wildfire){
		super("Idle", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		return !input.gameInfo.isRoundActive() && !Behaviour.isKickoff(input);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		// ATBA
		if(!input.gameInfo.isRoundActive()){
			CarData opponent = Behaviour.closestOpponent(input, input.car.position);
			if(opponent != null) return Handling.atba(input, opponent.position);
		}
		
		return new ControlsOutput().withNone().withThrottle(input.car.forwardMagnitude() / -1000);
	}

}
