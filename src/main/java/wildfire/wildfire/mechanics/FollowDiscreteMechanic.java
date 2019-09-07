package wildfire.wildfire.mechanics;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Mechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Utils;

public class FollowDiscreteMechanic extends Mechanic {

	public final static double steerLookahead = 0.312, speedLookahead = (1D / 60);
	private final static boolean verboseRender = true;

	private DiscreteCurve curve;
	private boolean dodge;
	private OptionalDouble targetTime;
	public boolean linearTargetTime;

	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted, boolean dodge, OptionalDouble targetTime){
		super("Follow Discrete", state, timeStarted);
		this.curve = curve;
		this.dodge = dodge;
		this.targetTime = (!targetTime.isPresent()/** || targetTime.getAsDouble() < curve.getTime()*/ ? OptionalDouble.empty() : targetTime);
	}

	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted, OptionalDouble targetTime){
		this(state, curve, timeStarted, false, targetTime);
	}
	
	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted){
		this(state, curve, timeStarted, false, OptionalDouble.empty());
	}
	
	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted, boolean dodge){
		this(state, curve, timeStarted, dodge, OptionalDouble.empty());
	}
	
	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted, double targetTime){
		this(state, curve, timeStarted, false, OptionalDouble.of(targetTime));
	}
	
	public FollowDiscreteMechanic(State state, DiscreteCurve curve, double timeStarted, boolean dodge, double targetTime){
		this(state, curve, timeStarted, dodge, OptionalDouble.of(targetTime));
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		// Target and acceleration.
		double carS = curve.findClosestS(input.car.position.flatten(), false);
		double initialVelocity = input.car.forwardVelocityAbs;
		double timeElapsed = this.timeDifference(input.elapsedSeconds);
		double guessedTimeLeft = (curve.getTime() - timeElapsed);
		double updatedTimeLeft = (curve.getTime() * (1 - carS / curve.getDistance()));
		Vector2 target = curve.S(Math.min(curve.getDistance(), carS + Math.max(400, initialVelocity) * steerLookahead));
		double targetVelocity = curve.getSpeed(Utils.clamp((carS + initialVelocity * speedLookahead) / curve.getDistance(), 0, 1));
		
//		if(this.targetTime.isPresent()) targetVelocity = Math.min(targetVelocity, (curve.getDistance() - carS) / (this.targetTime.getAsDouble() - timeElapsed));
//		if(this.targetTime.isPresent() && updatedTimeLeft < this.targetTime.getAsDouble() - timeElapsed) targetVelocity = ((curve.getDistance() - carS) / (this.targetTime.getAsDouble() - timeElapsed));
		double targetAcceleration = (targetVelocity - initialVelocity) / 0.05;
		if(this.targetTime.isPresent()){
			double targetTimeLeft = (this.targetTime.getAsDouble() - timeElapsed);
			if(this.linearTargetTime){
				targetAcceleration = (((curve.getDistance() - carS) / targetTimeLeft) - initialVelocity) / 0.05;
			}else{	
				double arrivalAcceleration = ((2 * (curve.getDistance() - carS - targetTimeLeft * initialVelocity)) / Math.pow(targetTimeLeft, 2));		
				targetAcceleration = Math.min(targetAcceleration, arrivalAcceleration);
			}
		}

		// Render.
		wildfire.renderer.drawString2d("Distance: " + (int)curve.getDistance() + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		if(!this.targetTime.isPresent()){
			wildfire.renderer.drawString2d("Est Time: " + Utils.round(updatedTimeLeft) + "s (" + (guessedTimeLeft < updatedTimeLeft ? "+" : "") + Utils.round(updatedTimeLeft - guessedTimeLeft) + "s)", Color.WHITE, new Point(0, 60), 2, 2);
		}else{
			wildfire.renderer.drawString2d("Est Time: " + Utils.round(updatedTimeLeft) + "s (Want: " + Utils.round(this.targetTime.getAsDouble() - timeElapsed) + "s)", Color.WHITE, new Point(0, 60), 2, 2);
		}
		if(verboseRender){
			wildfire.renderer.drawString2d("Current Vel.: " + (int)initialVelocity + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			wildfire.renderer.drawString2d("Target Vel.: " + (int)targetVelocity + "uu/s", Color.WHITE, new Point(0, 100), 2, 2);
			wildfire.renderer.drawString2d("Target Acc.: " + (int)targetAcceleration + "uu/s^2", Color.WHITE, new Point(0, 120), 2, 2);
		}
		curve.render(wildfire.renderer, Color.BLUE);
		wildfire.renderer.drawCircle(Color.CYAN, target, 10);
		wildfire.renderer.drawCircle(Color.RED, curve.S(Math.min(curve.getDistance(), carS + initialVelocity * speedLookahead)), 5);

		/*
		 *  Handling.
		 */
		double radians = Handling.aim(input.car, target);
		
		if(Math.abs(radians) < 0.4 && dodge){
			// Low time results in a chip shot, high time results in a low shot
			boolean dodgeNow = (updatedTimeLeft < 0.32);
			if(dodgeNow){
				double endRadians = Handling.aim(input.car, curve.T(1));
				return this.startAction(new DodgeAction(this.state, endRadians, input), input);
			}
		}
		
		double throttle = Handling.produceAcceleration(input.car, targetAcceleration);
		return new ControlsOutput()
				.withSteer(radians * -5)
				.withThrottle(throttle)
				.withBoost((throttle > 1 /**|| (input.car.isSupersonic && targetAcceleration > 0)*/) && input.car.hasWheelContact);
	}

	@Override
	public boolean expire(InfoPacket input){
		CarData car = input.car;
		
		double distance = curve.findClosestS(car.position.flatten(), true);
//		System.out.println(distance);
		if(distance > 70) return true;
		
		double carS = curve.findClosestS(car.position.flatten(), false);
		return (carS + Math.abs(car.forwardVelocity) * steerLookahead / (dodge ? 8 : 4)) / curve.getDistance() >= 1;
	}

}
