package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;

public class SmartDodgeAction extends Action {
	
	private final double maxJumpHeight = 230.76923076923077D, minJumpHeight = 69.23076923076923D;
	private final double ripperHeight = 31.30D;
	private final double tick = (1D / 60);
	
	/**
	 * In 2D
	 */
	private final double maxTargetDistance = 460D;
	
	private PredictionSlice target = null;
	private double timePressed, timeToPeak;

	public SmartDodgeAction(State state, DataPacket input){
		super("Smart Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.getBallPrediction();
		if(ballPrediction != null && wildfire.lastDodge + 2.5 < timeStarted){
			for(int i = 0; i < ballPrediction.slicesLength(); i++){
				Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
				location = location.plus(input.car.position.minus(location).scaledToMagnitude(Utils.BALLRADIUS));
				
				if(location.distanceFlat(input.car.position) > maxTargetDistance) continue;
				if(input.car.magnitudeInDirection(location.minus(input.car.position).flatten()) < -750) continue;
				
				double ballTime = (double)i / 60;
				double jumpHeight = (location.z - ripperHeight);
				
				this.timePressed = timePressedForHeight(jumpHeight);
				this.timeToPeak = timeToPeak(jumpHeight, timePressed);
				this.timePressed += tick;
				
				if(jumpHeight <= maxJumpHeight && jumpHeight >= minJumpHeight && Math.abs(timeToPeak - (ballTime - 0.12)) < 0.04){
					this.target = new PredictionSlice(location, i);
					break;
				}
			}
		}
		
		this.failed = (target == null);
		if(!failed) wildfire.lastDodge = timeStarted; //No spamming!
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withBoost(false).withSlide(false).withJump(false).withSteer(0).withPitch(0).withRoll(0).withYaw(0).withThrottle(0);
		double timeDifference = timeDifference(input.elapsedSeconds);
		
		wildfire.renderer.drawCrosshair(input.car, this.target.getPosition(), Color.ORANGE, 60);
		wildfire.renderer.drawString2d("Press: " + Utils.round(timePressed) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Peak: " + Utils.round(timeToPeak) + "s", Color.WHITE, new Point(0, 60), 2, 2);
		
		if(!input.car.hasWheelContact && (input.car.velocity.z < -50 || timeDifference > timeToPeak)){
			//Dodge
			double angle = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			controller.withJump(System.currentTimeMillis() % 100 > 50);
	        controller.withYaw((float)-Math.sin(angle));
	        controller.withPitch((float)-Math.cos(angle)); 
	        return controller;
		}else if(timeDifference < timePressed){
			return controller.withJump(true);
		}else if(timeDifference > 2.5 + Math.max(0, timeToPeak)){
			Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
		}else if(input.car.velocity.z > 300){
			controller.withPitch((float)(0.7 - input.car.orientation.noseVector.z) * 4.5F); //Point up
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > timePressed && input.car.hasWheelContact) || (target.getPosition().distanceFlat(input.car.position) > maxTargetDistance);
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
		return 0.035186577527449844D * Math.sqrt(h + 60D) - 0.4D;
	}

}
