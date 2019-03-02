package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;

public class DodgeAction extends Action {
	
	private double angle;

	public DodgeAction(State state, double angle, DataPacket input){
		super("Dodge", state, input.elapsedSeconds);
				
		if(wildfire.lastDodge + 2 > timeStarted || input.car.velocity.z < -1){
			failed = true; 
		}else{
			this.angle = angle;
			wildfire.lastDodge = timeStarted;
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(1).withBoost(false);
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		if(timeDifference <= 160){
			controller.withJump(timeDifference <= 90);
			controller.withPitch(-1);
		}else if(timeDifference <= 750){
			controller.withJump(true);
	        controller.withYaw((float)-Math.sin(angle));
	        controller.withPitch((float)-Math.cos(angle)); 
		}else if(input.car.position.z > 160 || timeDifference >= 2400){
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}
	        
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (timeDifference(input.elapsedSeconds) > 0.4 && input.car.hasWheelContact);
	}

}
