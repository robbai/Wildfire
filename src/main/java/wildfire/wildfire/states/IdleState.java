package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class IdleState extends State {
	
	/*
	 * Stays still when the game is not in play
	 */

	public IdleState(Wildfire wildfire){
		super("Idle", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		return !input.gameInfo.isRoundActive() && !Utils.isKickoff(input);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//ATBA
		CarData opponent = Utils.closestOpponent(input, input.car.position);
		if(opponent != null){
			wildfire.renderer.drawString2d("ATBA", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.atba(input, opponent.position);
		}
		
		return new ControlsOutput().withBoost(false).withSlide(false).withJump(false).withSteer(0).withPitch(0).withRoll(0).withYaw(0).withThrottle((float)(-input.car.forwardMagnitude() / 1000));
	}

	

}
