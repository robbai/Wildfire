package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.Physics;
import wildfire.wildfire.utils.Utils;

public class WavedashAction extends Action {

	/*
	 * Starts a wavedash from the ground
	 */

	private boolean jumped;

	public WavedashAction(State state, InfoPacket input){
		super("Wavedash", state, input.elapsedSeconds);

		this.failed = (!input.car.hasWheelContact && input.car.position.z < 200 && input.info.timeOnGround > 1);
//		this.failed = (!input.car.hasWheelContact || input.car.position.z > 100 || input.info.timeOnGround < 0.3);

		if(!this.failed){
			this.jumped = false;
		}
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		double timeDifference = this.timeDifference(input.elapsedSeconds);
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeDifference), Color.WHITE, new Point(0, 40), 2, 2);

		ControlsOutput controls = new ControlsOutput();

		double targetPitch = 0;

		if(timeDifference < 0.39){
			controls.withJump(!this.jumped);
			this.jumped = true;
			targetPitch = -0.42;
		}else{
			if(input.car.velocity.z < -1 && Physics.timeToHitGround(input.car) < 0.074){
				targetPitch = -3;
				controls.withJump(!input.car.hasDoubleJumped);
			}else{
				targetPitch = 0.38;
			}
		}

		Vector2 flatNose = input.car.orientation.forward.flatten();
		Vector3 desiredNose = new Vector3(flatNose.x, flatNose.y, Math.tan(targetPitch) * flatNose.magnitude());

		double[] angles = AirControl.getPitchYawRoll(input.car, desiredNose);
		controls.withPitchYawRoll(angles);
		if(controls.holdJump()){
			controls.withRoll(0);
			controls.withYaw(0);
		}

		return controls.withBoost(input.car.orientation.forward.z < 0).withThrottle(1);
	}

	@Override
	public boolean expire(InfoPacket input){
		return this.failed || this.timeDifference(
				input.elapsedSeconds) > (input.car.hasWheelContact || input.car.hasDoubleJumped ? 0.6 : 1.3);
	}

}
