package wildfire.wildfire.states;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.OrientAction;
import wildfire.wildfire.obj.State;

public class TestState2 extends State {
	
	/*
	 * Action testing state
	 */

	public TestState2(Wildfire wildfire){
		super("Test2", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return true;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction()){
			currentAction = new OrientAction(this, input);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input); 
			currentAction = null;
		}
		
		return new ControlsOutput().withNone();
	}

}