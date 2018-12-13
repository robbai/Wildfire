package rlbotexample.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.vector.Vector3;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.AerialAction;
import rlbotexample.wildfire.actions.DodgeAction;
import rlbotexample.wildfire.actions.RecoveryAction;

public class WaitState extends State {
	
	/*How far we want to be from the ball's bounce*/
	private final double desiredDistance = 26D;
	
	private Vector2 bounce = null;
	private double timeLeft = 0;

	public WaitState(Wildfire wildfire){
		super("Wait", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		Vector3 bounce3 = Utils.getBounce(wildfire.ballPrediction);
		if(bounce3 == null) return false;
		
		bounce = bounce3.flatten();
		timeLeft = Utils.getBounceTime(wildfire.ballPrediction);
		
		//Can't reach point
		if(bounce.distance(input.car.position.flatten()) / timeLeft > 3800) return false;
		
		//Teammate's closer
		if(Utils.isTeammateCloser(input, bounce)) return false;
		
		return (Utils.isBallAirborne(input.ball) && input.ball.velocity.magnitude() < 6000) || (input.ball.position.z > 110 && input.ball.position.distanceFlat(input.car.position) < 220);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Mostly for aerial purposes
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, wildfire.team);
		
		//Aerial
		double steerImpact = Utils.aim(input.car, wildfire.impactPoint.flatten());
		if(!hasAction() && wildfire.impactPoint.z > (onTarget && Math.abs(input.car.position.y) > 4500 ? 200 : 350) && Utils.isEnoughBoostForAerial(input.car, wildfire.impactPoint) && Math.signum(wildfire.impactPoint.y - input.car.position.y) == Utils.teamSign(input.car) && input.car.hasWheelContact && Math.abs(steerImpact) < (onTarget ? 0.47 : 0.35) && wildfire.impactPoint.y * Utils.teamSign(input.car) < (onTarget ? -1000 : -2000)){
			double maxRange = wildfire.impactPoint.z * (onTarget ? 6 : 5);
			double minRange = wildfire.impactPoint.z * (onTarget ? 1 : 3);
			Utils.drawCircle(wildfire.renderer, Color.ORANGE, wildfire.impactPoint.flatten(), minRange);
			Utils.drawCircle(wildfire.renderer, Color.ORANGE, wildfire.impactPoint.flatten(), maxRange);
			if(Utils.isPointWithinRange(input.car.position.flatten(), wildfire.impactPoint.flatten(), minRange, maxRange)){
				currentAction = new AerialAction(this, input, wildfire.impactPoint.z > 600);
				return currentAction.getOutput(input);
			}
		}

		Utils.drawCircle(wildfire.renderer, Color.YELLOW, bounce, Utils.BALLRADIUS);
		
		//Drive down the wall
		if(Utils.isOnWall(input.car) && bounce.withZ(Utils.BALLRADIUS).distance(input.car.position) > 700){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}else if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this);
			return currentAction.getOutput(input);
		}
		
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s", Color.WHITE, new Point(0, 20), 2, 2);
		
		//Catch (don't move out the way anymore)
		if(input.car.position.distanceFlat(bounce) < Utils.BALLRADIUS){
			wildfire.renderer.drawString2d("Catch", Color.WHITE, new Point(0, 40), 2, 2);
			return new ControlsOutput().withBoost(false).withSteer((float)-Utils.aim(input.car, bounce) * 2F).withThrottle((float)-input.car.forwardMagnitude());
		}
		
		Vector2 enemyGoal = Utils.enemyGoal(wildfire.team);

		Vector2 target = null;
		double velocityNeeded = -1;
		
		for(double length = 0.75; length > 0; length -= 0.05){
			target = getNextPoint(input.car.position.flatten(), bounce, enemyGoal, length);
			
			//This curved pathway goes backwards, skip it
//			if(target.distance(bounce) > input.car.position.distanceFlat(bounce)) continue;
			boolean illegal = (target.distance(bounce) > input.car.position.distanceFlat(bounce));
			
			// v = 2s / t - u
			double distance = getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, illegal ? Color.ORANGE : Color.YELLOW);
			double initialVelocity = input.car.magnitudeInDirection(target.minus(input.car.position.flatten()));
			double finalVelocity = (2 * distance) / timeLeft - initialVelocity;
			
			if(finalVelocity <= Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, input.car.boost))){
				velocityNeeded = distance / timeLeft;
				
				Utils.drawCircle(wildfire.renderer, Color.GREEN, target, Math.min(4D, timeLeft) * 15D);
				getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, Color.GREEN); //Redraw in pretty green!
				
				wildfire.renderer.drawString2d("Offset: " + (int)(length * 100) + "%", Color.WHITE, new Point(0, 40), 2, 2);
				wildfire.renderer.drawString2d("Distance: " + (int)distance + "uu", Color.WHITE, new Point(0, 60), 2, 2);
				wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
				
				break;
			}
		}
		
		boolean noPoint = (velocityNeeded == -1);
		if(noPoint) target = bounce;
		
		double steerCorrectionRadians = Utils.aim(input.car, target);
	    float steer = (float)-steerCorrectionRadians * 2F;
		
		//No point found
		if(noPoint){
			velocityNeeded = bounce.distance(input.car.position.flatten()) / timeLeft;			
			wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 40), 2, 2);
			
			boolean dribble = (input.ball.position.z > 120 && input.ball.position.distance(input.car.position) < 200);
			
			//Dodge
			if(!hasAction() && !dribble){
				if(Math.abs(steerCorrectionRadians) < Math.PI * 0.2 && (input.car.position.distanceFlat(bounce) < 620 && input.ball.position.z < 300)){
					currentAction = new DodgeAction(this, steerCorrectionRadians, input);
					if(!currentAction.failed) return currentAction.getOutput(input);
				}
			}
		}
		
		ControlsOutput controls = new ControlsOutput().withSteer(steer);
		double currentVelocity = input.car.magnitudeInDirection(target.minus(input.car.position.flatten()));
			
		if(velocityNeeded > currentVelocity){
			if(velocityNeeded > currentVelocity + 400 || velocityNeeded > 1410){
				controls.withThrottle(1).withBoost(Math.abs(steer) < 0.55F);
			}else{
				controls.withThrottle(1);
			}
		}else if(velocityNeeded < currentVelocity - 300){
			controls.withThrottle(-1);
		}else{
			controls.withThrottle(0);
		}
		
	    return controls;
	}
	
	public double getPathwayDistance(Vector2 start, Vector2 bounce, Vector2 enemyGoal, double length, Color colour){
		double distance = 0;
		for(int i = 0; i < 16; i++){
			double dist = start.distance(bounce);
			if(i != 15){
				Vector2 next = getNextPoint(start, bounce, enemyGoal, length);
				double newDist = next.distance(bounce);
				if(newDist > desiredDistance){
					wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
					distance += start.distance(next);
					start = next;	
				}else{
					wildfire.renderer.drawLine3d(colour, start.toFramework(), start.plus(next.minus(start).scaledToMagnitude(dist - desiredDistance)).toFramework());
					distance += (newDist - desiredDistance);
					break;
				}
			}else{
				//Final point
				wildfire.renderer.drawLine3d(colour, start.toFramework(), start.plus(bounce.minus(start).scaledToMagnitude(dist - desiredDistance)).toFramework());
				distance += (dist - desiredDistance); //We only need to reach the side of the ball
				break;
			}
		}
		return distance;
	}
	
	private Vector2 getNextPoint(Vector2 start, Vector2 bounce, Vector2 enemyGoal, double length){
		double offset = 0.43 * bounce.distance(start);
		Vector2 target = bounce.plus(bounce.minus(enemyGoal).scaledToMagnitude(offset));
		target = start.plus(target.minus(start).scaled(length)).confine();
		
		//Clamp the X when stuck in goal
		if(Math.abs(start.y) > Utils.PITCHLENGTH){
			target = new Vector2(Math.max(-wildfire.goalSafeZone, Math.min(wildfire.goalSafeZone, target.x)), target.y);
		}
		
		return target;
	}

}
