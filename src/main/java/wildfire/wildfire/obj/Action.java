package wildfire.wildfire.obj;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;

public abstract class Action {
	
	public float timeStarted;
	private String name;
	public boolean failed;
	public State state;
	
	public Action(String name, State state, float time){
		this.timeStarted = time;
		this.failed = false;
		this.name = name;
		this.state = state;
		state.currentAction = this;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract ControlsOutput getOutput(DataPacket input);
	
	public abstract boolean expire(DataPacket input);
	
	public float timeDifference(float time){
		return time - timeStarted;
	}

}
