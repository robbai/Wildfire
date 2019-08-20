package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.handling.AirControl;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class AerialAction extends Action {
			
	private final double maxJumpVelocity = 547.7225575D, jumpTime = 0.2 + (1D / 60), maxDoubleJumpVelocity = 623.100916053663D, angleBoostThreshold = 0.7;

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
	public ControlsOutput getOutput(InfoPacket input){
		ControlsOutput controls = new ControlsOutput().withNone();
		float timeDifference = timeDifference(input.elapsedSeconds);
		
		double timeLeft = this.time - timeDifference;
		if(timeLeft < 0){
			AerialAction a = fromBallPrediction(this.state, input.car, wildfire.ballPrediction, false);
			Utils.transferAction(this, (a != null && !a.failed ? a : new RecoveryAction(this.state, input.elapsedSeconds)));
		}
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s, Double Jump: " + this.doubleJump, Color.WHITE, new Point(0, 40), 2, 2);
		
		// Draw the crosshair.
		wildfire.renderer.drawCrosshair(input.car, target, Color.WHITE, 200);		
		
		// Jump.
		if((timeDifference < jumpTime && input.car.hasWheelContact) || (timeDifference > 0.22 && this.doubleJump && !input.car.doubleJumped)){
			controls.withJump(true);
//			return controls;
		}
		
		// Point towards the target, and stop boosting.
		Vector3 connection = renderFall(wildfire.renderer, Color.ORANGE, input.car.position, input.car.velocity, timeDifference);
		if(connection != null){
			wildfire.renderer.drawCrosshair(input.car, connection, Color.YELLOW, 150);
			
			double[] angles = AirControl.getPitchYawRoll(input.car, target.minus(input.car.position));
			
			return controls.withJump(true).withPitchYawRoll(angles);
		}
		
		// Calculate the direction.
		Vector3 s = target.minus(input.car.position);
		Vector3 u = input.car.velocity;
		Vector3 acc = new Vector3(acceleration(s.x, u.x, timeLeft), acceleration(s.y, u.y, timeLeft), accelerationGravity(s.z, u.z, timeLeft));
//		Vector3 localDirection = Utils.toLocalFromRelative(input.car, acc).normalized();
//		Vector2 angles = Handling.getAngles(localDirection);
		
		double accelerationReq = acc.magnitude();
		double accelerationReqForwards = acc.dotProduct(input.car.orientation.noseVector);
		
		wildfire.renderer.drawString2d("Avg. Acceleration: " + (int)averageAcceleration + "uu/s^2", Color.WHITE, new Point(0, 60), 2, 2);
		wildfire.renderer.drawString2d("Acceleration Req.: " + (int)accelerationReq + "uu/s^2", Color.WHITE, new Point(0, 80), 2, 2);
		
		double[] angles = AirControl.getPitchYawRoll(input.car, acc);
		
		if(!controls.holdJump()){
			controls.withPitchYawRoll(angles);
		}else{
			controls.withPitch(0);
			controls.withYaw(0);
			controls.withRoll(0);
		}
		
		//Boost
		double boostThreshold = (input.car.position.z < 500 ? 20 : 70);
		controls.withBoost(input.car.orientation.noseVector.normalized().dotProduct(acc.normalized()) > angleBoostThreshold && accelerationReqForwards > boostThreshold);
		
		return controls;
	}

	@Override
	public boolean expire(InfoPacket input){
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
		Vector3 generalDirection = new Vector3(acceleration(s.x, u.x, 1), acceleration(s.y, u.y, 1), accelerationGravity(s.z, u.z, 1)).normalized();
		double angleDifference = (2 - car.orientation.noseVector.dotProduct(generalDirection)) / 2;
		double angleTime = (angleDifference * 1.05);
//		System.out.println("Angular time: " + Utils.round(angleTime) + "s [" + Utils.round(car.orientation.noseVector.dotProduct(generalDirection)) + "]");
		t -= angleTime;
//		t -= 0.4;
		
		Vector3 a = new Vector3(acceleration(s.x, u.x, t), acceleration(s.y, u.y, t), accelerationGravity(s.z, u.z, t));
		averageAcceleration = a.magnitude();
		if(averageAcceleration > Constants.BOOSTACC) return false;
		
		double boostRequired = getBoostRequired(averageAcceleration, t);
			
//		System.out.println("Boost required for aerial: " + (int)boostRequired + ", current boost: " + (wildfire.unlimitedBoost ? "unlimited" : (int)car.boost));		
		return wildfire.unlimitedBoost || car.boost >= boostRequired;
	}
	
	private static double getBoostRequired(double averageAcceleration, double time){
		return (averageAcceleration * time) / Constants.BOOSTACC * (100D / 3);
	}

	private double acceleration(double s, double u, double t){
		return (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private double accelerationGravity(double s, double u, double t){
		return Constants.GRAVITY + (2 * (s - t * u)) / Math.pow(t, 2);
	}
	
	private final double renderScale = (1D / 50);
	private Vector3 renderFall(WRenderer renderer, Color colour, Vector3 start, Vector3 velocity, double t){
		if(start.z < 0) return null;
		if(start.distance(target) < 40 && Math.abs(t - time) < renderScale * 2) return start;
		
		velocity = velocity.plus(new Vector3(0, 0, -Constants.GRAVITY * renderScale)); //Gravity
		velocity = velocity.capMagnitude(2300);
		
		Vector3 next = start.plus(velocity.scaled(renderScale));
		renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
		return renderFall(renderer, colour, next, velocity, t + renderScale);
	}

}