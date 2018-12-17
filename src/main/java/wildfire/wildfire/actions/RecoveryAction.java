package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Action;
import wildfire.wildfire.PID;
import wildfire.wildfire.State;
import wildfire.wildfire.Utils;

public class RecoveryAction extends Action {
	
	private PID rollPID, pitchPID;

	public RecoveryAction(State state){
		super("Recovery", state);
		
		this.rollPID = new PID(1, 0.2, 0.2);
		this.pitchPID = new PID(0.75, 0.1, 0.1);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double roll = rollPID.getOutput(-input.car.orientation.rightVector.z, 0);
		double pitch = pitchPID.getOutput(input.car.orientation.noseVector.z, 0);

		state.wildfire.renderer.drawString2d("Roll Output: " + Utils.round(roll), Color.white, new Point(0, 40), 2, 2);
		state.wildfire.renderer.drawString2d("Pitch Output: " + Utils.round(pitch), Color.white, new Point(0, 60), 2, 2);
		
		return new ControlsOutput().withRoll((float)roll).withPitch((float)pitch).withThrottle(1);
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}

}
