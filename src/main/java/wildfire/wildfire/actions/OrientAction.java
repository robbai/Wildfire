package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
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
	
	private static PID gPitchPID, gYawPID, gRollPID;
	
	private PID pitchPID, yawPID, rollPID;

	public OrientAction(State state, DataPacket input){
		super("OrientTest", state, input.elapsedSeconds);
		
		this.pitchPID = new PID(Color.BLUE, gPitchPID);
		this.yawPID   = new PID(Color.RED, gYawPID);
		this.rollPID  = new PID(Color.YELLOW, gRollPID);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		this.pitchPID.set(gPitchPID).updateRenderer(wildfire.renderer);
		this.yawPID.set(gYawPID).updateRenderer(wildfire.renderer);
		this.rollPID.set(gRollPID).updateRenderer(wildfire.renderer);
		
		float time = input.elapsedSeconds;
		
		Vector3 target = Utils.toLocal(input.car, input.ball.position).normalized();
		Vector3 angles = Handling.getAngles(target);
		wildfire.renderer.drawString2d("Angles: " + angles.toString(), Color.WHITE, new Point(0, 40), 2, 2);
		
		double pitch = this.pitchPID.getOutput(time, 0, angles.y);
		double yaw = this.yawPID.getOutput(time, 0, angles.x);
		double roll = this.rollPID.getOutput(time, 0, angles.z);
		
		return new ControlsOutput().withNone()
				.withPitch(pitch).withYaw(yaw).withRoll(roll);
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}
	
	public static void resetPID(){
		gPitchPID = new PID(3.65, 0, 0.60);
		gYawPID   = new PID(4.05, 0, 1.05);
		gRollPID  = new PID(4.00, 0, 0.55);
	}

}
