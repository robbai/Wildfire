package rlbotexample.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbotexample.input.CarData;
import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector3;
import rlbotexample.wildfire.Action;
import rlbotexample.wildfire.PID;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;

public class AerialAction extends Action {
	
	private final double maxAngle = 1.5614006837182772D;
	private final Vector3 offset = new Vector3(0, 0, -Utils.BALLRADIUS * 0.75);
	
	private boolean startMidair;
	private boolean doubleJump;
	private Vector3 target;
	
	private PID pitchPID, yawPID;

	public AerialAction(State state, DataPacket input, boolean doubleJump){
		super("Aerial", state);
		this.target = state.wildfire.impactPoint.plus(offset);
		this.startMidair = !input.car.hasWheelContact;
		this.doubleJump = !startMidair && doubleJump;
		
		this.pitchPID = new PID(state.wildfire.renderer, Color.BLUE, 4, 0, 0.4);
		this.yawPID = new PID(state.wildfire.renderer, Color.RED, 3.1, 0, 0.9);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Slight offset so we hit the centre
		Vector3 impactPoint = state.wildfire.impactPoint.plus(offset);
		
		if(input.ball.velocity.flatten().isZero()){
			target = input.ball.position.plus(offset);
		}else if(target != null){
			target = impactPoint.plus(target).scaled(0.5); //Smooth transition
		}else{
			target = impactPoint;
		}
		
		//Draw the crosshair
		Utils.drawCrosshair(state.wildfire.renderer, input.car, target, Color.YELLOW, 125);
		
		ControlsOutput controller = new ControlsOutput().withThrottle(1).withBoost(false);
		long timeDifference = timeDifference();
		
		if(timeDifference <= 380 && !startMidair){
			controller.withBoost(timeDifference >= 420 || input.car.position.z > 300);
			controller.withJump(timeDifference < 160 || (timeDifference > 260 && doubleJump));
		}else{	        
	        //Bail out
	        if(input.car.magnitudeInDirection(target.minus(input.car.position).flatten()) < -1200 || (input.car.position.distance(target) < 200 && input.car.boost < 20)){
	        	Utils.transferAction(this, new RecoveryAction(this.state));
	        }
		}
		
		//This is the angling part
		if(timeDifference > (doubleJump ? 340 : 200) || timeDifference < 170 || startMidair){
			Vector3 targetLocal = target.minus(input.car.position);
			
	        //Stay flat by rolling
			controller.withRoll((float)input.car.orientation.rightVector.z * 0.85F);
	        
	        //The rest is pitch and yaw
			Vector3 path = simulate(input.car, 1D / 60D).scaledToMagnitude(targetLocal.magnitude());
			
			state.wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.toFramework(), targetLocal.scaledToMagnitude(2000).plus(input.car.position).toFramework());
			if(!input.car.hasWheelContact) state.wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.toFramework(), path.scaledToMagnitude(2000).plus(input.car.position).toFramework());
			
			Vector3 currentAngles = toPitchYawRoll(path);
			double currentPitch = currentAngles.x;
			double currentYaw = currentAngles.y;
			
			Vector3 desiredAngles = toPitchYawRoll(targetLocal);
			double desiredPitch = desiredAngles.x;
			double desiredYaw = desiredAngles.y;
			
			double yawCorrection = (desiredYaw - currentYaw);
			if(yawCorrection < -maxAngle) yawCorrection += 2 * maxAngle;
			if(yawCorrection > maxAngle) yawCorrection -= 2 * maxAngle;
			
			double pitch = pitchPID.getOutput(currentPitch, desiredPitch);
			double yaw = yawPID.getOutput(0, yawCorrection);
			
			state.wildfire.renderer.drawString2d("Pitch Output: " + Utils.round(pitch), Color.white, new Point(0, 60), 2, 2);
			state.wildfire.renderer.drawString2d("Yaw Output: " + Utils.round(yaw), Color.white, new Point(0, 80), 2, 2);
			
			controller.withBoost((Math.abs(pitch) < 0.9 && Math.abs(yaw) < 1.2) || input.car.position.z < 350);
//			controller.withBoost();
					
			if(input.car.orientation.eularPitch + Math.min(pitch, 1) > 1){
				controller.withPitch((float)(1 - input.car.orientation.noseVector.z));
			}else{
				controller.withPitch((float)pitch);
			}
			controller.withYaw((float)yaw);
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return System.currentTimeMillis() > 600 + timeStarted && input.car.hasWheelContact && input.car.position.z < 1000;
	}
	
	private Vector3 simulate(CarData car, double scale){
		Vector3 position = new Vector3(car.velocity).scaled(scale);
		position = position.plus(car.orientation.noseVector.scaledToMagnitude(1000 * scale)); //Boost
		position = position.plus(new Vector3(0, 0, -650 * scale)); //Gravity
		return position.normalized();
	}
	
	private Vector3 toPitchYawRoll(Vector3 direction){
		direction = direction.normalized();
		double pitch = Math.asin(direction.z);
		
//		double yaw = Math.asin(direction.y / Math.cos(pitch));
		double yaw = Math.atan2(direction.y, direction.x) / Math.PI * -maxAngle;
		
		return new Vector3(pitch, yaw, 0); //Rolling has no effect
	}

}
