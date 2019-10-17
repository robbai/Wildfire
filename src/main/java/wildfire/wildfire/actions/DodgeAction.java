package wildfire.wildfire.actions;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class DodgeAction extends Action {
	
//	private static DodgeTable table = new DodgeTable();
	
	private double angle, pitch, yaw;
	
	public DodgeAction(State state, double desiredAngle, InfoPacket input, boolean ignoreCooldown){
		super("Dodge", state, input.elapsedSeconds);
		
		if(!ignoreCooldown && wildfire != null && (input.info.timeOnGround < 0.3 || input.car.velocity.z < -1)){
			failed = true; 
		}
		
//		this.angle = table.getInputForAngle(desiredAngle, input.car.forwardVelocity, true);
		this.angle = desiredAngle;
		
		double[] pitchYaw = pitchYaw(this.angle);
		this.pitch = pitchYaw[0];
		this.yaw = pitchYaw[1];
	}
	
	public DodgeAction(State state, double angle, InfoPacket input){
		this(state, angle, input, false);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		double time = timeDifference(input.elapsedSeconds);
		
		ControlsOutput controls = new ControlsOutput().withThrottle(1).withBoost(false);
		
		if(time <= 0.16){
			controls.withJump(time <= 0.09);
			controls.withPitch(Math.signum(this.pitch));
		}else{
			if(time <= 0.75){
				controls.withPitch(this.pitch);
				controls.withYaw(this.yaw);
			}
			
			if(!car.hasDoubleJumped){
				controls.withJump(true);
			}else if(this.state != null){
				double height = Utils.clamp(input.car.position.z / Constants.CEILING, 0, 1);
				if(!input.info.isDodgeTorquing() || time >= (Math.abs(car.angularVelocity.pitch) + Math.abs(car.angularVelocity.yaw) < 0.9 + height ? (1 - height) * 0.3 : 1.6)){
					Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
				}
			}
		}
	        
		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
		return failed || (timeDifference(input.elapsedSeconds) > 0.4 && input.car.hasWheelContact);
	}
	
	public static double[] pitchYaw(double angle){
		return new double[] {-Math.cos(angle), -Math.sin(angle)};
	}

}
