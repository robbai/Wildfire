package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.Info;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;

public class PatientShootState extends State {
	
	private static final double maxLowZ = 150, goalThreshold = -210;

	private Impact target;
	private double globalTargetTime;
	private boolean jump, go;

	public PatientShootState(Wildfire wildfire){
		super("Patient Shoot", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		CarData car = input.car;
		if(!car.onFlatGround) return false; 
			
		Impact earliestImpact = input.info.impact;
		if(earliestImpact == null || earliestImpact.getBallPosition().y * car.sign < -4400) return false;
		
		// We already have an available shot!
		if(earliestImpact.getBallPosition().z < maxLowZ && Behaviour.isInCone(car, earliestImpact.getPosition(), goalThreshold)) return false;
		if(input.info.jumpImpact != null){
			if(input.info.jumpImpact.getBallPosition().z < maxLowZ && Behaviour.isInCone(car, input.info.jumpImpact.getPosition(), goalThreshold)) return false;
		}
		
		// Find if we are under pressure or not.
		double enemyTime = determineEnemyTime(input);
		if(enemyTime < earliestImpact.getTime()) return false;
		
		// Iterate through the ball prediction to find a good shot.
		BallPrediction ballPrediction = wildfire.ballPrediction;
		int end = Math.min(ballPrediction.slicesLength(), (int)(enemyTime * 60));
		for(int i = earliestImpact.getFrame(); i < end; i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			Vector3 slicePosition = new Vector3(rawSlice.physics().location());
			
			if(slicePosition.y * car.sign < -3000) break;
			
			if(slicePosition.z > Info.maxJumpImpactZ) continue;
			this.jump = (slicePosition.z > maxLowZ);
			if(!Behaviour.isInCone(car, slicePosition, goalThreshold)) continue;
			
			double globalTime = (rawSlice.gameSeconds() - 3D / 120);
			if(globalTime <= car.elapsedSeconds) continue;
			
			// Found a shot.
			Vector3 targetPosition = slicePosition.plus(car.position.minus(slicePosition).withZ(0).scaledToMagnitude(Constants.BALL_RADIUS));
			this.globalTargetTime = globalTime;
			this.target = new Impact(targetPosition, rawSlice, globalTime - car.elapsedSeconds);
			return true;
		}
		
		// No appropriate slice found.
		return false;
	}

	@Override
	public boolean expire(InfoPacket input){
		if(this.target == null) return true;
//		boolean expire = !Behaviour.isOnPredictionAroundGlobalTime(wildfire.ballPrediction, target.getBallPosition(), globalTargetTime, 12);
		boolean expire = !Behaviour.isOnPrediction(wildfire.ballPrediction, target.getBallPosition());
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
			double displacement = (targetPosition.distanceFlat(car.position) - Constants.RIPPER.y);
			displacement *= Math.signum(car.orientation.forward.dotProduct(targetPosition.minus(car.position)));
			double time = (globalTargetTime - car.elapsedSeconds);
			double initialVelocity = car.velocityDir(targetPosition.minus(car.position).flatten());
			double finalVelocity = ((2 * displacement) / time - initialVelocity);
			acceleration = (finalVelocity - initialVelocity) / time;
			
			/*
			 *  We adjust our target acceleration so that our final velocity
			 *  is closer to the maximum (so we hit the ball as hard as possible!).
			 */
			if(displacement < 200 || time < (this.jump ? (input.info.jumpImpact == null ? 10 : input.info.jumpImpact.getTime()) : input.info.impact.getTime()) + 0.12){
				this.go = true;
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
		if(Math.abs(radians) > Math.toRadians(15) && !go) return Handling.turnOnSpot(car, targetPosition);
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
		return Handling.forwardDrive(car, targetPosition).withThrottle(throttle).withBoost(throttle > 1);
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
