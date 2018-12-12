package rlbotexample.wildfire;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;

public abstract class Action {
	
	public long timeStarted;
	private String name;
	public boolean failed;
	public State state;
	
	public Action(String name, State state){
		this.timeStarted = System.currentTimeMillis();
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
	
	public long timeDifference(){
		return System.currentTimeMillis() - timeStarted;
	}

}
