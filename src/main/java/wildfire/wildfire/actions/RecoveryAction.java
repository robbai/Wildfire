package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class RecoveryAction extends Action {
	
	private PID rollPID, pitchPID;

	public RecoveryAction(State state){
		super("Recovery", state);
		
		this.rollPID = new PID(1, 0, 0.2);
		this.pitchPID = new PID(1.75, 0, 0.4);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double roll = rollPID.getOutput(-input.car.orientation.rightVector.z, 0);
		double pitch = pitchPID.getOutput(input.car.orientation.noseVector.z, 0);		
		return new ControlsOutput().withRoll((float)roll).withPitch((float)pitch).withThrottle(1);
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}

}
