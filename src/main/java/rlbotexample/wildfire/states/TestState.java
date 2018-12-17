package rlbotexample.wildfire.states;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.AerialAction;

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
		if(wildfire.impactPoint.z < 200){
			double steer = -Utils.aim(input.car, wildfire.impactPoint.flatten()) * 2;
			return new ControlsOutput().withSteer((float)steer).withThrottle(1F).withBoost(!Utils.isBallAirborne(input.ball) && input.car.hasWheelContact).withSlide(Math.abs(steer) > 2);
		}
		
		if(!hasAction() && input.car.hasWheelContact && input.car.velocity.magnitude() < 900){
			currentAction = new AerialAction(this, input, input.car.position.z < 1000 && wildfire.impactPoint.z > 800);
			if(!currentAction.failed) return currentAction.getOutput(input); //Start overriding
		}
		
		return new ControlsOutput().withThrottle(0).withBoost(false).withSteer(0).withSlide(false);
	}

}
