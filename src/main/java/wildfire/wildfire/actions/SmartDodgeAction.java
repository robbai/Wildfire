package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class SmartDodgeAction extends Action {
	
	/*
	 * Constants.
	 */
//	public final static double jumpVelocity = 547.7225575, tick = 1D / 60, carHeight = 17D, maxJumpHeight = 230.76923076923077D;
	public final static double tick = (1D / 60);
	
	/*
	 * Tweaky things.
	 */
	public final static double dodgeDistance = 70, zRatio = 0.85; 
	
	public Slice target = null;

	private boolean angleSet = false;
	private double angle;

	private double pressTime = JumpPhysics.maxPressTime;

	public SmartDodgeAction(State state, InfoPacket input, boolean coneRequired){
		super("Smart-Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.wildfire.ballPrediction;
		
		if(ballPrediction == null || input.info.timeOnGround < 0.3 || !input.car.hasWheelContact){
			this.failed = true;
			return;
		}
		
		double maxPeakTime = (JumpPhysics.maxJumpVelocity / Constants.GRAVITY);
		for(int i = 0; i < Math.min(ballPrediction.slicesLength(), maxPeakTime / tick); i++){
			Vector3 ballLocation = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			double time = (ballPrediction.slices(i).gameSeconds() - input.elapsedSeconds);

//			Vector3 carLocation = getJumpPosition(input.car, jumpVelocity, time);
			double pressTime = JumpPhysics.getPressTime(input.car, ballLocation.withZ(ballLocation.z + 50));
			double peakTime = JumpPhysics.getPeakTime(pressTime);
			double jumpVelocity = JumpPhysics.getJumpVelocity(pressTime);
			Vector3 carLocation = input.car.position.plus(input.car.velocity.scaled(time)).plus(input.car.orientation.roofVector.scaledToMagnitude(jumpVelocity * time - 0.5 * Constants.GRAVITY * Math.pow(time, 2)));
			
			Vector3 displace = ballLocation.minus(carLocation);
			double distance = displace.magnitude();
			
			// Cone.
			if(coneRequired){
				if(Utils.teamSign(input.car) * carLocation.y > Constants.PITCHLENGTH) continue; // Inside enemy goal.
				Vector2 trace = Utils.traceToY(carLocation.flatten(), ballLocation.minus(carLocation).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
				if(trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS) continue;
			}

			if(distance < Constants.BALLRADIUS + dodgeDistance && displace.normalized().z < zRatio){
				this.target = new Slice(ballLocation, time - 0.02);
				this.pressTime = pressTime;
				break;
			}
		}
		
		this.failed = (target == null);
	}
	
	public SmartDodgeAction(State state, InfoPacket input){
		this(state, input, false);
	}
	
	public static Slice getCandidateLocation(BallPrediction ballPrediction, CarData car, Vector2 enemyGoal){
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			if(location.z < 120) break;
//			if(location.z < 140) continue;
			
//			double time = ballPrediction.slices(i).gameSeconds() - car.elapsedSeconds;
//			double jumpHeight = getJumpPosition(car, jumpVelocity, time).z - car.position.z;
			
			if(location.z - car.position.z < JumpPhysics.maxJumpHeight + dodgeDistance * 0.8){
//				location = location.plus(location.minus(enemyGoal.withZ(location.z)).scaledToMagnitude(Constants.BALLRADIUS + dodgeDistance * 0.58));
				location = location.plus(location.minus(enemyGoal.withZ(location.z)).scaledToMagnitude(Constants.BALLRADIUS + dodgeDistance * 0.4));
				return new Slice(location, i);
			}
		}
		return null;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		double timeDifference = timeDifference(input.elapsedSeconds);
		CarData car = input.car;
		
		wildfire.renderer.drawString2d("Press Time: " + Utils.round(this.pressTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		
		ControlsOutput controls = new ControlsOutput().withThrottle(timeDifference > this.target.getTime() ? 1 : 0);
		
		wildfire.renderer.drawCrosshair(car, this.target.getPosition(), Color.ORANGE, 60);
		if(timeDifference < this.target.getTime()) wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, this.target.getFrame() - (int)(timeDifference * 60));
		
		boolean onPrediction = Behaviour.isOnPrediction(wildfire.ballPrediction, this.target.getPosition());
		
		if(timeDifference < Math.min(this.pressTime, this.target.getTime() - tick * 2)){
			return controls.withJump(true);
		}else{ 
			if(car.doubleJumped){
				if(timeDifference(input.elapsedSeconds) > this.target.getTime() + 0.7){
					Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
				}

				if(this.angleSet){
					controls.withPitch(-Math.cos(angle));
					controls.withRoll(-Math.sin(angle));
					return controls;
				}
			}
			
			if(timeDifference >= this.target.getTime() - tick){
				// Dodge
				controls.withJump(true);
				this.angle = Handling.aim(car, (onPrediction ? this.target.getPosition() : input.ball.position));
				controls.withPitch(-Math.cos(angle));
				controls.withRoll(-Math.sin(angle));

				this.angleSet = true;
			}else if(car.velocity.z > 40){
				// Point the car.
				Vector3 forward = this.target.getPosition().lerp(input.ball.position, 0.15).withZ(input.ball.position.z - 60).minus(car.position).normalized();
//				Vector3 forward = this.target.getPosition().withZ(input.ball.position.z + 110).minus(car.position).normalized();
				Vector3 roof = forward.scaled(-1).withZ(1 - forward.z);

				double[] angles = AirControl.getPitchYawRoll(car, forward, roof);

				wildfire.renderer.drawLine3d(Color.RED, car.position, car.position.plus(forward.scaledToMagnitude(200)));
				wildfire.renderer.drawLine3d(Color.GREEN, car.position, car.position.plus(roof.scaledToMagnitude(200)));

				controls.withPitchYawRoll(angles);
			}
		}
		
		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > (input.car.hasWheelContact ? 0.5 : 2.5));
	}

}
