package wildfire.wildfire.states;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.obj.State;

public class TestState extends State {
	
	/*
	 * This state is solely for the purpose of testing actions
	 */

	public TestState(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return true;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		boolean positioned = input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < wildfire.impactPoint.getPosition().z * 4;
		if(!hasAction() && input.car.hasWheelContact && positioned){
			currentAction = new AerialAction(this, input, input.car.position.z < 500 && wildfire.impactPoint.getPosition().z > 500);
			if(!currentAction.failed) return currentAction.getOutput(input); //Start overriding
		}
		
		double steer = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double throttle = Math.cos(steer);
		return new ControlsOutput().withSteer((float)(throttle < 0 ? Utils.invertAim(steer) : -steer) * 2F).withThrottle((float)throttle).withBoost(false);
	}

}
