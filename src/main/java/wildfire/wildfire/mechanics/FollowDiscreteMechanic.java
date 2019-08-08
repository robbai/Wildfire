package wildfire.wildfire.mechanics;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.obj.Mechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class FollowDiscreteMechanic extends Mechanic {

	private final static double steerLookahead = 0.345, speedLookahead = (1D / 60);
	private final static boolean verboseRender = true;

	private DiscreteCurve curve;
	private boolean dodge;

	public FollowDiscreteMechanic(State state, DiscreteCurve curve, float time, boolean dodge){
		super("Follow Discrete", state, time);
		this.curve = curve;
		this.dodge = dodge;
	}
	
	public FollowDiscreteMechanic(State state, DiscreteCurve curve, float time){
		this(state, curve, time, false);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		// Target and acceleration.
		double carS = curve.findClosestS(input.car.position.flatten(), false);
		double initialVelocity = Math.abs(input.car.forwardVelocity);
		double timeLeft = (curve.getTime() - this.timeDifference(input.elapsedSeconds));
		Vector2 target = curve.S(Math.min(curve.getDistance(), carS + Math.max(700, initialVelocity) * steerLookahead));
//		double targetAcceleration = curve.getAcceleration(Utils.clamp((carS + initialVelocity * speedLookahead) / curve.getDistance(), 0, 1));
		double targetVelocity = curve.getSpeed(Utils.clamp((carS + initialVelocity * speedLookahead) / curve.getDistance(), 0, 1));
		double targetAcceleration = (targetVelocity - initialVelocity) * 60;

		// Render.
		wildfire.renderer.drawString2d("Distance: " + (int)curve.getDistance() + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Time Left: " + Utils.round(timeLeft) + "s", Color.WHITE, new Point(0, 60), 2, 2);
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
			boolean dodgeNow = (timeLeft < 0.082);
			if(dodgeNow){
				double endRadians = Handling.aim(input.car, curve.T(1));
				return this.startAction(new DodgeAction(this.state, endRadians * 2.5, input), input);
			}
		}
		
		double throttle = Handling.produceAcceleration(input.car, targetAcceleration);
		return new ControlsOutput()
				.withSteer(radians * -4)
				.withThrottle(throttle)
				.withBoost((throttle > 1 /**|| (input.car.isSupersonic && targetAcceleration > 0)*/) && input.car.hasWheelContact);
	}

	@Override
	public boolean expire(DataPacket input){
		CarData car = input.car;
		
		double distance = curve.findClosestS(car.position.flatten(), true);
//		System.out.println(distance);
		if(distance > 70) return true;
		
		double carS = curve.findClosestS(car.position.flatten(), false);
		return (carS + Math.abs(car.forwardVelocity) * steerLookahead / 4) / curve.getDistance() >= 1 || car.velocity.magnitude() < 1;
	}

}
