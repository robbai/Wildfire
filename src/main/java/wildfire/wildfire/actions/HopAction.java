package wildfire.wildfire.actions;

import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Utils;

public class HopAction extends Action {

	private Vector2 target;
	private double throttleTime;

	public HopAction(State state, InfoPacket input, Vector2 target){
		super("Hop", state, input.elapsedSeconds);
		this.target = target;
		this.throttleTime = Math.min(3, input.car.velocity.flatten().magnitude() / 6000);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		ControlsOutput controls = new ControlsOutput().withThrottle(0).withBoost(false);

		float timeDifference = timeDifference(input.elapsedSeconds);

		// Switch to recovery.
		if(timeDifference > 1.5 && input.car.position.z > 400){
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}

//		if(throttleTime != 0) state.wildfire.renderer.drawString2d("Throttle: " + throttleTime + "ms", Color.WHITE, new Point(0, 40), 2, 2);

		if(timeDifference <= throttleTime){
			controls.withThrottle(-Math.signum(input.car.forwardVelocity));
		}else if(timeDifference < throttleTime + 1D / 60){
			controls.withJump(true);
		}else{
			double[] angles = AirControl.getPitchYawRoll(input.car, target.minus(input.car.position.flatten()));
			if(Math.abs(input.car.orientation.pitch) < 0.02)
				angles[0] = 0;

			controls.withPitchYawRoll(angles);

			// Avoid turtling.
			controls.withThrottle(timeDifference > Math.min(0.44, throttleTime + 0.12) ? 1 : 0);
		}

		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
		return timeDifference(input.elapsedSeconds) > 0.16 + throttleTime && input.car.hasWheelContact;
	}

}
