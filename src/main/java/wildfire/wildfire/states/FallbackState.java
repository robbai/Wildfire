package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.actions.WavedashAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class FallbackState extends State {

	/*
	 * These two mystical values hold the secrets to this state.
	 */
	private static final double dropoff = 0.176, scope = 0.385;

	/*
	 * Yeah this one too, I guess.
	 */
	private static final int targetPly = 7;

	public FallbackState(Wildfire wildfire){
		super("Fallback", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		return false;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		boolean wall = Behaviour.isOnWall(car);
		double impactDistance = input.info.impactDistance;
		Vector3 impactPosition = input.info.impact.getPosition();
		Impact jumpImpact = input.info.jumpImpact;

		// Smart-dodge.
		boolean smartDodge = (jumpImpact != null);
		if(smartDodge){
			smartDodge = isOkayToSmartDodge(input);
		}

		// Drive down the wall.
		if(wall && Utils.toLocal(car, impactPosition).z > 140){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}

		// Smart-dodge.
		if(smartDodge){
			SmartDodgeAction smartDodgeAction = new SmartDodgeAction(this, input, false);
			if(!smartDodgeAction.failed){
				return this.startAction(smartDodgeAction, input);
			}
			return Handling.arriveAtSmartDodgeCandidate(car, jumpImpact, wildfire.renderer);
		}

		// Goal.
		Vector2 goal = Behaviour.getTarget(car, input.ball, -190);
		wildfire.renderer.drawCrosshair(car, goal.withZ(Constants.BALL_RADIUS), Color.WHITE, 125);

		// Impact point.
		//		Vector3 impactPoint = matchHeight(input.info.impact.getPosition(), car);
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, input.info.impact.getFrame());
		if(!smartDodge){
			wildfire.renderer.drawCrosshair(car, impactPosition, Color.MAGENTA, 125);
		}else{
			wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.RED, input.info.impact.getFrame(), jumpImpact.getFrame());
			wildfire.renderer.drawCrosshair(car, jumpImpact.getBallPosition(), Color.ORANGE, 105);
		}

		// Avoid own-goaling.
		Vector2 trace = Utils.traceToY(car.position.flatten(), impactPosition.minus(car.position).flatten(), car.sign * -Constants.PITCH_LENGTH);
		boolean avoidOwnGoal = !Behaviour.correctSideOfTarget(car, input.ball.position) && trace != null;
		if(avoidOwnGoal){
			boolean tight = (Math.abs(impactPosition.minus(car.position).flatten().normalised().x) > 0.7 && Math.abs(impactPosition.y) > 3000);
			Vector3 avoidOffset = new Vector3(-Math.signum(trace.x) * Utils.clamp(impactDistance / 4.5, 60, 400), tight ? -car.sign * 30 : 0, 0);
			impactPosition = impactPosition.plus(avoidOffset);
			wildfire.renderer.drawCrosshair(car, impactPosition, Color.PINK, 125);
		}

		// Target.
		Vector3 localTarget = getLocalPosition(car, new Vector3(), Utils.toLocal(car, goal.withZ(Constants.BALL_RADIUS)), 0, Utils.toLocal(car, impactPosition));
		Vector3 target = Utils.toGlobal(car, localTarget);
		target = Behaviour.goalStuck(car, target);
		wildfire.renderer.drawCircle(Color.ORANGE, target, Constants.BALL_RADIUS * 0.2);

		double velocityTowardsImpact = car.velocityDir(input.info.impact.getPosition().minus(car.position).flatten());
		double steerImpact = Handling.aim(car, input.info.impact.getPosition().flatten());

		/*
		 * Actions
		 */
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
		}else if(input.info.impact.getTime() < Behaviour.IMPACT_DODGE_TIME && velocityTowardsImpact > (wall ? 900 : 1200) && Math.abs(car.forwardVelocity) > 0.95){
			double dodgeAngle = steerImpact;
			double goalAngle = Vector2.angle(input.info.impact.getPosition().minus(car.position).flatten(), goal.minus(car.position.flatten()));

			// Check if we can go for a shot.
			Vector2 traceGoal = Utils.traceToY(car.position.flatten(), input.info.impact.getPosition().minus(car.position).flatten(), car.sign * Constants.PITCH_LENGTH);
			boolean shotOpportunity = (traceGoal != null && Math.abs(traceGoal.x) < 1300);

			if(Math.abs(input.ball.velocity.z) > 500 || wall || !shotOpportunity ? (Math.abs(dodgeAngle) < 0.3) : (goalAngle < 0.5 || car.sign * input.ball.velocity.y < -500 || car.sign * car.position.y < -3000)){
				// If the dodge angle is small, make it big - trust me, it works.
				if(Math.abs(dodgeAngle) < Math.toDegrees(85) && car.onFlatGround){
					dodgeAngle = Utils.clamp(dodgeAngle * (wall ? 2 : 3.25), -Math.PI, Math.PI);
				}
				currentAction = new DodgeAction(this, dodgeAngle, input);
			}
		}else if(wall && Math.abs(car.position.x) < Constants.GOAL_WIDTH - 50){
			currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
		}else if(car.forwardVelocity < -800 && ((input.info.impact.getTime() < Behaviour.IMPACT_DODGE_TIME && Math.abs(input.info.impactRadians) < 0.15) || Behaviour.dodgeDistance(input.car) < input.info.impactDistanceFlat)){
			currentAction = new HalfFlipAction(this, input);
		}else if(input.info.impact.getTime() > (avoidOwnGoal ? 1.45 : 2.25) && !car.isSupersonic 
				&& car.forwardVelocity > (car.boost < 1 ? 1200 : 1500) && Math.abs(steerImpact) < 0.2){
			// Front flip for speed.
			if(car.boost < 10 || Math.abs(steerImpact) > 0.08 || input.info.impact.getTime() < 2.5 || Utils.distanceToWall(car.position) < 300){
				currentAction = new DodgeAction(this, 0, input);
			}else{
				// Throw out a little wavedash.
				currentAction = new WavedashAction(this, input);
			}
		}
		if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		currentAction = null;

		/*
		 *  Controls and handling.
		 */
		double radians = Handling.aim(car, target);

		boolean movingSlow = (car.forwardVelocityAbs < 900);
		boolean insideTurningRadius = (Handling.insideTurningRadius(car, target) && Math.abs(radians) > 0.24);
		if(insideTurningRadius) wildfire.renderer.drawTurningRadius(Color.WHITE, car);

		double maxVelocity = (insideTurningRadius && !movingSlow ? Math.min(DrivePhysics.maxVelForTurn(car, target), Constants.SUPERSONIC_VELOCITY) : Constants.SUPERSONIC_VELOCITY);
		double acceleration = (maxVelocity - car.forwardVelocity) / 0.05;
		double throttle = Handling.produceAcceleration(car, acceleration);

