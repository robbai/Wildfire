package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.Info;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class PatientShootState extends State {

	private static final double MAX_LOW_Z = 150, GOAL_THRESHOLD = -210;

	private Impact target;
	private double globalTargetTime;
	private boolean jump, go;
	private Vector2 arrivalDirection;

	public PatientShootState(Wildfire wildfire){
		super("Patient Shoot", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		CarData car = input.car;
		if(!car.onFlatGround) return false; 

		Impact earliestImpact = input.info.impact;
		if(earliestImpact == null || earliestImpact.getBallPosition().y * car.sign < -3000) return false;
		if(earliestImpact.getBallPosition().distance(input.car.position) < 300) return false;

		// We already have an available shot!
		if(earliestImpact.getBallPosition().z < MAX_LOW_Z){
			if(Behaviour.isInCone(car, earliestImpact.getPosition(), GOAL_THRESHOLD / 2)) return false;
		}else if(input.info.jumpImpact != null){
			if(Behaviour.isInCone(car, input.info.jumpImpact.getPosition(), GOAL_THRESHOLD / 2)) return false;
		}
		
		final double offsetSize = (Constants.BALL_RADIUS + (car.hitbox.length / 2 + car.hitbox.offset.y));

		// Find if we are under pressure or not.
		double enemyTime = determineEnemyTime(input);
		if(enemyTime < earliestImpact.getTime()) return false;

		// Iterate through the ball prediction to find a good shot.
		BallPrediction ballPrediction = wildfire.ballPrediction;
		int end = Math.min(ballPrediction.slicesLength(), (int)(enemyTime * 60));
		for(int i = earliestImpact.getFrame(); i < end; i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			Vector3 slicePosition = new Vector3(rawSlice.physics().location());

			if(Math.abs(slicePosition.y) > Constants.PITCH_LENGTH - Constants.BALL_RADIUS) break;

			if(slicePosition.y * car.sign < -2000) continue;
			if(slicePosition.z > Info.maxJumpImpactZ) continue;
			if(!Behaviour.isInCone(car, slicePosition, GOAL_THRESHOLD)) continue;

			boolean jump = (slicePosition.z > MAX_LOW_Z);

			double globalTime = rawSlice.gameSeconds();
//			globalTime -= (2D / 120);
			if(globalTime <= car.elapsedSeconds) continue;
			double distance = (Utils.toLocal(car, slicePosition).flatten().magnitude() - offsetSize);
			if(DrivePhysics.minTravelTime(car, distance) > (globalTime - car.elapsedSeconds)) continue;

			// Found a shot.
			Vector3 targetPosition = slicePosition.plus(car.position.minus(slicePosition).withZ(0).scaledToMagnitude(offsetSize));
			this.globalTargetTime = globalTime;
			this.target = new Impact(targetPosition, slicePosition, globalTime - car.elapsedSeconds);
			this.jump = jump;
			this.arrivalDirection = targetPosition.minus(car.position).flatten();
			return true;
		}

		// No appropriate slice found.
		return false;
	}

	@Override
	public boolean expire(InfoPacket input){
		if(this.target == null) return true;
		boolean expire = !Behaviour.isOnPredictionAroundGlobalTime(wildfire.ballPrediction, target.getBallPosition(), globalTargetTime, 12);
		//		boolean expire = !Behaviour.isOnPrediction(wildfire.ballPrediction, target.getBallPosition());
		if(!expire){
			if(this.jump){
				expire = (input.info.jumpImpact != null && this.globalTargetTime < (input.info.jumpImpact.getTime() + input.elapsedSeconds));
			}else{
				expire = (this.globalTargetTime < (input.info.impact.getTime() + input.elapsedSeconds));
			}
		}
		if(!expire){
			Vector2 targetDirection = this.target.getPosition().minus(input.car.position).flatten();
			expire = (this.arrivalDirection.angle(targetDirection) > Math.toRadians(15));
		}
		if(expire) this.go = false;
		return expire;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		Vector3 targetPosition = this.target.getPosition();
		Vector3 targetBallPosition = this.target.getBallPosition();

		double acceleration = 0;
		if(!this.jump || !go){
			// Motion equations.
			double displacement = targetPosition.distanceFlat(car.position);
			//			displacement *= Math.signum(car.orientation.forward.dotProduct(targetPosition.minus(car.position)));
			double time = Math.max(0, globalTargetTime - car.elapsedSeconds);
			double initialVelocity = car.velocityDir(targetPosition.minus(car.position));
			double finalVelocity = ((2 * displacement) / time - initialVelocity);
			acceleration = ((finalVelocity - initialVelocity) / time);

			/*
			 *  We adjust our target acceleration so that our final velocity
			 *  is closer to the maximum (so we hit the ball as hard as possible!).
			 */
			if(!this.go){
				if(Math.abs(displacement) < 200){
					this.go = true;
				}else if(this.jump){
					this.go = (input.info.jumpImpact == null || time < (input.info.jumpImpact.getTime() + 0.21));
				}else{
					this.go = (time < (input.info.impact.getTime() + 0.17));
				}
			}
			if(!this.go){
				acceleration = 0;
				if(Math.abs(initialVelocity) > Constants.COAST_ACCELERATION / 60){
					acceleration = -initialVelocity * 60;
				}
			}

			// Render.
			wildfire.renderer.drawString2d("Final Velocity: " + (int)finalVelocity + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
			wildfire.renderer.drawString2d("Target Acceleration: " + (int)acceleration + "uu/s^2", Color.WHITE, new Point(0, 40), 2, 2);
			//			wildfire.renderer.drawString2d("Wait: " + Utils.round(wait), Color.WHITE, new Point(0, 20), 2, 2);
			wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition(), Color.RED, 40);
		}

		// Render.
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, 0, (int)((globalTargetTime - car.elapsedSeconds) * 60));
		wildfire.renderer.drawCrosshair(car, targetBallPosition, Color.ORANGE, 80);
		wildfire.renderer.drawCrosshair(car, targetPosition, Color.YELLOW, 50);

		// Controls.
		double radians = Handling.aim(car, this.target.getPosition());
		if(Math.abs(radians) > Math.toRadians(15)) return Handling.turnOnSpot(car, targetPosition);
		if(this.jump){
			SmartDodgeAction smartDodge = new SmartDodgeAction(this, input, false);
			if(!smartDodge.failed){
				return this.startAction(smartDodge, input);
			}
			if(go){
				Slice candidate = new Slice(targetPosition, this.globalTargetTime - car.elapsedSeconds);
				return Handling.arriveAtSmartDodgeCandidate(car, candidate);
			}
		}
		double throttle = Handling.produceAcceleration(car, acceleration);
		return Handling.forwardDrive(car, targetPosition, false).withThrottle(throttle).withBoost(throttle > 1);
	}

	/**
	 * Determines how patient we can be with this shot.
	 */
	private double determineEnemyTime(InfoPacket input){
		if(input.info.impact.getBallPosition().y * input.car.sign > Constants.PITCH_LENGTH - 700) return 15;
		CarData enemy = input.info.earliestEnemy;
		if(enemy == null) return 15;
		boolean enemyCorrectSide = (Behaviour.correctSideOfTarget(enemy, input.info.earliestEnemyImpact.getPosition()));
		return input.info.enemyImpactTime * (enemyCorrectSide ? 0.8 : 0.95);
	}

}
