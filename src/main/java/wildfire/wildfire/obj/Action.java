package wildfire.wildfire.obj;

import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;

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
		this.wildfire = (state == null ? null : state.wildfire);
		if(state != null) state.currentAction = this;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract ControlsOutput getOutput(InfoPacket input);
	
	public abstract boolean expire(InfoPacket input);
	
	public float timeDifference(float elapsedSeconds){
		return elapsedSeconds - timeStarted;
	}

}
