package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;

public class HalfFlipAction extends Action {
	
	private final int throttleTime = 300;

	public HalfFlipAction(State state, float elapsedSeconds){
		super("Half Flip", state, elapsedSeconds);
		
		//No spamming!
		if(wildfire.lastDodge + 2 > timeStarted){
			failed = true; 
		}else{
			wildfire.lastDodge = timeStarted;
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(-1).withBoost(false);
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		if(timeDifference < throttleTime){
			controller.withThrottle(-1);
		}else{
			timeDifference -= throttleTime;
			if(timeDifference <= 100){
				controller.withJump(timeDifference <= 50);
				controller.withPitch(1);
			}else if(timeDifference <= 350){
				controller.withJump(true);
				controller.withPitch(1);
			}else if(timeDifference <= 1250){
				controller.withJump(false);
				controller.withPitch(-1);
				controller.withRoll(timeDifference <= 450 ? 0 : (float)Math.signum(input.car.orientation.noseVector.z));
				controller.withBoost(timeDifference >= 950);
			}
		}
	        
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.4 + throttleTime / 1000);
	}

}
