package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class OrientAction extends Action {
	
	/**
	 * Test action
	 */
	
	private final boolean roof = true; 
	
	private PID pitchPID, yawPID, rollPID;

	public OrientAction(State state, DataPacket input){
		super("OrientTest", state, input.elapsedSeconds);
		
		this.pitchPID = new PID(3.65, 0, 0.75);
		this.yawPID   = new PID(4.05, 0, 1.10);
		this.rollPID  = new PID(3.80, 0, 0.60);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		float time = input.elapsedSeconds;
		
		Vector3 target = Utils.toLocal(input.car, input.ball.position).normalized();
		Vector2 angles = (roof ? Handling.getAnglesRoof(target) : Handling.getAngles(target));
		wildfire.renderer.drawString2d("Angles: " + angles.toString(), Color.WHITE, new Point(0, 40), 2, 2);
		
		double pitch, yaw, roll;
		if(roof){
			pitch = this.pitchPID.getOutput(time, 0, angles.y);
//			yaw = this.yawPID.getOutput(time, Math.sin(input.car.orientation.eularYaw), 0);
			yaw = 0;
			roll = this.rollPID.getOutput(time, 0, angles.x);
		}else{
			pitch = this.pitchPID.getOutput(time, 0, angles.y);
			yaw = this.yawPID.getOutput(time, 0, angles.x);
			roll = this.rollPID.getOutput(time, Math.sin(input.car.orientation.eularRoll), 0);
//			roll = 0;
		}
		
		return new ControlsOutput().withNone()
				.withPitch(pitch).withYaw(yaw).withRoll(roll);
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact || this.timeDifference(input.elapsedSeconds) > 2.5;
	}

}
