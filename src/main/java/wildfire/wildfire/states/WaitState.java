package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.State;

public class WaitState extends State {
	
	/*How far we want to be from the ball's bounce*/
	private final double desiredDistance = 38D;
	
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
		
		//Wall hit
		if(timeLeft > 1 && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < 1500 && Utils.distanceToWall(wildfire.impactPoint.getPosition()) < 260 && Math.abs(wildfire.impactPoint.getPosition().x) > 1500){
			return false;
		}
		
		//Can't reach point (optimistic)
		if(bounce.distance(input.car.position.flatten()) / timeLeft > 2550) return false;
		
		//Teammate's closer
		if(Utils.isTeammateCloser(input, bounce)) return false;
		
		return (Utils.isBallAirborne(input.ball) && input.ball.velocity.flatten().magnitude() < 5000) || (input.ball.position.z > 110 && input.ball.position.distanceFlat(input.car.position) < 220);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){		
		//Aerial
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, input.car.team);
		double impactRadians = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		if(!hasAction() && wildfire.impactPoint.getPosition().z > (onTarget && Math.abs(input.car.position.y) > 4500 ? 200 : 350) && Utils.isEnoughBoostForAerial(input.car, wildfire.impactPoint.getPosition()) && Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()) && input.car.hasWheelContact && Math.abs(impactRadians) < (onTarget ? 0.47 : 0.32) && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < (onTarget ? -1000 : -2000)){
			double maxRange = wildfire.impactPoint.getPosition().z * (onTarget ? 7 : 6);
			double minRange = wildfire.impactPoint.getPosition().z * (onTarget ? 1 : 2);
			wildfire.renderer.drawCircle(Color.ORANGE, wildfire.impactPoint.getPosition().flatten(), minRange);
			wildfire.renderer.drawCircle(Color.ORANGE, wildfire.impactPoint.getPosition().flatten(), maxRange);
			if(Utils.isPointWithinRange(input.car.position.flatten(), wildfire.impactPoint.getPosition().flatten(), minRange, maxRange)){
				currentAction = new AerialAction(this, input, wildfire.impactPoint.getPosition().z > 600);
				return currentAction.getOutput(input);
			}
		}

		wildfire.renderer.drawCircle(Color.YELLOW, bounce, Utils.BALLRADIUS);
		boolean towardsOwnGoal = Utils.isTowardsOwnGoal(input.car, bounce.withZ(0));
		
		//Smart dodge (test)
		boolean planSmartDodge = (!towardsOwnGoal && input.car.position.z < 240 && (input.ball.position.z > 360 || input.ball.velocity.z < -500) && (Math.min(bounce.distance(Utils.homeGoal(input.car.team)), bounce.distance(Utils.enemyGoal(input.car.team))) < 2100 || Utils.closestOpponentDistance(input, bounce.withZ(0)) < 1000));
		if(!hasAction() && planSmartDodge && input.car.hasWheelContact && bounce.distance(input.car.position.flatten()) < (400 + 300 * Math.cos(impactRadians / 2))){ // && input.car.velocity.magnitude() < 2100
			currentAction = new SmartDodgeAction(this, input);
			if(!currentAction.failed) currentAction.getOutput(input);
		}
		
		//Drive down the wall
		if(Utils.isOnWall(input.car) && bounce.withZ(Utils.BALLRADIUS).distance(input.car.position) > 700){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}else if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		wildfire.renderer.drawString2d("Time: " + Utils.round(timeLeft) + "s", Color.WHITE, new Point(0, 20), 2, 2);
		
		//Catch (don't move out the way anymore)
		if(input.car.position.distanceFlat(bounce) < Utils.BALLRADIUS && Utils.correctSideOfTarget(input.car, bounce)){
			wildfire.renderer.drawString2d("Catch", Color.WHITE, new Point(0, 40), 2, 2);
			return new ControlsOutput().withBoost(false).withSteer((float)-Utils.aim(input.car, bounce) * 2F).withThrottle((float)-input.car.forwardMagnitude());
		}
		
		Vector2 enemyGoal = Utils.enemyGoal(input.car.team);
		Vector2 target = null;
		double velocityNeeded = -1;
		
		for(double length = (towardsOwnGoal ? 0.95 : (planSmartDodge ? 0.4 : 0.75)); length > (towardsOwnGoal ? 0.15 : 0); length -= 0.05){
			target = getNextPoint(input.car.position.flatten(), bounce, enemyGoal, length);
			
			// v = 2s / t - u
			double distance = getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, Color.YELLOW);
			double initialVelocity = input.car.magnitudeInDirection(target.minus(input.car.position.flatten()));
			double finalVelocity = (2 * distance) / timeLeft - initialVelocity;
			
			if(finalVelocity <= Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, input.car.boost))){
				velocityNeeded = distance / timeLeft;
				
				wildfire.renderer.drawCircle(Color.GREEN, target, Math.min(4D, timeLeft) * 25D);
				getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, Color.GREEN); //Redraw in pretty green!
				
				wildfire.renderer.drawString2d("Offset: " + (int)(length * 100) + "%", Color.WHITE, new Point(0, 40), 2, 2);
				wildfire.renderer.drawString2d("Distance: " + (int)distance + "uu", Color.WHITE, new Point(0, 60), 2, 2);
				wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
				
				break;
			}
		}
		
		boolean noPoint = (velocityNeeded == -1);
		if(noPoint) target = bounce;
		
		double steerRadians = Utils.aim(input.car, target);
	    float steer = (float)-steerRadians * 2F;
		
		//No point found
		if(noPoint){
			velocityNeeded = bounce.distance(input.car.position.flatten()) / timeLeft;			
			wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 40), 2, 2);
			
			boolean dribble = (input.ball.position.z > 110 && input.ball.position.distance(input.car.position) < 260);
			
			//Dodge
			if(!hasAction() && !dribble && !towardsOwnGoal && input.car.velocity.magnitude() > 950){
				if(Math.abs(steerRadians) < Math.PI * 0.2 && (input.car.position.distanceFlat(bounce) < 620 && input.ball.position.z < 300)){
					currentAction = new DodgeAction(this, steerRadians, input);
					if(!currentAction.failed) return currentAction.getOutput(input);
				}
			}
		}
		
		ControlsOutput controls = new ControlsOutput().withSteer(steer).withSlide(Math.abs(steerRadians) > 1.2 && input.car.position.distanceFlat(bounce) > 200);
		double currentVelocity = input.car.magnitudeInDirection(target.minus(input.car.position.flatten()));
			
		//Quick approach for a smart dodge
		if(planSmartDodge && Math.abs(steerRadians) < 0.7 && velocityNeeded < 300){
			controls.withThrottle(0);
		}else{
			if(velocityNeeded > currentVelocity){
				if(velocityNeeded > currentVelocity + 500 || velocityNeeded > 1410){
					controls.withThrottle(1).withBoost(Math.abs(steer) < 0.55F);
				}else{
					controls.withThrottle(1);
				}
			}else if(velocityNeeded < currentVelocity - 400){
				controls.withThrottle(-1);
			}else{
				controls.withThrottle(0);
			}
		}
		
	    return controls;
	}
	
	public double getPathwayDistance(Vector2 start, Vector2 bounce, Vector2 enemyGoal, double length, Color colour){
		double distance = 0;
		for(int i = 0; i < 16; i++){
			double dist = start.distance(bounce);
			if(i != 15){
				Vector2 next = getNextPoint(start, bounce, enemyGoal, length);
				
				double distanceStart = next.distance(start);
				double distanceBounce = next.distance(bounce);
				
				if(distanceBounce > desiredDistance){
					wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
					distance += start.distance(next);
					start = next;	
				}else{
					wildfire.renderer.drawLine3d(colour, start.toFramework(), start.plus(next.minus(start).scaledToMagnitude(dist - desiredDistance)).toFramework());
					distance += distanceStart + (distanceBounce - desiredDistance);
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
		double offset = length * bounce.distance(start) / 4;
		Vector2 target = bounce.plus(bounce.minus(enemyGoal).scaledToMagnitude(offset));
		target = start.plus(target.minus(start).scaled(0.28)).confine();
		
		//Clamp the X when stuck in goal
		if(Math.abs(start.y) > Utils.PITCHLENGTH){
			target = new Vector2(Math.max(-780, Math.min(780, target.x)), target.y);
		}
		
		return target;
	}

}
