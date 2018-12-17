package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Action;
import wildfire.wildfire.State;
import wildfire.wildfire.Utils;

public class DodgeAction extends Action {
	
	private double angle;

	public DodgeAction(State state, double angle, DataPacket input){
		super("Dodge", state);
		this.angle = angle;
		
		//No spamming!
		if(state.wildfire.lastDodge + 2000 > timeStarted){
			failed = true; 
		}else{
			state.wildfire.lastDodge = timeStarted;
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(1).withBoost(false);
		long timeDifference = timeDifference();
		
		if(timeDifference <= 160){
			controller.withJump(timeDifference <= 90);
			controller.withPitch(-1);
		}else if(timeDifference <= 750){
			controller.withJump(true);
	        controller.withYaw((float)-Math.sin(angle));
	        controller.withPitch((float)-Math.cos(angle)); 
		}else if(input.car.position.z > 250 || timeDifference >= 2600){
			Utils.transferAction(this, new RecoveryAction(this.state));
		}
	        
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (System.currentTimeMillis() >= 400 + timeStarted && input.car.hasWheelContact);
	}

}
