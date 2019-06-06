package wildfire.wildfire.obj;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;

public abstract class Action {
	
	private String name;	
	
	public State state;
	public Wildfire wildfire;
	public float timeStarted;
	public boolean failed;
	
	public Action(String name, State state, float time){
		this.timeStarted = time;
		this.failed = false;
		this.name = name;
		this.state = state;
		this.wildfire = state.wildfire;
		state.currentAction = this;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract ControlsOutput getOutput(DataPacket input);
	
	public abstract boolean expire(DataPacket input);
	
	public float timeDifference(float elapsedSeconds){
		return elapsedSeconds - timeStarted;
	}

}
