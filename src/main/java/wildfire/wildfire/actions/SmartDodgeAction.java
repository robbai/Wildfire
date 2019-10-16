package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import rlbot.flat.BallPrediction;
import wildfire.input.Rotator;
import wildfire.input.ball.BallData;
import wildfire.input.car.CarData;
import wildfire.input.car.CarOrientation;
import wildfire.input.car.Hitbox;
import wildfire.input.shapes.SphereShape;
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
			Vector3 ballLocation = new Vector3(ballPrediction.slices(i).physics().location());

			double time = (ballPrediction.slices(i).gameSeconds() - input.elapsedSeconds);
			double jumpHeight = ballLocation.minus(input.car.position).dotProduct(input.car.orientation.up);

			OptionalDouble pressOptional = JumpPhysics.getPressForTimeToZ(jumpHeight, time);
			//			if(!pressOptional.isPresent()) continue;
			double press = (pressOptional.isPresent() ? pressOptional.getAsDouble() : JumpPhysics.maxPressTime);

			Vector3 carLocation = JumpPhysics.simCar(input.car, press, time);
			Vector3 displace = ballLocation.minus(carLocation);
			double distance = displace.magnitude();

			// Cone.
			if(coneRequired){
				if(input.car.sign * carLocation.y > Constants.PITCH_LENGTH) continue; // Inside enemy goal.
				Vector2 trace = Utils.traceToY(carLocation.flatten(), ballLocation.minus(carLocation).flatten(), input.car.sign * Constants.PITCH_LENGTH);
				if(trace == null || Math.abs(trace.x) > Constants.GOAL_WIDTH - Constants.BALL_RADIUS) continue;
			}

			//if(distance < Constants.BALL_RADIUS + dodgeDistance){
			double angle = Math.abs(Handling.aim(input.car, ballLocation));
			double hitboxDistance = Utils.lerp(Constants.RIPPER.y / 2 + Constants.RIPPER_OFFSET.y, Constants.RIPPER.x / 2, Math.sin(angle));
			//if(displace.flatten().magnitude() < Constants.BALL_RADIUS + hitboxDistance * 0.9){
			if(distance < Constants.BALL_RADIUS + hitboxDistance * 0.95){
				//			if(distance < Constants.BALL_RADIUS + dodgeDistance){
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
		CarData car = input.car;

		double time = timeDifference(input.elapsedSeconds);

		wildfire.renderer.drawString2d("Press Time: " + Utils.round(this.pressTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);

		ControlsOutput controls = new ControlsOutput().withThrottle(time > this.target.getTime() + 1 ? 1 : 0).withBoost(false).withJump(false);

		wildfire.renderer.drawCrosshair(car, this.target.getPosition(), Color.ORANGE, 60);
		if(time < this.target.getTime()) wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, this.target.getFrame() - (int)(time * 60));

		boolean onPrediction = Behaviour.isOnPrediction(wildfire.ballPrediction, this.target.getPosition());		

		//		if(time < this.pressTime && !intersect){
		if(time < this.pressTime){
			return controls.withJump(true);
		}else{
			if(time > this.target.getTime() + (car.hasDoubleJumped ? 0.7 : 0.4)
					|| (car.hasDoubleJumped && !input.info.isDodgeTorquing())){
				Utils.transferAction(this, new RecoveryAction(this.state, input.elapsedSeconds));
			}

			if(this.angleSet && car.hasDoubleJumped){
				//				controls.withPitch(-Math.cos(angle));
				//				controls.withRoll(-Math.sin(angle));
				return controls;
			}

			boolean intersect = willIntersectNextTick(car, input.ball, wildfire.ballPrediction, 6, true);
			//			boolean intersect = input.ball.position.distance(car.position) < 200;

			if(intersect){
				// Dodge
				controls.withJump(true);
				this.angle = Handling.aim(car, (onPrediction ? this.target.getPosition() : input.ball.position));
				controls.withPitch(-Math.cos(angle));
				controls.withRoll(-Math.sin(angle));

				this.angleSet = true;
			}else{
				double[] airControl = airControl(car, true);
				controls.withPitchYawRoll(airControl);
			}
		}

		return controls;
	}

	/**
	 * Point the car.
	 */
	private double[] airControl(Vector3 carPosition, Vector3 carVelocity, CarOrientation carOrientation, Rotator carAngularVelocity, boolean render){
		if(carVelocity.z > 40){
			Vector3 forward = this.target.getPosition().withZ(this.target.getPosition().z + 100).minus(carPosition).normalised();
			Vector3 roof = forward.scaled(-1).withZ(1 - forward.z);

			if(render){
				wildfire.renderer.drawLine3d(Color.RED, carPosition, carPosition.plus(forward.scaledToMagnitude(200)));
				wildfire.renderer.drawLine3d(Color.GREEN, carPosition, carPosition.plus(roof.scaledToMagnitude(200)));
			}

			double[] angles = AirControl.getPitchYawRoll(carOrientation, carAngularVelocity, forward, roof);
			return angles;
		}
		return new double[3];
	}
	
	private double[] airControl(CarData car, boolean render){
		return airControl(car.position, car.velocity, car.orientation, car.angularVelocity, render);
	}

	@Override
	public boolean expire(InfoPacket input){
		return this.failed || (timeDifference(input.elapsedSeconds) > (input.car.hasWheelContact ? 0.5 : 2.5));
	}

	/*
	 * Based on:
	 * https://github.com/samuelpmish/RLUtilities/blob/master/src/simulation/car.cc#L97
	 * It's important to mention that chip uses forward-left/right-up (roll-pitch-yaw),
	 *  while I use right-forward-up (pitch-roll-yaw), and sometimes pitch-yaw-roll.
	 */
	private boolean willIntersectNextTick(CarData car, BallData initialBall, BallPrediction ballPrediction, int ticks, boolean render){
		CarOrientation carOrientation = car.orientation;
		Vector3 carPosition = car.position, carVelocity = car.velocity, angularVelocityAxis = car.angularVelocityAxis;

		final double dt = 1D / 120;
		
		// Air control torque coefficients.
		final Vector3 T = new Vector3(-130, -400, 95);

		// chip refers to this as "?"
		final double J = 10.5;

		final Vector3 gravity = Vector3.Z.scaled(-Constants.GRAVITY * dt);

		boolean intersects = false, intersectsInTime = false;

		for(int i = 0; i <= Math.max(20, ticks); i++){
			// Car.
			if(i != 0){
				// Controls (pitch-yaw-roll).
				double[] airControl = airControl(car, true);
				Vector3 airControlVec = new Vector3(airControl[0], airControl[2], airControl[1]); // Pitch-roll-yaw.
				
				// Air damping torque coefficients.
				final Vector3 H = new Vector3(-30D * (1D - Math.abs(airControl[0])), -50, -20D * (1D - Math.abs(airControl[1])));

				carVelocity = carVelocity.plus(gravity).capMagnitude(Constants.MAX_CAR_VELOCITY);
				carPosition = carPosition.plus(carVelocity.scaled(dt));

				Vector3 angularVelocityAxisLocal = Utils.toLocalFromRelative(carOrientation, angularVelocityAxis);
				angularVelocityAxis = angularVelocityAxis.plus(Utils.toGlobal(new Vector3(), carOrientation, angularVelocityAxisLocal.multiply(H).plus(T.multiply(airControlVec))).scaled(dt / J));
				carOrientation = carOrientation.step(dt, angularVelocityAxis);
			}
			//			if(render) wildfire.renderer.drawRipperHitbox(carPosition, carOrientation, i <= ticks ? Color.WHITE : Color.GRAY);
			if(render) wildfire.renderer.drawHitbox(carPosition, carOrientation, car.hitbox, intersects ? Color.RED : Color.WHITE);
			Hitbox carHitbox = new Hitbox(car.hitbox, carPosition, carOrientation);

			// Ball.
			Vector3 ballPosition;
			double ballTick = ((double)i / ((1D / 60) / dt) - 1);
			if(ballTick >= 0){
				Vector3 ballA = new Vector3(ballPrediction.slices((int)Math.floor(ballTick)).physics().location());
				Vector3 ballB = new Vector3(ballPrediction.slices((int)Math.floor(ballTick) + 1).physics().location());
				ballPosition = ballA.lerp(ballB, ballTick - Math.floor(ballTick));
			}else{
				ballPosition = initialBall.position;
			}
			if(render) wildfire.renderer.drawCrosshair(car, ballPosition, Color.BLUE, Constants.BALL_RADIUS * 2);
			SphereShape ball = new SphereShape(ballPosition, Constants.BALL_RADIUS);

			if(carHitbox.intersects(ball)){
				intersects = true;
				if(i <= ticks) intersectsInTime = true;
				//				break;
			}
		}

		return intersectsInTime;
	}

}
