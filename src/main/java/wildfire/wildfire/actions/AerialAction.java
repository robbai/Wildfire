package wildfire.wildfire.actions;

import java.awt.Color;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class AerialAction extends Action {
	
	/*What point of the ball we wish to hit*/
	private final Vector3 offset = new Vector3(0, 0, Utils.BALLRADIUS * -0.75);
	
	private boolean startMidair;
	private boolean doubleJump;
	private Vector3 target;
	
	private PID pitchPID, yawPID, rollPID;

	public AerialAction(State state, DataPacket input, boolean doubleJump){
		super("Aerial", state, input.elapsedSeconds);
		this.target = wildfire.impactPoint.getPosition().plus(offset);
		this.startMidair = !input.car.hasWheelContact;
		this.doubleJump = !startMidair && doubleJump;
		
		this.pitchPID = new PID(wildfire.renderer, Color.BLUE, 4, 0, 0.4);
		this.rollPID = new PID(wildfire.renderer, Color.YELLOW, 2.4, 0, 0.6);
		this.yawPID = new PID(wildfire.renderer, Color.RED, 4.4, 0, 1.1);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		
		//Slight offset so we hit the centre
		Vector3 impactPoint = wildfire.impactPoint.getPosition().plus(offset);
		if(input.ball.velocity.flatten().isZero()){
			target = input.ball.position.plus(offset);
		}else if(target != null){
			if(timeDifference > 500) target = impactPoint.plus(target).scaled(0.5); //Smooth transition
		}else{
			target = impactPoint;
		}
		
		//Draw the crosshair
		wildfire.renderer.drawCrosshair(input.car, target, Color.YELLOW, 125);
		
		ControlsOutput controller = new ControlsOutput().withThrottle(1).withBoost(false);
		
		if(timeDifference <= 400 && !startMidair){
			controller.withBoost(timeDifference >= 420 || input.car.position.z > 300);
			controller.withJump(timeDifference < 160 || (timeDifference > 300 && doubleJump));
		}else{	        
	        //Bail out
	        if(input.car.magnitudeInDirection(target.minus(input.car.position).flatten()) < -1200 || (input.car.position.distance(target) < 200 && input.car.boost < 20)){
	        	Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
	        }
		}
		
		//This is the angling part
		if(timeDifference > (doubleJump ? 380 : 200) || timeDifference < 170 || startMidair){
			Vector3 targetRelative = target.minus(input.car.position);
			
			//The last second decision to point directly at the ball
			boolean pointStraight = (input.car.position.distance(target) / input.car.velocity.magnitude()) < 0.335D && input.car.velocity.magnitude() > 2000;
			if(pointStraight){
				//Redraw the crosshair
				wildfire.renderer.drawCrosshair(input.car, target, Color.RED, 150);
			}
			
	        //Stay flat by rolling
			double currentRoll = input.car.orientation.eularRoll;
//			double rollSign = Math.abs(currentRoll) > Math.PI / 2 ? -1 : 1;
			double rollSign = 1;
			double roll = rollPID.getOutput(input.elapsedSeconds, currentRoll, rollSign == 1 ? 0 : Math.PI * Math.signum(currentRoll));
			controller.withRoll((float)roll);
	        
	        //The rest is pitch and yaw
			Vector3 path = pointStraight ? input.car.orientation.noseVector : simulate(input.car, 1D / 60D).scaledToMagnitude(targetRelative.magnitude());
			
			//Rendering
//			wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.toFramework(), targetRelative.scaledToMagnitude(2000).plus(input.car.position).toFramework());
//			if(!input.car.hasWheelContact && !pointStraight) wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.toFramework(), path.scaledToMagnitude(2000).plus(input.car.position).toFramework());
			renderFall(Color.PINK, input.car.position, input.car.velocity, null);
			renderFall(Color.CYAN, input.car.position, input.car.velocity, input.car.orientation.noseVector);
			
			//Get our current angles
			Vector3 currentAngles = toPitchYawRoll(path);
			double currentPitch = currentAngles.x;
			double currentYaw = currentAngles.y;
			
			//Get the desired angles
			Vector3 desiredAngles = toPitchYawRoll(targetRelative);
			double desiredPitch = desiredAngles.x;
			double desiredYaw = desiredAngles.y;
			
			//It is useful to wrap our yaw angle
			double yawCorrection = Utils.wrapAngle(desiredYaw - currentYaw);
			
			//Get the PID output
			double pitch = rollSign * pitchPID.getOutput(input.elapsedSeconds, currentPitch, desiredPitch);
			double yaw = rollSign * yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
			
			//Managing boost
			if(!pointStraight){
				boolean boost = Math.abs(desiredPitch - currentPitch) < 1.3 && Math.abs(yawCorrection) < 1.1;
				controller.withBoost(boost);
				if(!boost) controller.withBoost(timeDifference < 1200 && input.car.velocity.z < 0 && currentPitch > 0);
			}

			controller.withPitch((float)pitch);
			if(input.car.orientation.eularPitch + Utils.clampSign(pitch) > 1){
				controller.withPitch((float)(1 - input.car.orientation.noseVector.z));
			}
			
			controller.withYaw((float)yaw);
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return timeDifference(input.elapsedSeconds) > 0.4 && input.car.hasWheelContact;
	}
	
	private Vector3 simulate(CarData car, double scale){
		Vector3 position = new Vector3(car.velocity).scaled(scale);
		position = position.plus(car.orientation.noseVector.scaledToMagnitude(1000 * scale)); //Boost
		position = position.plus(new Vector3(0, 0, -Utils.GRAVITY * scale)); //Gravity
		return position.normalized();
	}
	
	private Vector3 toPitchYawRoll(Vector3 direction){
		direction = direction.normalized();
		double pitch = Math.asin(direction.z);
		double yaw = -Math.atan2(direction.y, direction.x);
		return new Vector3(pitch, yaw, 0); //Rolling has no effect
	}
	
	private void renderFall(Color colour, Vector3 start, Vector3 velocity, Vector3 nose){
		if(start.isOutOfBounds()) return;
		double scale = 1D / 10;
		velocity = velocity.plus(new Vector3(0, 0, -Utils.GRAVITY * scale)); //Gravity
		if(nose != null) velocity = velocity.plus(nose.scaledToMagnitude(1000D * scale)); //Boost
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		Vector3 next = start.plus(velocity.scaled(scale));
		wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
		renderFall(colour, next, velocity, nose);
	}

}
