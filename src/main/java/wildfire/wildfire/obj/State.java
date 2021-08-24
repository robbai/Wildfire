package wildfire.wildfire.obj;

import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;

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

	public abstract ControlsOutput getOutput(InfoPacket input);

	public abstract boolean ready(InfoPacket input);

	public boolean expire(InfoPacket input){
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

	protected ControlsOutput startMechanic(Mechanic mechanic, InfoPacket input){
		this.currentMechanic = mechanic;
		return this.currentMechanic.getOutput(input);
	}

	protected ControlsOutput startAction(Action action, InfoPacket input){
		this.currentAction = action;
		return action.getOutput(input);
	}

}
