package wildfire.wildfire.mechanics;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Mechanic;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class FollowSmartDodgeMechanic extends Mechanic {
	
	// TODO extend FollowDiscreteMechanic
	
	public final static double steerLookahead = 0.312, speedLookahead = (1D / 60);

	private DiscreteCurve curve;

	private Slice candidate;

	public FollowSmartDodgeMechanic(State state, DiscreteCurve curve, double timeStarted, Slice candidate){
		super("Follow Smart-Dodge", state, timeStarted);
		this.curve = curve;
		this.candidate = candidate;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		// Target and acceleration.
		double carS = curve.findClosestS(input.car.position.flatten(), false);
		double initialVelocity = input.car.forwardVelocityAbs;
		double timeElapsed = this.timeDifference(input.elapsedSeconds);
//		double timeLeft = (curve.getTime() * (1 - carS / curve.getDistance()));
		Vector2 target = curve.S(Math.min(curve.getDistance(), carS + Math.max(400, initialVelocity) * steerLookahead));
		double targetVelocity = curve.getSpeed(Utils.clamp((carS + initialVelocity * speedLookahead) / curve.getDistance(), 0, 1));	
		double targetAcceleration = (targetVelocity - initialVelocity) / 0.05;
				
		// Jump calculations.
		double peakTime = JumpPhysics.getFastestTimeZ(candidate.getPosition().minus(input.car.position).dotProduct(input.car.orientation.up));
		double driveTime = (candidate.getTime() - peakTime - timeElapsed - 1D / 120);
		double jumpVelocity = (2 * (curve.getDistance() - carS) - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
//		double jumpDistance = (jumpVelocity * peakTime);
		
		// Manipulate the target timing to jump in time.
		double jumpWeighting;
		if(input.car.orientation.forward.dotProduct(curve.getDestination().withZ(Constants.RIPPER_RESTING).minus(input.car.position)) < 0){
			jumpWeighting = 0;
		}else{
			double jumpAcceleration = ((jumpVelocity - initialVelocity) / driveTime);
			jumpWeighting = Utils.clamp(1.1 / driveTime, 0, 1);
			targetAcceleration = Utils.lerp(Math.min(targetAcceleration, jumpAcceleration), jumpAcceleration, jumpWeighting);
		}
		
		// Render.
		wildfire.renderer.drawString2d("Drive Time: " + Utils.round(driveTime) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Jump Weighting: " + (int)(jumpWeighting * 100) + "%", Color.WHITE, new Point(0, 60), 2, 2);
		wildfire.renderer.drawString2d("Target Acc.: " + (int)targetAcceleration + "uu/s^2", Color.WHITE, new Point(0, 80), 2, 2);
		curve.render(wildfire.renderer, Color.YELLOW);
		wildfire.renderer.drawCircle(Color.CYAN, target, 10);
		wildfire.renderer.drawCircle(Color.RED, curve.S(Math.min(curve.getDistance(), carS + initialVelocity * speedLookahead)), 5);
		wildfire.renderer.drawCrosshair(input.car, this.candidate.getPosition(), Color.RED, 80);

		// Smart-dodge.
		SmartDodgeAction smartDodge = new SmartDodgeAction(this.state, input, false);
		if(!smartDodge.failed){
			return this.startAction(smartDodge, input);
		}

		/*
		 *  Handling.
		 */
		double throttle = Handling.produceAcceleration(input.car, targetAcceleration);
		return Handling.forwardDrive(input.car, target).withSlide(false)
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
		return (carS + Math.abs(car.forwardVelocity) * steerLookahead / 4) / curve.getDistance() >= 1;
	}

}
