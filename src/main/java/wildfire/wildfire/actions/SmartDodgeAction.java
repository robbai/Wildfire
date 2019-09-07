package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.CarOrientation;
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
	public final static double dodgeDistance = 40, zRatio = 0.75; 
	
	public Slice target = null;

	private boolean angleSet = false;
	private double angle;

	private double pressTime;

	public SmartDodgeAction(State state, InfoPacket input, boolean coneRequired){
		super("Smart-Dodge", state, input.elapsedSeconds);
		
		BallPrediction ballPrediction = this.wildfire.ballPrediction;
		
		if(ballPrediction == null || input.info.timeOnGround < 0.3 || !input.car.hasWheelContact){
			this.failed = true;
			return;
		}
		
		for(int i = 0; i < Math.min(ballPrediction.slicesLength(), JumpPhysics.maxPeakTime * 60); i++){
			Vector3 ballLocation = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			double time = (ballPrediction.slices(i).gameSeconds() - input.elapsedSeconds);
			double jumpHeight = ballLocation.minus(input.car.position).dotProduct(input.car.orientation.roofVector);
			
			OptionalDouble pressOptional = JumpPhysics.getPressForTimeToZ(jumpHeight, time);
//			if(!pressOptional.isPresent()) continue;
//			double press = pressOptional.getAsDouble();
			double press = (pressOptional.isPresent() ? pressOptional.getAsDouble() : JumpPhysics.maxPressTime);
			
			Vector3 carLocation = JumpPhysics.simCar(input.car, press, time);
			Vector3 displace = ballLocation.minus(carLocation);
			double distance = displace.magnitude();
			
			// Cone.
			if(coneRequired){
				if(Utils.teamSign(input.car) * carLocation.y > Constants.PITCHLENGTH) continue; // Inside enemy goal.
				Vector2 trace = Utils.traceToY(carLocation.flatten(), ballLocation.minus(carLocation).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
				if(trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS) continue;
			}

//			if(distance < Constants.BALLRADIUS + dodgeDistance && displace.normalized().z < zRatio){
			if(distance < Constants.BALLRADIUS + dodgeDistance){
//			if(distance < Constants.BALLRADIUS){
				this.target = new Slice(ballLocation, time);
				this.pressTime = press;
				break;
			}
			
			if(pressOptional.isPresent()) break;
		}
		
		this.failed = (target == null);
	}
	
	public SmartDodgeAction(State state, InfoPacket input){
		this(state, input, false);
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
		
//		if(timeDifference < Math.min(this.pressTime, this.target.getTime() - tick * 2)){
		if(timeDifference < this.pressTime){
			return controls.withJump(true);
		}else{
			if(timeDifference(input.elapsedSeconds) > this.target.getTime() + (car.doubleJumped ? 0.7 : 0.4)){
				Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
			}

			if(this.angleSet && car.doubleJumped){
				controls.withPitch(-Math.cos(angle));
				controls.withRoll(-Math.sin(angle));
				return controls;
			}
			
//			if(timeDifference >= this.target.getTime() - tick){
			if(willIntersectNextTick(car, wildfire.ballPrediction, 1)){
				// Dodge
				controls.withJump(true);
				this.angle = Handling.aim(car, (onPrediction ? this.target.getPosition() : input.ball.position));
				controls.withPitch(-Math.cos(angle));
				controls.withRoll(-Math.sin(angle));

				this.angleSet = true;
			}else if(car.velocity.z > 40){
				// Point the car.
//				Vector3 forward = this.target.getPosition().lerp(input.ball.position, 0.15).withZ(input.ball.position.z + 80).minus(car.position).normalized();
				Vector3 forward = this.target.getPosition().withZ(input.ball.position.z + 120).minus(car.position).normalized();
				Vector3 roof = forward.scaled(-1).withZ(1 - forward.z);

				double[] angles = AirControl.getPitchYawRoll(car, forward);

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
	
	private boolean willIntersectNextTick(CarData car, BallPrediction ballPrediction, int ticks){
		Vector3 ballPosition = Vector3.fromFlatbuffer(ballPrediction.slices(ticks).physics().location());
		
		Vector3 carPosition = car.position, carVelocity = car.velocity;
		double pitchVel = car.angularVelocity.dotProduct(car.orientation.rightVector);
	    double yawVel = -car.angularVelocity.dotProduct(car.orientation.roofVector);
	    double rollVel = car.angularVelocity.dotProduct(car.orientation.noseVector);
	    
	    for(int i = 0; i < ticks; i++){
			carVelocity = carVelocity.plus(AirControl.worldUp.scaled(-Constants.GRAVITY * tick)).capMagnitude(Constants.MAXCARSPEED);
			carPosition = carPosition.plus(carVelocity.scaled(tick));
	    }
	    CarOrientation carOrientation = CarOrientation.convert(car.orientation.eularPitch + pitchVel * tick * ticks, car.orientation.eularYaw + yawVel * tick * ticks, car.orientation.eularRoll + rollVel * tick * ticks);
		
		ballPosition = ballPosition.plus(carPosition.minus(ballPosition).scaledToMagnitude(Constants.BALLRADIUS));
		
		Vector3 local = Utils.toLocal(carPosition, carOrientation, ballPosition);
		
		// https://discordapp.com/channels/348658686962696195/348661571297214465/617774276879056919
	    return Math.abs(local.y) - 9 < 127.93 && Math.abs(local.x) < 83.28 && Math.abs(local.z) - 15.75 < 31.30;
	}
	
	private boolean willIntersectNextTick(CarData car, BallPrediction ballPrediction){
		return willIntersectNextTick(car, ballPrediction, 1);
	}

}
