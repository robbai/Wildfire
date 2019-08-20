package wildfire.wildfire.obj;

import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;

public abstract class Mechanic {
	
	private final String name;
	
	public Wildfire wildfire;
	public State state;
	
	public double timeStarted;
	
	public Mechanic(String name, State state, double time){
		this.name = name;
		this.timeStarted = time;
		
		this.wildfire = state.wildfire;
		this.state = state;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract ControlsOutput getOutput(InfoPacket input);
	
	public abstract boolean expire(InfoPacket input);
	
	public double timeDifference(double elapsedSeconds){
		return elapsedSeconds - timeStarted;
	}
	
	protected ControlsOutput startAction(Action action, InfoPacket input){
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
