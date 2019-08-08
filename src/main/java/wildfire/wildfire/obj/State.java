package wildfire.wildfire.obj;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;

public abstract class State {
	
	private final String name;
	
	public Wildfire wildfire;
	
	public Action currentAction;
	public Mechanic currentMechanic;
	
	public State(String name, Wildfire wildfire){
		this.name = name;
		this.wildfire = wildfire;
		wildfire.states.add(this);
	}
	
	public abstract ControlsOutput getOutput(DataPacket input);
	
	public abstract boolean ready(DataPacket input);
	
	public boolean expire(DataPacket input){
		return !ready(input);
	}
	
	public boolean hasAction(){
		return currentAction != null;
	}
	
	public boolean runningMechanic(){
		return currentMechanic != null;
	}

	public String getName(){
		return name;
	}
	
	protected ControlsOutput startMechanic(Mechanic mechanic, DataPacket input){
		this.currentMechanic = mechanic;
		return this.currentMechanic.getOutput(input);
	}

}
