package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;

public class SmartDodgeAction extends Action {
	
//	private final double maxJumpHeight = 230.76923076923077D, minJumpHeight = 69.23076923076923D;
//	private final double ripperHeight = 31.30D;
	private final double tick = (1D / 60);
	
	private PID pitchPID;
	
	/**
	 * In 2D
	 */
	private final double maxTargetDistance = 300D;
	
	public PredictionSlice target = null;
	private double timePressed, timeToPeak;

	public SmartDodgeAction(State state, DataPacket input, boolean coneRequired){
		super("Smart Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.getBallPrediction();
		if(ballPrediction != null && wildfire.lastDodgeTime(input.elapsedSeconds) > 1.5 && input.car.hasWheelContact){
			for(int i = 0; i < ballPrediction.slicesLength(); i++){
				Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
				location = location.plus(input.car.position.minus(location).scaledToMagnitude(Utils.BALLRADIUS));
				
//				if(location.distanceFlat(input.car.position) > maxTargetDistance) continue;
//				if(input.car.magnitudeInDirection(location.minus(input.car.position).flatten()) < -600) continue;
				
				double ballTime = ((double)i * tick);
				double jumpHeight = (location.z);
				
				this.timePressed = timePressedForHeight(jumpHeight);
				this.timeToPeak = timeToPeak(jumpHeight, timePressed);
				
				Vector3 carPosition = getJumpPosition(input.car, getJumpVelocity(timePressed), timeToPeak);
				if(location.distance(carPosition) > maxTargetDistance) continue;
				
				//Cone
				if(coneRequired){
					if(Utils.teamSign(input.car) * carPosition.y > Utils.PITCHLENGTH) continue; //Inside enemy goal
					Vector2 trace = Utils.traceToY(carPosition.flatten(), location.minus(carPosition).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH);
					if(trace == null || Math.abs(trace.x) > Utils.GOALHALFWIDTH - Utils.BALLRADIUS) continue;
				}
				
				if(timePressed <= 0.2 && Math.abs(timeToPeak - (ballTime - 0.31)) < tick){
//					System.out.println("Frame = " + i + ", Height = " + (int)jumpHeight + "uu, Time Pressed = " + (int)(timePressed * 1000) + "ms");
					this.timePressed += tick;
					this.target = new PredictionSlice(location, i);
					break;
				}
			}
		}
		
		this.failed = (target == null);
		if(!failed){
			this.pitchPID = new PID(5, 0, 0.45);
		}
	}

	private Vector3 getJumpPosition(CarData car, double jumpVelocity, double time){
		Vector3 position = car.position;
		
		Vector3 velocity = car.velocity;
		velocity = velocity.plus(car.orientation.roofVector.scaled(jumpVelocity)); //Jump
		Utils.capMagnitude(velocity, 2300);
		
		double t = 0;
		while(t < time){
			position = position.plus(velocity.scaled(tick));
			
			velocity = velocity.plus(new Vector3(0, 0, -Utils.GRAVITY * tick)); //Gravity
			Utils.capMagnitude(velocity, 2300);
			
			t += tick;
		}
		
		return position;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withBoost(false).withSlide(false).withJump(false).withSteer(0).withPitch(0).withRoll(0).withYaw(0).withThrottle(0);
		double timeDifference = timeDifference(input.elapsedSeconds);
		
		wildfire.renderer.drawCrosshair(input.car, this.target.getPosition(), Color.ORANGE, 60);
		wildfire.renderer.drawString2d("Press: " + Utils.round(timePressed) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Peak: " + Utils.round(timeToPeak) + "s", Color.WHITE, new Point(0, 60), 2, 2);
		
		if(timeDifference < timePressed){
			return controller.withJump(true);
		}else if(!input.car.hasWheelContact && timeDifference > timeToPeak){
			if(timeDifference > 0.04 + timeToPeak || input.car.doubleJumped || input.car.velocity.z < -1000) Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
			
			//Dodge
			double angle = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			controller.withJump(true);
	        controller.withRoll((float)-Math.sin(angle) * 2);
	        controller.withPitch((float)-Math.cos(angle));
	        wildfire.resetDodgeTime(input.elapsedSeconds); //No spamming!
	        return controller;
		}else if(input.car.velocity.z > 200){
			//Point up
			Vector3 carTargetNormalised = target.getPosition().minus(input.car.position).normalized();
			double pitch = pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.noseVector.z, Utils.clamp(carTargetNormalised.z, 0.3, 0.8));
			controller.withPitch((float)pitch); 
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > timePressed + 0.25 && input.car.hasWheelContact);
	}
	
	private BallPrediction getBallPrediction(){
		return this.wildfire.ballPrediction;
	}
	
	private double getJumpVelocity(double timePressed){
		return Math.sqrt(Math.pow(1400D * timePressed + 300D, 2) - (Utils.GRAVITY * 1400D * Math.pow(timePressed, 2)));
//		return 300 + 1400 * timePressed - Utils.GRAVITY * timePressed;
	}
	
	private double timeToPeak(double height, double timePressed){
		double jumpVelocity = getJumpVelocity(timePressed);
		double timeTaken = -(jumpVelocity / -Utils.GRAVITY);		
		return timeTaken;
	}
	
	/** This uses hard-coded gravity (-650),
	 *  since WolframAlpha convinced me it's not worth it rearranging to have gravity on the other side
	 */
	private double timePressedForHeight(double h){
//		return 0.035186577527449844D * Math.sqrt(h + 60D) - 0.4D;
		return 0.1D * Math.sqrt(13D / 105D) * Math.sqrt(h + 60D) - 0.4D;
	}

}
