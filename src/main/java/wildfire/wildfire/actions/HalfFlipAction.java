package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class HalfFlipAction extends Action {
	
	private PID rollPID, pitchPID;
	
	private final int throttleTime = 300;

	public HalfFlipAction(State state, float elapsedSeconds){
		super("Half Flip", state, elapsedSeconds);
		
		//No spamming!
		if(wildfire.lastDodgeTime(elapsedSeconds) < 1){
			failed = true; 
		}else{
			wildfire.resetDodgeTime(elapsedSeconds);
			
			this.pitchPID = new PID(5.4, 0, 1.5);
			this.rollPID = new PID(2, 0, 0.3);
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
			if(timeDifference <= 140){
				controller.withJump(timeDifference <= 70);
				controller.withPitch(1);
			}else if(timeDifference <= 360){
				controller.withJump(true);
				controller.withPitch(1);
			}else if(timeDifference <= 1250){
				controller.withJump(false);
				controller.withBoost(timeDifference >= 900);
				
				//Use PID controllers to stabilise
				controller.withPitch((float)pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.noseVector.z * Math.signum(Math.cos(input.car.orientation.eularRoll)), 0));
				if(Math.abs(pitchPID.lastError) < 0.45) controller.withRoll((float)rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, 0));
			}
		}
	        
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.4 + throttleTime / 1000);
	}

}
