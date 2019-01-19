package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class HopAction extends Action {	

	private Vector2 target;
	private int throttleTime;
	private PID yawPID;

	public HopAction(State state, DataPacket input, Vector2 target){
		super("Hop", state, input.elapsedSeconds);
		this.target = target;
		this.throttleTime = (int)Math.min(300, input.car.velocity.flatten().magnitude() / 6);
		this.yawPID = new PID(3.2, 0, 1.1);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(0).withBoost(false);
		
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		//Switch to recovery
		if(timeDifference > 1500 && input.car.position.z > 400){
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}
		
//		if(throttleTime != 0) state.wildfire.renderer.drawString2d("Throttle: " + throttleTime + "ms", Color.WHITE, new Point(0, 40), 2, 2);
		
		if(timeDifference <= throttleTime){
			controller.withThrottle((float)-Math.signum(input.car.forwardMagnitude()));
		}else if(timeDifference < throttleTime + 40){
			controller.withJump(true);
		}else{
			double targetAngle = Utils.aim(input.car, target);
			double yaw = yawPID.getOutput(input.elapsedSeconds, targetAngle, 0);
			controller.withYaw((float)yaw);

			//This is a very rough recovery, no PID controllers, just pure multiplication
			controller.withPitch((float)-input.car.orientation.noseVector.z * 0.5F);
			controller.withRoll((float)input.car.orientation.rightVector.z * 0.5F);

			//Avoid turtling 
			controller.withThrottle(timeDifference > 2000 ? 1 : 0);
		}
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return timeDifference(input.elapsedSeconds) * 1000 > 160 + throttleTime && input.car.hasWheelContact;
	}
	
	@SuppressWarnings("unused")
	private Vector3 aim(double pitch, double yaw, double roll){
	    double x = -1 * Math.cos(pitch) * Math.cos(yaw);
	    double y = Math.cos(pitch) * Math.sin(yaw);
	    double z = Math.sin(pitch);
	    return new Vector3(x, y, z);
	}

}
