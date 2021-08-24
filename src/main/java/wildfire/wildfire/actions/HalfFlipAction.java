package wildfire.wildfire.actions;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Utils;

public class HalfFlipAction extends Action {

	private Vector3 direction;

	public HalfFlipAction(State state, InfoPacket input){
		super("Half-Flip", state, input.elapsedSeconds);

		CarData car = input.car;

		if(input.info.timeOnGround < 0.3){
			failed = true;
		}else{
			this.direction = car.orientation.forward.scaled(-1).normalised().withZ(-0.2);
		}
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		double time = timeDifference(input.elapsedSeconds);

		ControlsOutput controls = new ControlsOutput().withThrottle(-1).withJump(false);
		controls.withBoost(input.car.orientation.forward.dotProduct(direction) > 0.75);

		// Stabilise.
		double[] angles = AirControl.getPitchYawRoll(input.car, direction);

		if(time <= 0.12){
			controls.withJump(time <= 0.04);
			controls.withPitch(1);
		}else if(time <= 0.33){
			controls.withJump(true);
			controls.withPitch(1);

			if(!input.car.hasDoubleJumped)
				controls.withYaw(0.15 * -Math.signum(input.car.angularVelocity.yaw));

		}else if(time <= 1.25){
			controls.withPitchYawRoll(angles);
		}else{
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}

		if(input.car.hasDoubleJumped || !controls.holdJump()){
			controls.withRoll(angles[2]);
		}

		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
		return failed || (input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.4);
	}

}
