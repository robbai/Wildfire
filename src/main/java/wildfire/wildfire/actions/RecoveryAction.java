package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class RecoveryAction extends Action {
	
	/*
	 * Good old PID controllers, they never fail
	 */	
	private PID rollPID, pitchPID, yawPID;
	
	private final double renderScale = (1D / 50);

	public RecoveryAction(State state, float elapsedSeconds){
		super("Recovery", state, elapsedSeconds);
		
		this.pitchPID = new PID(4.4, 0, 1.1);
		this.rollPID = new PID(1.6, 0, 0.24);
		this.yawPID = new PID(5.5, 0, 1.6);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//whatisaphone's Secret Recipe
		boolean boostDown = (Utils.timeToHitGround(input.car) > (input.car.doubleJumped ? 0.5 : 0.575) && input.car.boost > 5 && Utils.distanceToWall(input.car.position) > 100 && input.car.position.z > 300);
		
		double angularCoefficient = Math.signum(Math.cos(input.car.orientation.eularRoll));
//		wildfire.renderer.drawString2d("Coefficient: " + Utils.round(angularCoefficient), Color.WHITE, new Point(0, 60), 2, 2);
				
		double yaw = 0;
		
		boolean planWaveDash = (!input.car.doubleJumped && !boostDown && input.car.velocity.z < -420 && input.car.orientation.roofVector.normalized().z > 0.6);
		wildfire.renderer.drawString2d("Plan Wave-Dash: " + planWaveDash, Color.WHITE, new Point(0, 40), 2, 2);
		
		if(input.car.position.z > 50){
			if(input.car.position.z > 120) renderFall(boostDown ? (input.car.orientation.noseVector.z < -0.75 ? Color.RED : Color.YELLOW) : Color.WHITE, input.car.position, input.car.velocity);
			
			Vector2 yawIdealDirection = (input.car.velocity.flatten().magnitude() > 600 ? input.car.velocity.flatten() : wildfire.impactPoint.getPosition().minus(input.car.position).flatten());
			double yawCorrection = -input.car.orientation.noseVector.flatten().correctionAngle(yawIdealDirection);
			yaw = angularCoefficient * yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
		}else{
			//Perform the wave-dash
			if(planWaveDash && input.car.position.z < 75){
				return new ControlsOutput().withPitch(-1).withYaw(0).withRoll(0).withJump(true);
			}
		}
		
		double roll = rollPID.getOutput(input.elapsedSeconds, -input.car.orientation.rightVector.z, 0);
		double pitch = angularCoefficient * pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.noseVector.z, boostDown ? -0.95 : (planWaveDash ? Utils.clamp(input.car.position.z / 3000, 0.25, 0.8) : 0));
		
		return new ControlsOutput().withRoll((float)roll).withPitch((float)pitch).withYaw((float)yaw).withBoost(input.car.orientation.noseVector.z < -0.75 && boostDown).withThrottle(timeDifference(input.elapsedSeconds) > 0.65 ? 1 : 0); //Throttle to avoid turtling
	}

	@Override
	public boolean expire(DataPacket input){
		return input.car.hasWheelContact;
	}
	
	private void renderFall(Color colour, Vector3 start, Vector3 velocity){
		if(start.isOutOfBounds()){
			wildfire.renderer.drawCircle(colour, start.flatten(), 30);
			return;		
		}
		velocity = velocity.plus(new Vector3(0, 0, -Utils.GRAVITY * renderScale)); //Gravity
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		Vector3 next = start.plus(velocity.scaled(renderScale));
		wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
		renderFall(colour, next, velocity);
	}

}
