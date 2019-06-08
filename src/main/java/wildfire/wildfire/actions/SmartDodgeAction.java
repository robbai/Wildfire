package wildfire.wildfire.actions;

import java.awt.Color;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class SmartDodgeAction extends Action {
	
	/*
	 * Constants
	 */
	public final static double jumpVelocity = 547.7225575, tick = 1D / 60, carHeight = 17D, maxJumpHeight = 230.76923076923077D;
	
	public PredictionSlice target = null;
	private PID rollPID, pitchPID;

	public SmartDodgeAction(State state, DataPacket input, boolean coneRequired){
		super("Smart Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.wildfire.ballPrediction;
		
		if(ballPrediction == null || wildfire.lastDodgeTime(input.elapsedSeconds) < 0.5 || !input.car.hasWheelContact){
			this.failed = true;
			return;
		}
		
		int shortestDistance = Integer.MAX_VALUE;
		
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
//			location = location.plus(input.car.position.minus(location).scaledToMagnitude(Constants.BALLRADIUS));
			
			double time = (ballPrediction.slices(i).gameSeconds() - input.elapsedSeconds);

			Vector3 carPosition = getJumpPosition(input.car, jumpVelocity, time);
			double distance = location.distance(carPosition);
			
			//Cone
			if(coneRequired){
				if(Utils.teamSign(input.car) * carPosition.y > Constants.PITCHLENGTH) continue; //Inside enemy goal
				Vector2 trace = Utils.traceToY(carPosition.flatten(), location.minus(carPosition).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
				if(trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS) continue;
			}
			
//			if(i < 100) System.out.println(Utils.round(time) + "s (" + i + ") would be " + (int)distance + "uu away");

			shortestDistance = (int)Math.min(distance, shortestDistance);
			if(distance < Constants.BALLRADIUS + 60){
				this.target = new PredictionSlice(location, i);
				break;
			}
		}
		
		this.failed = (target == null);
//		System.out.println(this.failed + ": " + shortestDistance);
		if(!failed){
			this.pitchPID = new PID(wildfire.renderer, Color.BLUE, 6.5, 0, 0.5);
			this.rollPID = new PID(wildfire.renderer, Color.YELLOW, 3, 0, 0.2);
		}
	}
	
	public static PredictionSlice getCandidateLocation(BallPrediction ballPrediction, Vector2 enemyGoal){
//		int peakTick = (int)((jumpVelocity / Constants.GRAVITY) / tick);
		for(int i = (int)(0.2 / tick); i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			if(location.z < 100) break;
			
			Vector3 velocity = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().velocity());
			if(Math.signum(enemyGoal.y) == Math.signum(velocity.y)) velocity = new Vector3(velocity.x, 0, velocity.z);
			
			if(location.z - carHeight < maxJumpHeight){
				//Add an offset, which considers the ball's velocity, and the direction to their goal
//				location = location.plus(location.flatten().minus(enemyGoal).plus(velocity.scaled(0.5).flatten()).scaledToMagnitude(120).withZ(0));
								
				location = location.plus(location.flatten().minus(enemyGoal).scaledToMagnitude(Constants.BALLRADIUS / 2).withZ(0));			
				
				return new PredictionSlice(location, i);
			}
		}
		return null;
	}

	private Vector3 getJumpPosition(CarData car, double jumpVelocity, double time){
		Vector3 lastPosition = car.position;
		Vector3 position = car.position;		
		Vector3 velocity = car.velocity;
		
		//Jump
		velocity = velocity.plus(car.orientation.roofVector.scaled(jumpVelocity)).capMagnitude(2300); 
		
		double t = 0;
		while(t < time){
			lastPosition = position;
			position = position.plus(velocity.scaled(tick));
			
			//Gravity
			velocity = velocity.plus(new Vector3(0, 0, -Constants.GRAVITY * tick)).capMagnitude(2300); 
			if(velocity.z < 0) break;
			
			t += tick;
		}
		
		//Interpolate
		double overflow = (t - time) / tick;
		return lastPosition.plus(position.minus(lastPosition).scaled(overflow));
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controller = new ControlsOutput().withNone().withThrottle(timeDifference(input.elapsedSeconds) > this.target.getTime() ? 1 : 0);
		double timeDifference = timeDifference(input.elapsedSeconds);
		
		wildfire.renderer.drawCrosshair(input.car, this.target.getPosition(), Color.ORANGE, 60);
		if(timeDifference < this.target.getTime()) wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, this.target.getFrame() - (int)(timeDifference * 60));
		
		if(timeDifference < Math.min(0.2, this.target.getTime() - tick * 4)){
			return controller.withJump(true);
		}else if(timeDifference >= this.target.getTime()){
			if(input.car.doubleJumped && timeDifference(input.elapsedSeconds) > this.target.getTime() + 0.1){
				Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
			}
			
			//Dodge
			controller.withJump(true);
			double angle = Handling.aim(input.car, this.target.getPosition().flatten());
			controller.withPitch((float)-Math.cos(angle));
	        controller.withRoll((float)-Math.sin(angle) * 1.5F);
	        wildfire.resetDodgeTime(input.elapsedSeconds); //No spamming!
		}else if(input.car.velocity.z > 50){
			//Point the car
			Vector3 direction = target.getPosition().minus(input.car.position).normalized();
			direction = new Vector3(direction.x, direction.y, direction.z).normalized();
			wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.toFramework(), input.car.position.plus(direction.scaledToMagnitude(400)).toFramework());
			direction = Utils.toLocalFromRelative(input.car, direction);
			
//			System.out.println(direction.toString());
			
			double pitch = pitchPID.getOutput(input.elapsedSeconds, 0, Math.signum(direction.z) * Math.asin(direction.z / 16));
			double roll = rollPID.getOutput(input.elapsedSeconds, 0, -Math.signum(direction.y) * Math.asin(direction.y / 32));
			controller.withPitch((float)pitch);
			controller.withRoll((float)roll);
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > (input.car.hasWheelContact ? 0.5 : 2.5));
	}

}
