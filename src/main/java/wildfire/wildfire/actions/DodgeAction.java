package wildfire.wildfire.actions;

import wildfire.output.ControlsOutput;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Utils;

public class DodgeAction extends Action {
	
//	private static DodgeTable table = new DodgeTable();
	
	private double angle;
	
	public DodgeAction(State state, double desiredAngle, InfoPacket input, boolean ignoreCooldown){
		super("Dodge", state, input.elapsedSeconds);
		
		if(!ignoreCooldown && wildfire != null && (input.info.timeOnGround < 0.3 || input.car.velocity.z < -1)){
			failed = true; 
		}else{
//			this.angle = table.getInputForAngle(desiredAngle, input.car.forwardVelocity, true);
			this.angle = desiredAngle;
		}
	}
	
	public DodgeAction(State state, double angle, InfoPacket input){
		this(state, angle, input, false);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		ControlsOutput controller = new ControlsOutput().withThrottle(1).withBoost(false);
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		if(timeDifference <= 160){
			controller.withJump(timeDifference <= 90);
			controller.withPitch(-Math.signum(Math.cos(angle)));
		}else if(timeDifference <= 750){
			controller.withJump(true);
	        controller.withYaw(-Math.sin(angle));
	        controller.withPitch(-Math.cos(angle)); 
		}else if(input.car.position.z > 160 || timeDifference >= 2400){
			if(this.state != null) Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}
	        
		return controller;
	}

	@Override
	public boolean expire(InfoPacket input){
		return failed || (timeDifference(input.elapsedSeconds) > 0.4 && input.car.hasWheelContact);
	}

}
