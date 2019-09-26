package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;

public class PatientShootState extends State {
	
	private static final double maxSliceZ = 145, goalThreshold = -200;

	private Impact target;
	private double globalTargetTime;

	public PatientShootState(Wildfire wildfire){
		super("Patient Shoot", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		CarData car = input.car;
		if(!car.hasWheelContact || car.orientation.roofVector.z < 0.8) return false; 
			
		Impact earliestImpact = input.info.impact;
		if(earliestImpact == null) return false;
		
		// We already have an available shot!
		if(earliestImpact.getBallPosition().z < maxSliceZ && Behaviour.isInCone(car, earliestImpact.getPosition(), goalThreshold)) return false;
		if(input.info.jumpImpact != null){
			if(input.info.jumpImpact.getBallPosition().z < maxSliceZ && Behaviour.isInCone(car, input.info.jumpImpact.getPosition(), goalThreshold)) return false;
		}
		
		BallPrediction ballPrediction = wildfire.ballPrediction;
		
		double enemyTime = determineEnemyTime(input);
		if(enemyTime < earliestImpact.getTime()) return false;
		
		int end = Math.min(ballPrediction.slicesLength(), (int)(enemyTime * 60));
		for(int i = earliestImpact.getFrame(); i < end; i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			Vector3 slicePosition = Vector3.fromFlatbuffer(rawSlice.physics().location());
			
			if(slicePosition.z > maxSliceZ) continue;
			if(!Behaviour.isInCone(car, slicePosition, goalThreshold)) continue;
			
			// Found a shot.
			Vector3 targetPosition = slicePosition.plus(car.position.minus(slicePosition).scaledToMagnitude(Constants.BALL_RADIUS));
			this.globalTargetTime = rawSlice.gameSeconds();
			this.target = new Impact(targetPosition, rawSlice, rawSlice.gameSeconds() - car.elapsedSeconds);
			return true;
		}
		
		// No appropriate slice found.
		return false;
	}

	@Override
	public boolean expire(InfoPacket input){
		if(this.target == null) return true;
		return !Behaviour.isOnPredictionAroundGlobalTime(wildfire.ballPrediction, target.getPosition(), globalTargetTime, 16);
	}
	
	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		Vector3 targetPosition = this.target.getPosition();
		Vector3 targetBallPosition = this.target.getBallPosition();
		
		// Motion equations.
		double s = (targetPosition.distanceFlat(car.position) - Constants.RIPPER.y);
		s *= Math.signum(car.orientation.noseVector.dotProduct(targetPosition.minus(car.position)));
		double t = (globalTargetTime - car.elapsedSeconds);
		double u = car.velocityDir(targetPosition.minus(car.position).flatten());
		double v = ((2 * s) / t - u);
		double a = (v - u) / t;
		
		a /= Math.pow(input.info.impact.getTime() / t, 2);
		
		// Render.
		wildfire.renderer.drawString2d("Final Velocity: " + (int)v + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Target Acceleration: " + (int)a + "uu/s^2", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, 0, (int)(t * 60));
		wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition(), Color.RED, 40);
		wildfire.renderer.drawCrosshair(car, targetBallPosition, Color.ORANGE, 80);
		wildfire.renderer.drawCrosshair(car, targetPosition, Color.YELLOW, 50);
		
		// Controls.
//	    double radians = Handling.aim(car, this.target.getPosition());
//		if(Math.abs(radians) > Math.toRadians(50) && a < 550) return Handling.turnOnSpot(car, targetPosition);
		double throttle = Handling.produceAcceleration(car, a);
		return Handling.forwardDrive(car, targetPosition).withThrottle(throttle).withBoost(throttle > 1);
	}
	
	/*
	 * Effectively determines how patient we can be with this shot.
	 */
	private double determineEnemyTime(InfoPacket input){
		CarData enemy = input.info.earliestEnemy;
		if(enemy == null) return 15;
		boolean enemyCorrectSide = (Behaviour.correctSideOfTarget(enemy, input.info.earliestEnemyImpact.getPosition()));
		return input.info.enemyImpactTime * (enemyCorrectSide ? 0.8 : 0.95);
	}

}
