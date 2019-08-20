package wildfire.wildfire.actions;

import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;

public class HalfFlipAction extends Action {
	
	private final static int throttleTime = 300;

	private Vector3 direction;

	public HalfFlipAction(State state, InfoPacket input){
		super("Half-Flip", state, input.elapsedSeconds);
		
		CarData car = input.car;
		
		if(input.info.timeOnGround < 0.3){
			failed = true; 
		}else{
			this.direction = car.orientation.noseVector.scaled(-1);
		}
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
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
				controls.withBoost(timeDifference >= 800);
				
				// Stabilise.
				double[] angles = AirControl.getPitchYawRoll(input.car, direction.flatten());
				controls.withPitchYawRoll(angles);
			}
		}
	        
		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
		return failed || (input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.4 + throttleTime / 1000);
	}

}
