package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class AerialAction extends Action {
	
//	private final double boostThreshold = 100, jumpHeight = 230.76923076923077D, doubleJumpHeight = 298.6575012207031D;
			
	private final double maxJumpVelocity = 547.7225575D, jumpTime = 0.2 + (1D / 60), maxDoubleJumpVelocity = 623.100916053663D;
	
	private PID rollPID, pitchPID, yawPID;

	private Vector3 target;
	private double time;
	private boolean doubleJump;
	public double averageAcceleration;

	public AerialAction(State state, CarData car, Vector3 target, double time, boolean doubleJump){
		super("Aerial-2", state, car.elapsedSeconds);
		
		this.failed = !isAerialPossible(car, target, time);
		
		if(!this.failed){
			this.doubleJump = doubleJump;
			this.target = target;
			this.time = time;
			
			this.pitchPID = new PID(wildfire.renderer, Color.BLUE, 6.5, 0, 1.05).withRender(true);
			this.yawPID = new PID(wildfire.renderer, Color.RED, 5.8, 0, 1).withRender(true);
			this.rollPID = new PID(wildfire.renderer, Color.YELLOW, 3, 0, 0.4).withRender(true);
		}
	}
	
	public static AerialAction fromBallPrediction(State state, CarData car, BallPrediction ballPrediction, boolean forceDoubleJump){
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			if(location.isOutOfBounds()) continue;
			
			location = location.plus(car.position.minus(location).scaledToMagnitude(Constants.BALLRADIUS / 2));
//			location = location.plus(location.minus(Utils.enemyGoal(car).withZ(Utils.BALLRADIUS)).scaledToMagnitude(140));
			
			//Double jumping
			AerialAction a;
			if(!forceDoubleJump){
				a = new AerialAction(state, car, location, (double)i / 60, false);
				if(!a.failed) return a;
			}
			a = new AerialAction(state, car, location, (double)i / 60, true);
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
			AerialAction a = fromBallPrediction(this.state, input.car, this.wildfire.ballPrediction, false);
			Utils.transferAction(this, (a != null && !a.failed ? a : new RecoveryAction(this.state, input.elapsedSeconds)));
		}
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s, Double Jump: " + this.doubleJump, Color.WHITE, new Point(0, 40), 2, 2);
		
		//Draw the crosshair
		wildfire.renderer.drawCrosshair(input.car, target, Color.WHITE, 200);		
		
		//Jump
		if((timeDifference < jumpTime && input.car.hasWheelContact) || (timeDifference > 0.22 && this.doubleJump && !input.car.doubleJumped) || (target.distance(input.car.position) < 800 && !input.car.doubleJumped)){
			controls.withJump(true);
//			return controls;
		}
		
		//Point towards the target, and stop boosting
		Vector3 connection = renderFall(Color.ORANGE, input.car.position, input.car.velocity, timeDifference);
		if(connection != null){
			wildfire.renderer.drawCrosshair(input.car, connection, Color.YELLOW, 150);
			Vector3 targetDirLocal = Utils.toLocal(input.car, target).normalized();
//			double roll = rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, Math.PI);
			double roll = 0;
			double pitch = pitchPID.getOutput(input.elapsedSeconds, 0, Math.asin(targetDirLocal.z));
			double yaw = yawPID.getOutput(input.elapsedSeconds, 0, Math.asin(targetDirLocal.x));
			return controls.withPitch((float)pitch).withYaw((float)yaw).withRoll((float)roll);
		}
		
		//Calculate the direction
		Vector3 s = target.minus(input.car.position);
		Vector3 u = input.car.velocity;
		Vector3 acc = new Vector3(acceleration(s.x, u.x, timeLeft), acceleration(s.y, u.y, timeLeft), accelerationGravity(s.z, u.z, timeLeft));
		Vector3 dir = acc.normalized();
		Vector3 dirLocal = Utils.toLocalFromRelative(input.car, dir);
		
		double accelerationReq = acc.magnitude();
		double accelerationReqForwards = accelerationReq * dirLocal.y;
		
		wildfire.renderer.drawString2d("Avg. Acceleration: " + (int)averageAcceleration + "uu/s^2", Color.WHITE, new Point(0, 60), 2, 2);
		wildfire.renderer.drawString2d("Acceleration Req.: " + (int)accelerationReq + "uu/s^2", Color.WHITE, new Point(0, 80), 2, 2);
		
		double roll = rollPID.getOutput(input.elapsedSeconds, input.car.orientation.eularRoll, 0);
//		double roll = (accelerationReq < 900 ? 1 : 0);
//		double roll = 0;
		
		double dirPitch = Math.asin(dirLocal.z);
		double pitch = pitchPID.getOutput(input.elapsedSeconds, 0, dirPitch);
	
		double yawCorrection = Math.asin(dirLocal.x);
		double yaw = yawPID.getOutput(input.elapsedSeconds, 0, yawCorrection);
		
		if(timeDifference < jumpTime || !controls.holdJump()){
			controls.withPitch((float)pitch);
			controls.withYaw((float)yaw);
			controls.withRoll((float)roll);
		}else{
			controls.withPitch(0);
			controls.withYaw(0);
			controls.withRoll(0);
		}
		
		//Boost
		int boostThreshold = (input.car.position.z < 500 ? 0 : 220);
		controls.withBoost(Math.abs(yawCorrection) < 1.5 && (dir.z > 0 == input.car.orientation.noseVector.z > 0) && accelerationReqForwards > boostThreshold);
		
		return controls;
	}

	@Override
	public boolean expire(DataPacket input){
		return this.failed || input.car.hasWheelContact && timeDifference(input.elapsedSeconds) > 0.75;
	}
	
	private boolean isAerialPossible(CarData car, Vector3 target, double t){
		Vector3 carPosition = car.position;
		Vector3 s = target.minus(carPosition);
		
		Vector3 u = car.velocity;
		if(car.hasWheelContact){
			//Jump
			u = u.plus(car.orientation.roofVector.scaledToMagnitude(doubleJump ? maxDoubleJumpVelocity : maxJumpVelocity));
			u = u.capMagnitude(2300);
		}
		
		//Compensate for turning by reducing the time we have left
//		Vector3 generalDirection = new Vector3(acceleration(s.x, u.x, 1), acceleration(s.y, u.y, 1), accelerationGravity(s.z, u.z, 1)).normalized();
//		double angleDifference = car.orientation.noseVector.minus(generalDirection).withZ(0).magnitude();
//		double angleTime = Math.min(1, angleDifference * 1.4);
//		System.out.println("Angular time: " + Utils.round(angleTime) + "s");
//		t -= angleTime;
		t -= 0.25;
		
		Vector3 a = new Vector3(acceleration(s.x, u.x, t), acceleration(s.y, u.y, t), accelerationGravity(s.z, u.z, t));
		averageAcceleration = a.magnitude();
		if(averageAcceleration > Constants.BOOSTACC) return false;
		
		double boostRequired = ((averageAcceleration * t) / Constants.BOOSTACC * (100D / 3));
			
//		System.out.println("Boost required for aerial: " + (int)boostRequired + ", current boost: " + (wildfire.unlimitedBoost ? "unlimited" : (int)car.boost));		
		return wildfire.unlimitedBoost || car.boost >= boostRequired;
	}
	
	private double acceleration(double s, double u, double t){
		return (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private double accelerationGravity(double s, double u, double t){
		return Constants.GRAVITY + (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private final double renderScale = (1D / 50);
	private Vector3 renderFall(Color colour, Vector3 start, Vector3 velocity, double t){
		if(start.isOutOfBounds()) return null;
		if(start.distance(target) < 40 && Math.abs(t - time) < renderScale) return start;
		
		velocity = velocity.plus(new Vector3(0, 0, -Constants.GRAVITY * renderScale)); //Gravity
		velocity = velocity.capMagnitude(2300);
		
		Vector3 next = start.plus(velocity.scaled(renderScale));
		wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
		return renderFall(colour, next, velocity, t + renderScale);
	}

}