package wildfire.wildfire.obj;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;

public abstract class Mechanic {
	
	private final String name;
	
	public Wildfire wildfire;
	public State state;
	
	public float timeStarted;
	
	public Mechanic(String name, State state, float time){
		this.name = name;
		this.timeStarted = time;
		
		this.wildfire = state.wildfire;
		this.state = state;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract ControlsOutput getOutput(DataPacket input);
	
	public abstract boolean expire(DataPacket input);
	
	public float timeDifference(float elapsedSeconds){
		return elapsedSeconds - timeStarted;
	}
	
	protected ControlsOutput startAction(Action action, DataPacket input){
		this.state.currentAction = action;
		return action.getOutput(input);
	}

	/*
	 * This is okay to do since the state manager
	 *  will just change to the action's output
	 */
	protected ControlsOutput startAction(Action action){
		this.state.currentAction = action;
		return null;
	}

}
