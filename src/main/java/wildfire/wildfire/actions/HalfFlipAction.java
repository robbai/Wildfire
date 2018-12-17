package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Action;
import wildfire.wildfire.State;

public class HalfFlipAction extends Action {
	
	private final int throttleTime = 300;

	public HalfFlipAction(State state){
		super("Half Flip", state);
		
		if(state.wildfire.lastDodge + 2000 > timeStarted){
			failed = true; //Dodge hasn't recharged yet
		}else{
			state.wildfire.lastDodge = timeStarted;
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(-1).withBoost(false);
		long timeDifference = (System.currentTimeMillis() - timeStarted);
		
		if(timeDifference < throttleTime){
			controller.withThrottle(-1);
		}else{
			timeDifference -= throttleTime;
			if(timeDifference <= 100){
				controller.withJump(timeDifference <= 50);
				controller.withPitch(1);
			}else if(timeDifference <= 260){
				controller.withJump(true);
				controller.withPitch(1);
			}else if(timeDifference <= 1200){
				controller.withJump(false);
				controller.withPitch(-1);
				controller.withRoll(timeDifference <= 500 ? 0F : (float)Math.signum(input.car.orientation.noseVector.z));
				controller.withBoost(timeDifference > 950F);
			}
		}
	        
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (System.currentTimeMillis() >= 400 + throttleTime + timeStarted && input.car.hasWheelContact);
	}

}