//		ControlsOutput controls = Handling.forwardDrive(car, target, false);
		ControlsOutput controls = (car.forwardVelocity > (car.boost > 10 ? -350 : -250) ? Handling.forwardDrive(car, target, false) : Handling.chaosDrive(car, target, true));
		if(controls.getThrottle() < -Constants.COAST_THRESHOLD) return controls;
		return controls.withThrottle(throttle).withBoost(controls.holdBoost() && Math.abs(radians) < 0.2 && !car.isSupersonic && throttle > 1);
	}

	/**
	 * This brings the centre of the ball down to the car's height
	 * so that rendering doesn't clip through the ground.
	 */
	@SuppressWarnings ("unused")
	private Vector3 matchHeight(Vector3 position, CarData car){
		return position.plus(car.orientation.up.scaled(Constants.RIPPER_RESTING - Constants.BALL_RADIUS));
	}

	private Vector3 getLocalPosition(CarData car, Vector3 startLocal, Vector3 goalLocal, int ply, Vector3 impactPointLocal){
		if(ply >= 30) return null;

		double distance = impactPointLocal.distanceFlat(startLocal);

		Vector3 endLocal = impactPointLocal.plus(impactPointLocal.minus(goalLocal).withZ(0).scaledToMagnitude(distance * scope)).withZ(0);
		endLocal = startLocal.plus(endLocal.minus(startLocal).scaled(dropoff));//.confine(35, 50);

		if(car.hasWheelContact){
			//			wildfire.renderer.drawLine3d(Color.RED, Utils.toGlobal(car, startLocal).fbs(), Utils.toGlobal(car, endLocal).fbs());
			final double size = 80;
			Vector3 globalStart = Utils.toGlobal(car, startLocal);
			Vector3 globalEnd = Utils.toGlobal(car, endLocal);
			wildfire.renderer.drawUprightSquare(globalStart.plus(car.orientation.up.scaled(size / 2)), Color.RED, globalEnd.minus(globalStart), car.orientation.up, size);
		}

		Vector3 next = getLocalPosition(car, endLocal, goalLocal, ply + 1, impactPointLocal);
		return ply < targetPly ? (ply == targetPly ? startLocal : next) : endLocal;
	}

	private boolean isOkayToSmartDodge(InfoPacket input){
		CarData car = input.car;
		if(!car.onFlatGround) return false;
		
//		if(Utils.distanceToWall(input.info.jumpImpact.getBallPosition()) < (input.car.onFlatGround ? 220 : 180)) return false;
//		return input.info.jumpImpact.getTime() < 2.5 && input.info.impact.getBallPosition().minus(car.position).dotProduct(car.orientation.up) > 170;

		//return input.info.impact.getTime() < 4 && Utils.toLocal(car, input.info.impact.getBallPosition()).z > 205;

		if(input.info.impact.getPosition().minus(car.position).dotProduct(car.orientation.up) + (Constants.RIPPER_RESTING - Constants.BALL_RADIUS) > 140 && input.info.impactDistance < 4000){

//			if(Utils.toLocal(car, input.info.impact.getBallPosition()).z < 180){
//				return false;
//			}else if(input.info.impact.getTime() < 2 && Behaviour.correctSideOfTarget(car, input.info.impact.getBallPosition())){
//				return true;
//			}
		
			Impact jumpImpact = input.info.jumpImpact;
			Vector3 jumpImpactPosition = jumpImpact.getBallPosition();
			
			if(jumpImpact.getTime() > 2){
				if(Utils.distanceToWall(jumpImpactPosition) < (input.car.onFlatGround ? 220 : 180)) return false;
			}
//			else if(jumpImpact.getTime() < 1){
//				return true;
//			}

			Vector3 carPosition = car.position;
			Vector2 trace = Utils.traceToWall(carPosition.flatten(), jumpImpactPosition.flatten());
			Vector2 carToTrace = trace.minus(carPosition.flatten());

			if(carToTrace.y * car.sign < 0){
				//if(jumpImpact.y * car.sign < -(Constants.PITCHLENGTH - 1800)){
				//	return Math.abs(carToTrace.normalized().x) > 0.8;
				//}
				return Math.abs(trace.x) > (trace.x * jumpImpactPosition.x < 0 ? Constants.GOAL_WIDTH + 250 : Constants.PITCH_WIDTH - 1200);
			}else{
				if(jumpImpact.getTime() < 1.5) return true;
				return Math.abs(trace.x) < Constants.GOAL_WIDTH + 50 || Math.abs(trace.x) > Constants.PITCH_WIDTH - 1300;
			}
		}
		return false;
	}

}
