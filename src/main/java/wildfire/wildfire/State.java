package wildfire.wildfire;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;

public abstract class State {
	
	private String name;
	public Wildfire wildfire;
	public Action currentAction;
	
	public State(String name, Wildfire wildfire){
		this.name = name;
		this.wildfire = wildfire;
		wildfire.states.add(this);
	}
	
	public abstract ControlsOutput getOutput(DataPacket input);
	
	public abstract boolean ready(DataPacket input);
	
	public boolean expire(DataPacket input){
		return !ready(input) && !hasAction();
	}
	
	public boolean hasAction(){
		return currentAction != null;
	}

	public String getName(){
		return name;
	}

}
