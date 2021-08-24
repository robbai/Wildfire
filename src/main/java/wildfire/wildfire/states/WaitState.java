package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.curve.Biarc;
import wildfire.wildfire.curve.CompositeArc;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.mechanics.FollowDiscreteMechanic;
import wildfire.wildfire.mechanics.FollowSmartDodgeMechanic;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class WaitState extends State {

	private boolean alwaysSmartDodge;

	/*
	 * How far we want to be from the ball's bounce
	 */
	private final double desiredDistanceGround = 44;
	private final double offsetDecrement = 0.05;

	private Vector2 bounce = null;
	private double timeLeft = 0;
	private double bounceDistance;
	private boolean planSmartDodge/** , planSmartDodgeCone */
			, towardsOwnGoal;
	private Vector2 enemyGoal;
	private Impact smartDodgeCandidate;

	public WaitState(Wildfire wildfire, boolean alwaysSmartDodge){
		super("Wait", wildfire);
		this.alwaysSmartDodge = alwaysSmartDodge;
	}

	@Override
	public boolean ready(InfoPacket input){
		if(!input.car.onFlatGround)
			return false;

		if(this.runningMechanic() && this.smartDodgeCandidate != null
				&& this.currentMechanic instanceof FollowSmartDodgeMechanic
				&& Behaviour.isOnPrediction(wildfire.ballPrediction, this.smartDodgeCandidate.getPosition())){
			return true;
		}

		Vector3 bounce3 = Behaviour.getBounce(wildfire.ballPrediction);
		if(bounce3 == null)
			return false;

		bounce = bounce3.flatten();
		bounce = bounce
				.plus(bounce.minus(Behaviour.getTarget(input.car, bounce)).scaledToMagnitude(desiredDistanceGround));
		timeLeft = Behaviour.getBounceTime(input.elapsedSeconds, wildfire.ballPrediction);
		bounceDistance = bounce.distance(input.car.position.flatten()) + (input.car.position.z - Constants.BALL_RADIUS);

		towardsOwnGoal = Behaviour.isTowardsOwnGoal(input.car, bounce.withZ(0), 240);
		enemyGoal = Behaviour.getTarget(input.car, bounce);
		smartDodgeConditions(input, bounce, enemyGoal, input.info.impact.getPosition(), towardsOwnGoal);
		smartDodgeCandidate = (planSmartDodge ? input.info.jumpImpact : null);
		this.planSmartDodge &= (smartDodgeCandidate != null);
		if(planSmartDodge && (this.runningMechanic() && !(this.currentMechanic instanceof FollowSmartDodgeMechanic)))
			this.currentMechanic = null;

		// Wall hit.
		double wallDistance = Utils.distanceToWall(input.info.impact.getPosition());
//		if(input.info.impact.getPosition().y * input.car.sign < 1500
//				&& wallDistance < 260 && Math.abs(input.info.impact.getPosition().x) > 1500){
//			return false;
//		}

		// Can't reach point (optimistic).
		if(DrivePhysics.maxDistance(timeLeft,
				input.car.velocityDir(bounce.withZ(Constants.RIPPER_RESTING).minus(input.car.position)),
				input.car.boost) < bounceDistance - 50){
			return false;
		}

		// Teammate's closer.
		if(Behaviour.isTeammateCloser(input, bounce))
			return false;

//		// Opponent's corner.
//		if(input.car.sign * bounce.y > 4000 && Constants.enemyGoal(input.car.team).distance(bounce) > 1800 && !Behaviour.isInCone(input.car, bounce3, 1000)) return bounceDistance < 2200;
//
		return ((Behaviour.isBallAirborne(input.ball) || input.info.impact.getPosition().z > 200)
				&& input.ball.velocity.flatten().magnitude() < 5000)
				|| (input.ball.position.z > 110 && input.ball.position.distanceFlat(input.car.position) < 220
						&& wallDistance > 400);

//		return true;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;

		// Aerial.
		boolean onTarget = Behaviour.isOnTarget(wildfire.ballPrediction, car.team);
		double impactRadians = Handling.aim(car, input.info.impact.getPosition().flatten());
		if(input.info.impact.getPosition().z > (onTarget && Math.abs(car.position.y) > 4500 ? 220 : 700)
				&& Behaviour.correctSideOfTarget(car, input.info.impact.getPosition()) && car.hasWheelContact
				&& Math.abs(impactRadians) < (onTarget ? 0.42 : 0.32)
				&& input.info.impact.getPosition().y * car.sign < (onTarget ? -1500 : -2500)
				&& (onTarget || car.sign * input.ball.velocity.y < -1000)){
			currentAction = AerialAction.fromBallPrediction(this, car, wildfire.ballPrediction,
					input.info.impact.getPosition().z > 420);
			if(currentAction != null && !currentAction.failed)
				return currentAction.getOutput(input);
			currentAction = null;
		}

		wildfire.renderer.drawCircle(Color.YELLOW, bounce, Constants.BALL_RADIUS);

		/*
		 * Smart dodge.
		 */
		planSmartDodge = (planSmartDodge && smartDodgeCandidate != null
				&& Utils.toLocal(car, smartDodgeCandidate.getPosition()).z > 80);
		wildfire.renderer.drawString2d("Plan Smart Dodge: " + planSmartDodge, Color.WHITE, new Point(0, 40), 2, 2);

//		double circleRadius = getDesiredDistance(car, planSmartDodge ? smartDodgeCandidate.getPosition() : null);
//		wildfire.renderer.drawCircle(Color.ORANGE, bounce, circleRadius);

		// Drive down the wall.
		if(Behaviour.isOnWall(car) && bounce.withZ(Constants.BALL_RADIUS).distance(car.position) > 700){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}else if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}

		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s", Color.WHITE, new Point(0, 20), 2, 2);

		// Make use of the candidate position from the smart dodge.
		if(planSmartDodge && input.car.onFlatGround){
			if(car.onFlatGround){
				Pair<DiscreteCurve, Impact> result = findSmartDodgeCurve(car, smartDodgeCandidate);
				if(result != null){
					DiscreteCurve discrete = result.getOne();
					Impact candidate = result.getTwo();
					return this.startMechanic(
							new FollowSmartDodgeMechanic(this, discrete, input.elapsedSeconds, candidate), input);
				}
			}

			SmartDodgeAction smartDodge = new SmartDodgeAction(this, input, false);
			if(!smartDodge.failed){
				return this.startAction(smartDodge, input);
			}

			return Handling.arriveAtSmartDodgeCandidate(car, smartDodgeCandidate, wildfire.renderer);
		}

		// Catch (don't move out the way anymore).
		if(car.position.distanceFlat(bounce) < desiredDistanceGround / 4 && Behaviour.correctSideOfTarget(car, bounce)){
			wildfire.renderer.drawString2d("Catch", Color.WHITE, new Point(0, 60), 2, 2);
			return Handling.stayStill(car);
		}

		// Curve!
		if(car.onFlatGround){
			Vector2 bounceGoalDir = enemyGoal.minus(bounce).normalised();
			Vector2 carBounceDir = bounce.minus(car.position.flatten()).normalised();
			for(double offset = 1; offset >= 0; offset -= offsetDecrement){
				Vector2 targetDirection = bounceGoalDir.scaled(offset).plus(carBounceDir.scaled(1 - offset));
				if(targetDirection.isZero())
					targetDirection = new Vector2(carBounceDir);

				Biarc biarc = new Biarc(car.position.flatten(), car.orientation.forward.flatten(), bounce,
						targetDirection);
				DiscreteCurve discrete = new DiscreteCurve(car.forwardVelocity, car.boost, biarc, timeLeft);

				if(discrete.isValid() && discrete.getTime() < timeLeft){
					return this.startMechanic(
							new FollowDiscreteMechanic(this, discrete, input.elapsedSeconds, timeLeft), input);
				}
			}
		}

		/*
		 * No curve found :(
		 */

		double radians = Handling.aim(car, bounce);

		boolean dribble = (input.ball.position.z > 100 && input.ball.position.distance(car.position) < 290);

		// Dodge.
		if(!dribble && !towardsOwnGoal && car.velocity.magnitude() > 950){
			if(Math.abs(radians) < Math.PI * 0.2
					&& (car.position.distanceFlat(bounce) < 620 && input.ball.position.z < 300)){
				currentAction = new DodgeAction(this, radians, input);
				if(!currentAction.failed)
					return currentAction.getOutput(input);
			}
			currentAction = null;
		}

		// Handling.
		ControlsOutput controls = Handling.steering(car, bounce);
		double targetVelocity = (bounceDistance / timeLeft);
		double currentVelocity = car.velocityDir(bounce.withZ(Constants.RIPPER_RESTING).minus(car.position));
		double acceleration = ((targetVelocity - currentVelocity) / timeLeft);
		if(Math.abs(radians) > 0.35){
			// Turn.
			return Handling.turnOnSpot(car, bounce);
		}else{
			double throttle = Handling.produceAcceleration(car, acceleration);
			controls.withThrottle(throttle).withBoost(throttle > 1);
		}

		wildfire.renderer.drawString2d("Velocity Needed: " + (int)targetVelocity + "uu/s", Color.WHITE,
				new Point(0, 60), 2, 2);
		wildfire.renderer.drawString2d("Acceleration Req.: " + (int)acceleration + "uu/s^2", Color.WHITE,
				new Point(0, 80), 2, 2);

		return controls;
	}

	private void smartDodgeConditions(InfoPacket input, Vector2 bounce, Vector2 enemyGoal, Vector3 impactLocation,
			boolean towardsOwnGoal){
		CarData car = input.car;

		this.planSmartDodge = false;
//		this.planSmartDodgeCone = false;

		if(alwaysSmartDodge){
			this.planSmartDodge = true;
		}else if(!towardsOwnGoal && car.hasWheelContact && car.position.z < 200){ // && impactLocation.z > 250
			if(Behaviour.closestOpponentDistance(input, bounce.withZ(Constants.BALL_RADIUS)) < 2200){
				this.planSmartDodge = true;
			}else if(bounce.distance(enemyGoal) < 2900){
				this.planSmartDodge = true;
//				this.planSmartDodgeCone = true;
			}else if(bounce.distance(Constants.homeGoal(car.team)) < 2900){
				this.planSmartDodge = true;
			}
		}
	}

