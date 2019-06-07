package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Physics;
import wildfire.wildfire.utils.Utils;

public class WavedashAction extends Action {
	
	/*
	 * Starts a wavedash from the ground
	 */
	
	private static final boolean yawRollControl = false;
	
	private PID pitchPID, rollPID, yawPID;
	private boolean jumped;

	public WavedashAction(State state, DataPacket input){
		super("Wavedash", state, input.elapsedSeconds);
		
		this.failed = !input.car.hasWheelContact && input.car.position.z < 200 && wildfire.lastDodgeTime(input.elapsedSeconds) > 1;
		
		if(!this.failed){
			this.pitchPID = new PID(7.5, 0, 1.05);	
			if(yawRollControl){
				this.rollPID = new PID(3, 0, 0.4);
				this.yawPID = new PID(0.2, 0, 0.05);
			}
			
			this.jumped = false;
			
			wildfire.resetDodgeTime(input.elapsedSeconds);
		}
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double timeDifference = this.timeDifference(input.elapsedSeconds);
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeDifference), Color.WHITE, new Point(0, 40), 2, 2);
		
		ControlsOutput controls = new ControlsOutput();
		
		double roll = 0, yaw = 0;
		if(yawRollControl){
			roll = rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, 0);
			yaw = yawPID.getOutput(input.elapsedSeconds, 0, input.car.orientation.noseVector.flatten().correctionAngle(input.car.velocity.flatten()));
		}
		
		controls.withRoll((float)roll).withYaw((float)yaw);
		
		double targetPitch = 0;
		
		if(timeDifference < 0.39){
			controls.withJump(!this.jumped);
			this.jumped = true;
			targetPitch = -0.42;
		}else{
			if(input.car.velocity.z < -1 && Physics.timeToHitGround(input.car) < 0.075){
				targetPitch = -3;
				controls.withJump(!input.car.doubleJumped);
			}else{
				targetPitch = 0.38;
			}
		}
		
		double pitch = pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.eularPitch, targetPitch);
		if(controls.holdJump()){
			controls.withRoll(0);
			controls.withYaw(0);
		}
		return controls.withPitch((float)(input.car.doubleJumped ? 0 : pitch)).withBoost(input.car.orientation.noseVector.z < 0).withThrottle(1);
	}

	@Override
	public boolean expire(DataPacket input){
		return this.timeDifference(input.elapsedSeconds) > (input.car.hasWheelContact || input.car.doubleJumped ? 0.6 : 1.3);
	}

}
