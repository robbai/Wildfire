package wildfire.wildfire.mechanics;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Mechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Utils;

public class FollowDiscreteMechanic extends Mechanic {

	public final static double steerLookahead = 0.328, speedLookahead = (1D / 60);
	private final static boolean verboseRender = true;

	private DiscreteCurve curve;
	private boolean dodge;
	private OptionalDouble targetTime;
	public boolean renderPredictionToTargetTime = false;
	public boolean linearTarget = false;

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
		Vector2 target = getTarget(carS, initialVelocity);
		double targetVelocity = curve.getSpeed(Utils.clamp((carS + initialVelocity * speedLookahead) / curve.getDistance(), 0, 1));
		
		double targetAcceleration = (targetVelocity - initialVelocity) / 0.05;
		if(this.targetTime.isPresent()){
			double targetTimeLeft = (this.targetTime.getAsDouble() - timeElapsed);
			
			if(renderPredictionToTargetTime){
				int endFrame = (int)Utils.clamp(Math.ceil(targetTimeLeft * 60), 0, wildfire.ballPrediction.slicesLength());
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.WHITE, 0, endFrame);
				Vector3 slicePosition = new Vector3(wildfire.ballPrediction.slices(endFrame).physics().location());
				if(slicePosition != null){
					wildfire.renderer.drawCrosshair(input.car, slicePosition, Color.GRAY, 80);
					Vector2 end = curve.T(1);
					if(end != null) wildfire.renderer.drawCrosshair(input.car, end.withZ(slicePosition.z), Color.BLACK, 40);
				}
			}
			
			if(linearTarget){
				targetAcceleration = ((curve.getDistance() - carS) / targetTimeLeft - initialVelocity) / 0.05; // Enforce!
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
		
		if(Math.abs(radians) < 0.3 && targetAcceleration > 0 && dodge){
			// Low time results in a chip shot, high time results in a low shot
			boolean dodgeNow = (updatedTimeLeft < Behaviour.IMPACT_DODGE_TIME - 0.05);
			if(dodgeNow){
//				double endRadians = Handling.aim(input.car, curve.T(1));
				return this.startAction(new DodgeAction(this.state, input.info.impactRadians * 3, input), input);
			}
		}
		
		double throttle = Handling.produceAcceleration(input.car, targetAcceleration);
		return Handling.forwardDrive(input.car, target).withSlide(false)
//		return new ControlsOutput().withSteer(-Math.signum(radians))
				.withThrottle(throttle)
				.withBoost((throttle > 1 /**|| (input.car.isSupersonic && targetAcceleration > 0)*/) && input.car.hasWheelContact);
	}

	private Vector2 getTarget(double carS, double initialVelocity){
		return curve.S(Math.min(curve.getDistance() - 1, carS + Math.max(500, initialVelocity) * steerLookahead));
	}

	@Override
	public boolean expire(InfoPacket input){
		CarData car = input.car;
		
		double distanceError = curve.findClosestS(car.position.flatten(), true);
		if(distanceError > 80) return true;
		
		double carS = curve.findClosestS(car.position.flatten(), false);
//		return (carS + Math.abs(car.forwardVelocity) * steerLookahead / 8) / curve.getDistance() >= 1;
		return getTarget(carS, car.forwardVelocityAbs) == null;
	}

}
