package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
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
	private final double desiredDistance = 40D, desiredDistanceSmartDodge = 320D;
	
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
		double bounceDistance = bounce.distance(input.car.position.flatten());
		
		//Wall hit
		double wallDistance = Utils.distanceToWall(wildfire.impactPoint.getPosition());
		if(timeLeft > 1 && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < 1500 && wallDistance < 260 && Math.abs(wildfire.impactPoint.getPosition().x) > 1500){
			return false;
		}
		
		//Can't reach point (optimistic)
		if(bounceDistance / timeLeft > 2800) return false;
		
		//Teammate's closer
		if(Utils.isTeammateCloser(input, bounce)) return false;
		
		//Opponent's corner
		if(Utils.teamSign(input.car) * bounce.y > 4000 && Utils.enemyGoal(input.car.team).distance(bounce) > 1800 && !Utils.isInCone(input.car, bounce3, 1000)) return bounceDistance < 2400;
		
		return (Utils.isBallAirborne(input.ball) && input.ball.velocity.flatten().magnitude() < 5000) || (input.ball.position.z > 110 && input.ball.position.distanceFlat(input.car.position) < 220 && wallDistance > 450);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){		
		//Aerial
		boolean onTarget = Utils.isOnTarget(wildfire.ballPrediction, input.car.team);
		double impactRadians = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		if(!hasAction() && wildfire.impactPoint.getPosition().z > (onTarget && Math.abs(input.car.position.y) > 4500 ? 220 : 700) && Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()) && input.car.hasWheelContact && Math.abs(impactRadians) < (onTarget ? 0.42 : 0.32) && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < (onTarget ? -1500 : -2500)){
//		if(!hasAction() && timeLeft > 1.5 && (input.car.position.z > 120 || Math.abs(impactRadians) < 0.35)){
			currentAction = AerialAction.fromBallPrediction(this, input.car, wildfire.ballPrediction, wildfire.impactPoint.getPosition().z > 420);
//			currentAction = AerialAction2.fromBallPrediction(this, input.car, wildfire.ballPrediction, false);
			if(currentAction != null && !currentAction.failed){
				return currentAction.getOutput(input);
			}else{
				currentAction = null;
			}
		}

		wildfire.renderer.drawCircle(Color.YELLOW, bounce, Utils.BALLRADIUS);
		boolean towardsOwnGoal = Utils.isTowardsOwnGoal(input.car, bounce.withZ(0), 240);
		
		//Smart dodge (test)
		boolean planSmartDodge = false, smartDodgeCone = false;
		if(!towardsOwnGoal && !input.car.isSupersonic && input.car.hasWheelContact && input.car.position.z < 200 && input.ball.position.z > 180){
			if(Utils.closestOpponentDistance(input, bounce.withZ(0)) < 1500){
				planSmartDodge = true;
			}else if(bounce.distance(Utils.enemyGoal(input.car.team)) < 2500){
				planSmartDodge = true;
				smartDodgeCone = true;
			}else if(bounce.distance(Utils.homeGoal(input.car.team)) < 2500){
				planSmartDodge = true;
			} 
		}
		wildfire.renderer.drawString2d("Plan Smart Dodge: " + planSmartDodge, Color.WHITE, new Point(0, 40), 2, 2);
		if(planSmartDodge) renderJump(input.car);
		if(!hasAction() && planSmartDodge){
			currentAction = new SmartDodgeAction(this, input, smartDodgeCone);
			if(!currentAction.failed && ((SmartDodgeAction)currentAction).target.getPosition().z > 200){
				currentAction.getOutput(input);
			}else{
				currentAction = null;
			}
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
		double timeEffective = (planSmartDodge ? timeLeft - 0.34 : timeLeft);
		
		for(double length = (towardsOwnGoal ? 0.95 : (planSmartDodge ? 0.4 : 0.75)); length > (towardsOwnGoal ? 0.15 : 0); length -= 0.05){
			target = getNextPoint(input.car.position.flatten(), bounce, enemyGoal, length);
			
			double distance = getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, Color.YELLOW, (planSmartDodge ? desiredDistanceSmartDodge : desiredDistance));
			double initialVelocity = input.car.magnitudeInDirection(target.minus(input.car.position.flatten()));
			double finalVelocity = (2 * distance) / timeEffective - initialVelocity;
			
			if(finalVelocity <= Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, input.car.boost))){
				velocityNeeded = distance / timeEffective;
				
				wildfire.renderer.drawCircle(Color.GREEN, target, Math.min(4, Math.max(0.1, timeLeft)) * 30D);
				getPathwayDistance(input.car.position.flatten(), bounce, enemyGoal, length, Color.GREEN, (planSmartDodge ? desiredDistanceSmartDodge : desiredDistance)); //Redraw in pretty green!
				
				wildfire.renderer.drawString2d("Offset: " + (int)(length * 100) + "%", Color.WHITE, new Point(0, 60), 2, 2);
//				wildfire.renderer.drawString2d("Distance: " + (int)distance + "uu", Color.WHITE, new Point(0, 60), 2, 2);
//				wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
				
				break;
			}
		}
		
		boolean noPoint = (velocityNeeded == -1);
		if(noPoint) target = bounce;
		
		double steerRadians = Utils.aim(input.car, target);
	    float steer = (float)-steerRadians * 2F;
		
		//No point found
		if(noPoint){
			velocityNeeded = bounce.distance(input.car.position.flatten()) / timeEffective;			
			wildfire.renderer.drawString2d("Velocity Needed: " + (int)velocityNeeded + "uu/s", Color.WHITE, new Point(0, 60), 2, 2);
			
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
//		if(planSmartDodge && Math.abs(steerRadians) < 0.7 && velocityNeeded < 500){
//			controls.withThrottle(0);
//		}else{
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
//		}
		
	    return controls;
	}
	
	public double getPathwayDistance(Vector2 start, Vector2 bounce, Vector2 enemyGoal, double length, Color colour, double desiredD){
		double distance = 0;
		for(int i = 0; i < 16; i++){
			double dist = start.distance(bounce);
			if(i != 15){
				Vector2 next = getNextPoint(start, bounce, enemyGoal, length);
				
				double distanceStart = next.distance(start);
				double distanceBounce = next.distance(bounce);
				
				if(distanceBounce > desiredD){
					wildfire.renderer.drawLine3d(colour, start.toFramework(), next.toFramework());
					distance += start.distance(next);
					start = next;	
				}else{
					wildfire.renderer.drawLine3d(colour, start.toFramework(), start.plus(next.minus(start).scaledToMagnitude(dist - desiredD)).toFramework());
					distance += distanceStart + (distanceBounce - desiredD);
					break;
				}
			}else{
				//Final point
				wildfire.renderer.drawLine3d(colour, start.toFramework(), start.plus(bounce.minus(start).scaledToMagnitude(dist - desiredD)).toFramework());
				distance += (dist - desiredD); //We only need to reach the side of the ball
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
	
	private void renderJump(CarData car){
		Vector3 velocity = car.velocity.withZ(547.7225575); //Max jump velocity
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		continueRender(car, car.position, velocity);
	}
	
	private void continueRender(CarData car, Vector3 start, Vector3 velocity){
		final double scale = (1D / 40);
		if(start.isOutOfBounds()) return;	
		boolean up = (velocity.z > 0);
		velocity = velocity.plus(new Vector3(0, 0, -Utils.GRAVITY * scale)); //Gravity
		if(velocity.magnitude() > 2300) velocity.scaledToMagnitude(2300);
		Vector3 next = start.plus(velocity.scaled(scale));
		if(up && velocity.z <= 0) wildfire.renderer.drawCrosshair(car, next, Color.WHITE, 35);
		wildfire.renderer.drawLine3d((velocity.z > 0 ? Color.CYAN : Color.GRAY), start.toFramework(), next.toFramework());
		continueRender(car, next, velocity);
	}

}