//	private Vector3 simJumpRoot(CarData car){
//		Vector3 velocity = car.velocity.withZ(SmartDodgeAction.jumpVelocity); // Max jump velocity.
//		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
//		return simJump(car, car.position, velocity);
//	}
//
//	private Vector3 simJump(CarData car, Vector3 start, Vector3 velocity){
//		if(start.isOutOfBounds()) return start;
//
//		final double scale = (1D / 60);
//		velocity = velocity.plus(new Vector3(0, 0, -Constants.GRAVITY * scale)); //Gravity
//		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
//		boolean up = (velocity.z > 0);
//
//		Vector3 next = start.plus(velocity.scaled(scale));
//		if(renderJump) wildfire.renderer.drawLine3d((velocity.z > 0 ? Color.CYAN : Color.GRAY), start.toFramework(), next.toFramework());
//
//		Vector3 continued = simJump(car, next, velocity);
//		return (up ? continued : start);
//	}
//
//	public double getDesiredDistance(CarData car, Vector3 smartDodgeCandidate){
//		if(smartDodgeCandidate == null) return desiredDistanceGround;
//		Vector3 jump = simJumpRoot(car);
//		return jump.distanceFlat(car.position);
//	}

	private Pair<DiscreteCurve, Impact> findSmartDodgeCurve(CarData car, Impact candidate){
		Vector2 carPosition = car.position.flatten();
		Vector2 flatCandidate = candidate.getBallPosition().flatten();
		Vector2 candidateGoalDir = enemyGoal.minus(flatCandidate).normalised();
		Vector2 carBounceDir = flatCandidate.minus(carPosition).normalised();

		// Arrival constants.
		double peakTime = JumpPhysics
				.getFastestTimeZ(candidate.getPosition().minus(car.position).dotProduct(car.orientation.up));
		double driveTime = (candidate.getTime() - peakTime);
		double lineup = Utils.clamp(
				DrivePhysics.maxVelocity(Math.max(0, car.forwardVelocity), car.boost, driveTime) * peakTime, 700,
				flatCandidate.distance(carPosition) / 3);

		for(double offset = 1; offset >= 0; offset -= 1D / 8){
			Vector2 targetDirection = candidateGoalDir.scaled(offset).plus(carBounceDir.scaled(1 - offset));
			if(targetDirection.isZero())
				targetDirection = new Vector2(carBounceDir);

			Vector2 end = flatCandidate.minus(targetDirection
					.scaledToMagnitude(Constants.BALL_RADIUS + (car.hitbox.length / 2 + car.hitbox.offset.y)));

			CompositeArc compositeArc = CompositeArc.create(car, end, Utils.traceToWall(flatCandidate, targetDirection),
					Constants.RIPPER.y, lineup);
			Vector2[] points = compositeArc.discretise(DiscreteCurve.analysePoints);

			DiscreteCurve discreteCurve = new DiscreteCurve(car.forwardVelocity, car.boost, points,
					OptionalDouble.of(timeLeft));
			if(!discreteCurve.isValid())
				continue;

			/*
			 * Time check. This is actually something that needs to be redone, it's actually
			 * questioning "can I drive under the candidate before the ball reaches it",
			 * rather than
			 * "can I reach a position to jump to the candidate before the ball reaches it"
			 */
			if(discreteCurve.getTime() > driveTime)
				continue;

			// Render.
			wildfire.renderer.drawPolyline3d(Color.PINK, points);

			if(!PathState.curveOutOfBounds(discreteCurve)){
				Impact curveCandidate = new Impact(end.withZ(candidate.getPosition().z), candidate.getBallPosition(),
						candidate.getTime());
				return new Pair<DiscreteCurve, Impact>(discreteCurve, curveCandidate);
			}else{
				break;
			}
		}

		return null;
	}

}
