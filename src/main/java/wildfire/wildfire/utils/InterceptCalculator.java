package wildfire.wildfire.utils;

import rlbot.flat.BallPrediction;
import wildfire.input.car.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.physics.JumpPhysics;

public class InterceptCalculator {
	
	private static BallPrediction ballPrediction;

	public static void updateBallPrediction(BallPrediction ballPrediction){
		InterceptCalculator.ballPrediction = ballPrediction;
	}

	public static Impact getImpact(Vector3 carPosition, double carBoost, Vector3 carVelocity, double elapsedSeconds){
		// "NullPointerException - Lookin' good!"
		if(ballPrediction == null){
			System.err.println("NullPointerException - Lookin' good!");
			return null;
		}
		
		final double dt = 1D / 120;
		
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			boolean finalSlice = (i == ballPrediction.slicesLength() - 1);
			
			Vector3 slicePosition = new Vector3(rawSlice.physics().location());
			
			double time = (rawSlice.gameSeconds() - elapsedSeconds);
			if(time < 0) continue;
			
			if(!finalSlice){
				double targetDistance = slicePosition.distance(carPosition) - (Constants.BALL_RADIUS + Constants.RIPPER.y + Constants.RIPPER_OFFSET.y - 25);
				double initialVelocity = carVelocity.dotProduct(slicePosition.minus(carPosition).normalised());
								
				Car1D sim = new Car1D(0, initialVelocity, carBoost, 0);
				while(sim.getTime() < time){
					sim.step(dt, 1, true);
					if(sim.distanceFrom(targetDistance) <= 0){
						break;
					}
				}
				
				if(sim.distanceFrom(targetDistance) > 0) continue;
			}
			
			Vector3 impactPosition = slicePosition.plus(carPosition.minus(slicePosition).scaledToMagnitude(Constants.BALL_RADIUS));
			return new Impact(impactPosition, rawSlice, time);
		}
		
		return null; // Uh oh.
	}

	public static Impact getImpact(CarData car){
		return getImpact(car.position, car.boost, car.velocity, car.elapsedSeconds);
	}
	
	public static Impact getJumpImpact(CarData car){
		if(ballPrediction == null) return null;
		
		Vector2 carPosition = car.position.flatten();
//		Vector2 carForward = car.orientation.forward.flatten().normalized();
		
		final double hitboxDistanceForward = (Constants.BALL_RADIUS + Constants.RIPPER.y / 2 + Constants.RIPPER_OFFSET.y);
		final double hitboxDistanceSide = (Constants.BALL_RADIUS + Constants.RIPPER.x / 2);
		
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			
			double time = (rawSlice.gameSeconds() - car.elapsedSeconds);
//			time -= 1D / 120;
			
			Vector3 slicePosition = new Vector3(rawSlice.physics().location());
			
			Vector2 enemyGoal = Behaviour.getTarget(car, slicePosition.flatten(), -300);
			double goalAngle = enemyGoal.minus(slicePosition.flatten()).angle(slicePosition.flatten().minus(carPosition));
			double hitboxDistance = Utils.lerp(hitboxDistanceForward, hitboxDistanceSide, Math.sin(goalAngle));
//			hitboxDistance *= (Behaviour.correctSideOfTarget(car, slicePosition) ? (slicePosition.distanceFlat(enemyGoal) < 2500 ? 1.3 : 1.1 : 1.25);
			hitboxDistance *= Math.max(0.85, Math.cos(goalAngle) + 0.1);
//			hitboxDistance *= 1.1;
			Vector3 impactPosition = slicePosition.plus(slicePosition.flatten().minus(enemyGoal).withZ(0).scaledToMagnitude(hitboxDistance));
//			Vector3 impactPosition = slicePosition.plus(slicePosition.flatten().minus(enemyGoal).withZ(0).scaledToMagnitude(Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance * 0.405));
			
			Vector3 localPosition = Utils.toLocal(car, impactPosition);
			
			double jumpHeight = localPosition.z;
//			if(jumpHeight > JumpPhysics.maxJumpHeight) continue;
//			if(jumpHeight < JumpPhysics.minJumpHeight) continue;
			if(jumpHeight > JumpPhysics.maxJumpHeight + (Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance) * (SmartDodgeAction.zRatio * 0.7)) continue;
			jumpHeight = Utils.clamp(jumpHeight, JumpPhysics.minJumpHeight, JumpPhysics.maxJumpHeight);
			
			double fullDistance = localPosition.flatten().magnitude();
//			if(fullDistance < 0) continue;
			
			double peakTime = JumpPhysics.getFastestTimeZ(jumpHeight);
			double driveTime = (time - peakTime);
			
			double initialVelocity = car.velocityDir(slicePosition.minus(car.position).flatten());
			double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
//			double acceleration = ((finalVelocity - initialVelocity) / driveTime);
			
			if(finalVelocity < DrivePhysics.maxVelocity(initialVelocity, car.boost, time - 2D / 120)){
//				Vector3 impactPosition = slicePosition.plus(car.position.minus(slicePosition).scaledToMagnitude(Constants.BALLRADIUS));
//				Vector3 impactPosition = slicePosition.plus(slicePosition.minus(Constants.enemyGoal(car).withZ(slicePosition.z)).scaledToMagnitude(Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance * 0.4));
				return new Impact(impactPosition, rawSlice, time);
			}
		}
		
		return null;
	}

}
