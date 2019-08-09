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
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class SmartDodgeAction extends Action {
	
	/*
	 * Constants
	 */
	public final static double jumpVelocity = 547.7225575, tick = 1D / 60, carHeight = 17D, maxJumpHeight = 230.76923076923077D;
	
	/*
	 * Tweaky things
	 */
	public final static double dodgeDistance = 60, peakThreshold = 0.15; 
	
	public PredictionSlice target = null;
	private PID rollPID, pitchPID;

	public SmartDodgeAction(State state, DataPacket input, boolean coneRequired){
		super("Smart Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.wildfire.ballPrediction;
		
		if(ballPrediction == null || wildfire.lastDodgeTime(input.elapsedSeconds) < 0.5 || !input.car.hasWheelContact){
			this.failed = true;
			return;
		}
		
		double peakTime = (jumpVelocity / Constants.GRAVITY);
		for(int i = 0; i < Math.min(ballPrediction.slicesLength(), (peakTime + peakThreshold) / tick); i++){
			Vector3 ballLocation = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			double time = (ballPrediction.slices(i).gameSeconds() - input.elapsedSeconds);

			Vector3 carLocation = getJumpPosition(input.car, jumpVelocity, time);
			Vector3 displace = ballLocation.minus(carLocation);
			double distance = displace.magnitude();
			
			//Cone
			if(coneRequired){
				if(Utils.teamSign(input.car) * carLocation.y > Constants.PITCHLENGTH) continue; //Inside enemy goal
				Vector2 trace = Utils.traceToY(carLocation.flatten(), ballLocation.minus(carLocation).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
				if(trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS) continue;
			}

			if(distance < Constants.BALLRADIUS + dodgeDistance && displace.normalized().z < 0.75){
				this.target = new PredictionSlice(ballLocation, i);
				break;
			}
		}
		
		this.failed = (target == null);
		if(!failed){
//			this.pitchPID = new PID(6.5, 0, 0.5);
//			this.rollPID  = new PID(3.0, 0, 0.2);
			
			this.pitchPID = new PID(3.65, 0, 0.75);
			this.rollPID  = new PID(3.80, 0, 0.60);
		}
	}
	
	public static PredictionSlice getCandidateLocation(BallPrediction ballPrediction, CarData car, Vector2 enemyGoal){
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			if(location.z < 110) break;
			
//			double time = ballPrediction.slices(i).gameSeconds() - car.elapsedSeconds;
//			double jumpHeight = getJumpPosition(car, jumpVelocity, time).z - car.position.z;
			
			if(location.z < maxJumpHeight /*jumpHeight*/ + dodgeDistance * 0.66){
				location = location.plus(location.minus(enemyGoal.withZ(location.z)).scaledToMagnitude(Constants.BALLRADIUS + dodgeDistance * 0.58));
				return new PredictionSlice(location, i);
			}
		}
		return null;
	}

	private static Vector3 getJumpPosition(CarData car, double jumpVelocity, double time){
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
		
		if(timeDifference < Math.min(0.2, this.target.getTime() - tick * 3)){
			return controller.withJump(true);
		}else if(timeDifference >= this.target.getTime() - tick){
//			if(input.car.doubleJumped){
			if(input.car.doubleJumped && timeDifference(input.elapsedSeconds) > this.target.getTime() + 1){
				Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds).bangBang(input.car.position.z < 500));
			}
			
			if(!input.car.doubleJumped){
				//Dodge
				controller.withJump(true);
				double angle = Handling.aim(input.car, (Behaviour.isOnPrediction(wildfire.ballPrediction, this.target.getPosition()) ? this.target.getPosition() : input.ball.position));
				controller.withPitch(-Math.cos(angle));
		        controller.withRoll(-Math.sin(angle) * 1.5);
		        wildfire.resetDodgeTime(input.elapsedSeconds); //No spamming!
			}else{
				controller.withPitch(0);
		        controller.withRoll(0);
			}
		}else if(input.car.velocity.z > 50){
			//Point the car
			Vector3 direction = target.getPosition().minus(input.car.position).normalized();
			direction = new Vector3(-direction.x, -direction.y, 2.5).normalized();
			wildfire.renderer.drawLine3d(Color.RED, input.car.position.toFramework(), input.car.position.plus(direction.scaledToMagnitude(120)).toFramework());
			
			Vector2 angles = Handling.getAnglesRoof(Utils.toLocalFromRelative(input.car, direction));
			
			double pitch = this.pitchPID.getOutput(input.elapsedSeconds, 0, angles.y);;
			double roll = this.rollPID.getOutput(input.elapsedSeconds, 0, angles.x);
			
			controller.withPitch(pitch);
			controller.withRoll(roll);
		}
		
		return controller;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > (input.car.hasWheelContact ? 0.5 : 2.5));
	}

}
