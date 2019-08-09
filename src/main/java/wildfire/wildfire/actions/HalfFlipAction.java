package wildfire.wildfire.actions;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;

public class HalfFlipAction extends Action {
	
	private final static int throttleTime = 300;

	private Vector3 direction;

	public HalfFlipAction(State state, CarData car){
		super("Half-Flip", state, car.elapsedSeconds);
		
		// No spamming!
		if(wildfire.lastDodgeTime(car.elapsedSeconds) < 1){
			failed = true; 
		}else{
			wildfire.resetDodgeTime(car.elapsedSeconds);
			this.direction = car.orientation.noseVector.scaled(-1);
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		ControlsOutput controls = new ControlsOutput().withThrottle(-1).withBoost(false);
		
		if(timeDifference < throttleTime){
			controls.withThrottle(-1);
		}else{
			timeDifference -= throttleTime;
			if(timeDifference <= 140){
				controls.withJump(timeDifference <= 70);
				controls.withPitch(1);
			}else if(timeDifference <= 300){
				controls.withJump(true);
				controls.withPitch(1);
			}else if(timeDifference <= 1250){
				controls.withJump(false);
				controls.withBoost(timeDifference >= 900);
				
				// Stabilise.
				double[] angles = AirControl.getPitchYawRoll(input.car, direction.flatten());
				controls.withPitchYawRoll(angles);
			}
		}
	        
		return controls;
	}

	@Override
	public boolean expire(DataPacket input){
		return failed || (input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.4 + throttleTime / 1000);
	}

}
