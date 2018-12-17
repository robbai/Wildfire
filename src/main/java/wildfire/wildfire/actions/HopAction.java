package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Action;
import wildfire.wildfire.PID;
import wildfire.wildfire.State;
import wildfire.wildfire.Utils;

public class HopAction extends Action {	

	private Vector2 target;
	private int throttleTime;
	private PID yawPID;

	public HopAction(State state, Vector2 target, Vector3 initialVelocity){
		super("Hop", state);
		this.target = target;
		this.throttleTime = (int)Math.min(300, initialVelocity.flatten().magnitude() / 6);
		
		this.yawPID = new PID(state.wildfire.renderer, Color.RED, 2, 0.2, 1.05);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(0).withBoost(false);
		
		long timeDifference = timeDifference();
		state.wildfire.renderer.drawString2d("Throttle Time: " + throttleTime, Color.WHITE, new Point(0, 40), 2, 2);
		
		if(timeDifference <= throttleTime){
			controller.withThrottle((float)-Math.signum(input.car.forwardMagnitude()));
		}else{
			if(input.car.hasWheelContact){
				controller.withJump(true);
			}else{
				double targetAngle = Utils.aim(input.car, target);
				double yaw = yawPID.getOutput(targetAngle, 0);
		        controller.withYaw((float)yaw);
				
				//This is a very rough recovery, no PID controllers, just pure multiplication
				controller.withPitch((float)-input.car.orientation.noseVector.z * 0.5F);
				controller.withRoll((float)input.car.orientation.rightVector.z * 0.5F);
				
				//Avoid turtling 
				if(timeDifference > 300) controller.withThrottle(1);
			}
		}
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return timeDifference() > 160 + throttleTime && input.car.hasWheelContact;
	}
	
	@SuppressWarnings("unused")
	private Vector3 aim(double pitch, double yaw, double roll){
	    double x = -1 * Math.cos(pitch) * Math.cos(yaw);
	    double y = Math.cos(pitch) * Math.sin(yaw);
	    double z = Math.sin(pitch);
	    return new Vector3(x, y, z);
	}

}