package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;

public class AerialAction2 extends Action {
	
//	private final double boostThreshold = 100, jumpHeight = 230.76923076923077D, doubleJumpHeight = 298.6575012207031D;
			
	private final double maxJumpVelocity = 547.7225575D, jumpTime = 0.2 + (1D / 60), maxDoubleJumpVelocity = 623.100916053663D;
	
	private PID rollPID, pitchPID, yawPID;

	private Vector3 target;
	private double time;
	private boolean doubleJump;
	public double averageAcceleration;

	public AerialAction2(State state, CarData car, Vector3 target, double time, boolean doubleJump){
		super("Aerial-2", state, car.elapsedSeconds);
		
		this.failed = !isAerialPossible(car, target, time);
		
		if(!this.failed){
			this.doubleJump = doubleJump;
			this.target = target;
			this.time = time;
			
			this.pitchPID = new PID(wildfire.renderer, Color.BLUE, 6.5, 0, 1).withRender(true);
			this.yawPID = new PID(wildfire.renderer, Color.RED, 5.8, 0, 1).withRender(true);
			this.rollPID = new PID(wildfire.renderer, Color.YELLOW, 3, 0, 0.4).withRender(true);
		}
	}
	
	public static AerialAction2 fromBallPrediction(State state, CarData car, BallPrediction ballPrediction, boolean doubleJump){
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
//		for(int i = (ballPrediction.slicesLength() - 1); i > -1; i--){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			if(location.isOutOfBounds()) continue;
			location = location.plus(car.position.minus(location).scaledToMagnitude(Utils.BALLRADIUS));
			
			AerialAction2 a = new AerialAction2(state, car, location, (double)i / 60, doubleJump);
			if(!a.failed) return a;
		}
		return null;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		ControlsOutput controls = new ControlsOutput().withNone();
		float timeDifference = timeDifference(input.elapsedSeconds);
		
		double timeLeft = this.time - timeDifference;
		if(timeLeft < 0){
//			AerialAction2 doubleTouch = (input.ball.position.distance(input.car.position) < 400 ? null : AerialAction2.fromBallPrediction(this.state, input.car, wildfire.ballPrediction));
			AerialAction2 doubleTouch = null;
			Utils.transferAction(this, doubleTouch != null ? doubleTouch : new RecoveryAction(this.state, input.elapsedSeconds));
		}
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s, Double Jump: " + this.doubleJump, Color.WHITE, new Point(0, 40), 2, 2);
		
		//Draw the crosshair
		wildfire.renderer.drawCrosshair(input.car, target, Color.WHITE, 200);		
		
		//Jump
		if((timeDifference < jumpTime && input.car.hasWheelContact) || (timeDifference > 0.26 && this.doubleJump && !input.car.doubleJumped)){
			controls.withJump(true);
//			return controls;
		}
		
		//Point towards the target
		Vector3 connection = renderFall(Color.ORANGE, input.car.position, input.car.velocity, timeDifference);
		if(connection != null){
			wildfire.renderer.drawCrosshair(input.car, connection, Color.YELLOW, 150);
			double angularCoefficient = Math.signum(Math.cos(input.car.orientation.eularRoll));
			double roll = rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, 0);			
			double pitch = angularCoefficient * pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.eularPitch, Math.asin(target.minus(input.car.position).normalized().z));
			double yawCorrection = -input.car.orientation.noseVector.flatten().correctionAngle(target.minus(input.car.position).flatten());
			double yaw = angularCoefficient * yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
			return controls.withPitch((float)pitch).withYaw((float)yaw).withRoll((float)roll);
		}
		
		Vector3 s = target.minus(input.car.position);
		Vector3 u = input.car.velocity;
		
		double dirX = acceleration(s.x, u.x, timeLeft);
		double dirY = acceleration(s.y, u.y, timeLeft);
		double dirZ = accelerationGravity(s.z, u.z, timeLeft);
		Vector3 acc = new Vector3(dirX, dirY, dirZ);
		Vector3 dir = acc.normalized();
		
		double accelerationReq = acc.magnitude();
		double accelerationForwards = Math.cos(input.car.orientation.noseVector.flatten().correctionAngle(acc.flatten()));
		
		wildfire.renderer.drawString2d("Avg. Acceleration: " + (int)averageAcceleration + "uu/s^2", Color.WHITE, new Point(0, 60), 2, 2);
		wildfire.renderer.drawString2d("Acceleration Req.: " + (int)accelerationReq + "uu/s^2", Color.WHITE, new Point(0, 80), 2, 2);
		
		//Line rendering
