package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class HopAction extends Action {	

	private Vector2 target;
	private double throttleTime;
	
	private PID yawPID, pitchPID, rollPID;

	public HopAction(State state, DataPacket input, Vector2 target){
		super("Hop", state, input.elapsedSeconds);
		this.target = target;
		this.throttleTime = Math.min(3, input.car.velocity.flatten().magnitude() / 6000);
		
		this.yawPID = new PID(3.2, 0, 1.1);
		this.pitchPID = new PID(3.8, 0, 0.52);
		this.rollPID = new PID(1.6, 0, 0.24);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(0).withBoost(false);
		
		float timeDifference = timeDifference(input.elapsedSeconds);
		
		//Switch to recovery
		if(timeDifference > 1.5 && input.car.position.z > 400){
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}
		
//		if(throttleTime != 0) state.wildfire.renderer.drawString2d("Throttle: " + throttleTime + "ms", Color.WHITE, new Point(0, 40), 2, 2);
		
		if(timeDifference <= throttleTime){
			controller.withThrottle((float)-Math.signum(input.car.forwardVelocity));
		}else if(timeDifference < throttleTime + 0.0167){
			controller.withJump(true);
		}else{
			double angularCoefficient = Math.signum(Math.cos(input.car.orientation.eularRoll));
			
			double targetAngle = Handling.aim(input.car, target);
			double yaw = yawPID.getOutput(input.elapsedSeconds, targetAngle, 0);
			controller.withYaw((float)(angularCoefficient * yaw));

			controller.withRoll((float)rollPID.getOutput(input.elapsedSeconds, -input.car.orientation.rightVector.z, 0));
			controller.withPitch((float)(angularCoefficient * pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.noseVector.z, 0)));

			//Avoid turtling 
			controller.withThrottle(timeDifference > Math.min(0.44, throttleTime + 0.12) ? 1 : 0);
		}
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return timeDifference(input.elapsedSeconds) > 0.16 + throttleTime && input.car.hasWheelContact;
	}
	
	@SuppressWarnings("unused")
	private Vector3 aim(double pitch, double yaw, double roll){
	    double x = -1 * Math.cos(pitch) * Math.cos(yaw);
	    double y = Math.cos(pitch) * Math.sin(yaw);
	    double z = Math.sin(pitch);
	    return new Vector3(x, y, z);
	}

}
