package wildfire.wildfire.states;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.obj.State;

public class StayStillState extends State {
	
	/*
	 * Another test state, just with a real name this time
	 */

	public StayStillState(Wildfire wildfire){
		super("Stay Still", wildfire);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		return new ControlsOutput().withBoost(false).withSlide(false).withJump(false).withSteer(0).withPitch(0).withRoll(0).withYaw(0).withThrottle((float)(-input.car.forwardMagnitude() / 1000));
	}

	@Override
	public boolean ready(DataPacket input){
		return false;
	}

}