//		final double lineRenderLength = 600;
//		wildfire.renderer.drawLine3d(Color.GREEN, input.car.position.toFramework(), input.car.position.plus(dir.scaledToMagnitude(lineRenderLength)).toFramework());
//		wildfire.renderer.drawLine3d(Color.BLUE, input.car.position.toFramework(), input.car.position.plus(input.car.orientation.noseVector.scaledToMagnitude(lineRenderLength)).toFramework());
//		wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.toFramework(), input.car.position.plus(target.minus(input.car.position).scaledToMagnitude(lineRenderLength)).toFramework());
				
		//For handling the car when upside down
		double angularCoefficient = Math.signum(Math.cos(input.car.orientation.eularRoll));
		
		double roll = rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, 0);
		
		double dirPitch = Math.asin(dir.z);
		double pitch = angularCoefficient * pitchPID.getOutput(input.elapsedSeconds, input.car.orientation.eularPitch, dirPitch);
	
		double yawCorrection = -input.car.orientation.noseVector.flatten().correctionAngle(dir.flatten());
		double yaw = angularCoefficient * yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
		
		if(timeDifference < jumpTime || !controls.holdJump()){
			controls.withPitch((float)pitch);
			controls.withYaw((float)yaw);
			controls.withRoll((float)roll);
		}
		
		//Boost
		controls.withBoost(Math.abs(pitch) + Math.abs(yaw) < 1.8 && accelerationForwards > 0);
		
		return controls;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 1;
	}
	
	private boolean isAerialPossible(CarData car, Vector3 target, double t){
		Vector3 carPosition = car.position;
//		if(car.hasWheelContact){
//			//Jump
//			carPosition = carPosition.plus(car.orientation.roofVector.scaledToMagnitude(jumpHeight));
//		}
			
		Vector3 s = target.minus(carPosition);
		
		Vector3 u = car.velocity;
		if(car.hasWheelContact){
			//Jump
			u = u.plus(car.orientation.roofVector.scaledToMagnitude(doubleJump ? maxDoubleJumpVelocity : maxJumpVelocity));
			u = Utils.capMagnitude(u, 2300);
		}
		
		t -= jumpTime;
		
		double aX = acceleration(s.x, u.x, t);
		double aY = acceleration(s.y, u.y, t);
		double aZ = accelerationGravity(s.z, u.z, t);
		Vector3 a = new Vector3(aX, aY, aZ);
		averageAcceleration = a.magnitude();
		if(averageAcceleration > Utils.BOOSTACC) return false;
		
		double boostRequired = ((averageAcceleration * t) / Utils.BOOSTACC * (100D / 3));
				
		if(wildfire.unlimitedBoost){
			System.out.println("Boost required for aerial: " + (int)boostRequired + ", current boost: unlimited");
			return true;
		}	
		System.out.println("Boost required for aerial: " + (int)boostRequired + ", current boost: " + (int)car.boost);		
		return car.boost >= boostRequired;
	}
	
	private double acceleration(double s, double u, double t){
		return (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private double accelerationGravity(double s, double u, double t){
		return Utils.GRAVITY + (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private final double renderScale = (1D / 50);
	private Vector3 renderFall(Color colour, Vector3 start, Vector3 velocity, double t){
		if(start.isOutOfBounds()) return null;
		if(start.distance(target) < 38 && Math.abs(t - time) < renderScale) return start;
		
		velocity = velocity.plus(new Vector3(0, 0, -Utils.GRAVITY * renderScale)); //Gravity
		velocity = Utils.capMagnitude(velocity, 2300);
		
		Vector3 next = start.plus(velocity.scaled(renderScale));
		wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
		return renderFall(colour, next, velocity, t + renderScale);
	}

}
