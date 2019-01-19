package wildfire.wildfire.actions;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class RecoveryAction extends Action {
	
	/*
	 * Good old PID controllers, they never fail
	 */
	
	private PID rollPID, pitchPID;

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);
		this.rollPID = new PID(1, 0, 0.24);
		this.pitchPID = new PID(1.2, 0, 0.4);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//whatisaphone's Secret Recipe
		boolean boostDown = (Utils.timeToHitGround(input.car) > 0.85 && input.car.boost > 5 && Utils.distanceToWall(input.car.position) > 100 && input.car.position.z > 350);
		
		double roll = rollPID.getOutput(input.elapsedSeconds, -input.car.orientation.rightVector.z, 0);
		double pitch = pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.noseVector.z, boostDown ? -0.85 : 0);	
		
		return new ControlsOutput().withRoll((float)roll).withPitch((float)pitch).withBoost(input.car.orientation.noseVector.z < -0.75 && boostDown).withThrottle(timeDifference(input.elapsedSeconds) > 1 ? 1 : 0); //Throttle to avoid turtling
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}

}
