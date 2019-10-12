package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class ClearState extends State {
	
	private final double homeZoneSize = 3500D;
	
	private boolean onTarget = false;
	private Vector2 offset = null;

	public ClearState(Wildfire wildfire){
		super("Clear", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		// General conditions.
		if(input.gameInfo.isMatchEnded() || Behaviour.isKickoff(input) || Behaviour.isCarAirborne(input.car) || input.car.sign * input.ball.position.y > 2000 || input.info.impact.getPosition().z > 400){
			return false;
		}
		
		if(Behaviour.isTowardsOwnGoal(input.car, input.info.impact.getPosition())) return false;
		
		double impactYFlip = input.info.impact.getPosition().y * input.car.sign;
		
		// Near our backboard.
		if(!Behaviour.correctSideOfTarget(input.car, input.ball.position)){
			return impactYFlip < -(Constants.PITCH_LENGTH - 340);
		}

		// Check if we have a shot opportunity.
		if(!Behaviour.isOpponentBehindBall(input) && input.info.impact.getPosition().distanceFlat(input.car.position) < 3000){
			double aimBall = Handling.aim(input.car, input.info.impact.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.3 && impactYFlip > -3000){
				if(Behaviour.isInCone(input.car, input.info.impact.getPosition())) return false;
			}
		}
		
		onTarget  = Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team);
		if(onTarget){
			return true;
		}else if(input.car.sign * input.ball.velocity.y > -1000 && input.car.sign * input.ball.position.y > -4000){
			return false;
		}
		
		return Behaviour.defendNotReturn(input, input.info.impact.getPosition(), homeZoneSize, onTarget);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		// Drive down the wall.
		boolean wall = Behaviour.isOnWall(car);
		if(wall && input.info.impact.getTime() > 0.9){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		// Smart-dodge.
		Vector3 impactPosition = input.info.impact.getPosition();
		if(input.info.impact.getTime() < 3 && Utils.toLocal(input.car, input.info.impact.getBallPosition()).z > 180 && car.onFlatGround){
//			Vector2 trace = Utils.traceToWall(car.position.flatten(), input.info.impact.getPosition().plus(offset.withZ(0)).minus(car.position).flatten());
//			Slice candidate = SmartDodgeAction.getCandidateLocation(wildfire.ballPrediction, car, trace);
			Slice candidate = input.info.jumpImpact;
			if(candidate == null || candidate.getFrame() < input.info.impact.getFrame() - (0.2 * 60)) candidate = input.info.impact;
			if(candidate != null){
				currentAction = new SmartDodgeAction(this, input);
				if(currentAction != null && !currentAction.failed){
					return currentAction.getOutput(input);
				}
				currentAction = null;
				
				if((candidate.getPosition().y - car.position.y) * car.sign > -1000 || !Behaviour.isOnTarget(wildfire.ballPrediction, car.team)){
					return Handling.arriveAtSmartDodgeCandidate(car, candidate, wildfire.renderer);
				}
			}
		}
		
		double impactRadians = Handling.aim(car, input.info.impact.getPosition());
				
		// Dodge or half-flip
		if(car.hasWheelContact){
			double dodgeRadians = Handling.aim(car, input.ball.position);
			if(Math.abs(impactRadians - dodgeRadians) < Math.toRadians(90)){
				dodgeRadians = Utils.clamp(impactRadians, -Math.PI, Math.PI);
			}
			
			boolean travellingToBall = (car.velocity.normalized().dotProduct(impactPosition.minus(car.position).normalized()) > 0.8 && car.forwardVelocityAbs > 700);
//			boolean backflip = (Math.abs(dodgeRadians) > 0.75 * Math.PI);
			
			if((input.info.impact.getTime() < (travellingToBall ? Behaviour.IMPACT_DODGE_TIME : Behaviour.IMPACT_DODGE_TIME - 0.08))
					&& Math.abs(car.position.z - input.info.impact.getPosition().z) < 350){
				currentAction = new DodgeAction(this, dodgeRadians, input);
			}else if(car.forwardVelocity < -500 && Math.abs(impactRadians) > Math.toRadians(150)){
				currentAction = new HalfFlipAction(this, input);
			}else if(car.forwardVelocity < 2000 && car.forwardVelocity > (car.boost < 1 ? 1300 : 1500)
					&& Math.abs(impactRadians) < 0.1 && Behaviour.dodgeDistance(input.car) < input.info.impactDistance){
				// Front flip for speed.
				currentAction = new DodgeAction(this, 0, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		// Aerial.
		double ballSpeedAtCar = input.ball.velocity.dotProduct(car.position.minus(input.ball.position).withZ(0)); 
		if(car.hasWheelContact && input.info.impact.getPosition().z > (ballSpeedAtCar > 1700 ? 300 : 500) && input.info.impact.getPosition().y * car.sign < 0 && car.position.z < 140){
			double maxRange = input.info.impact.getPosition().z * 4;
			double minRange = input.info.impact.getPosition().z * 1.3;
			if(Utils.isPointWithinRange(car.position.flatten(), input.info.impact.getPosition().flatten(), minRange, maxRange)){
				currentAction = AerialAction.fromBallPrediction(this, car, wildfire.ballPrediction, input.info.impact.getPosition().z > 800);
				if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
				currentAction = null;
			}
		}
		
		wildfire.renderer.drawCircle(Color.GREEN, Constants.homeGoal(car.team), homeZoneSize);
		
		// We are in position for the ball to hit us (and we can't quickly turn towards the ball).
		if(car.onFlatGround && car.position.y * car.sign > -Constants.PITCH_LENGTH + 200 && Math.abs(impactRadians) > Math.toRadians(55)){
			// We don't want to wait too long for the ball to reach us.
			double ballTime = Math.abs((car.position.y - input.ball.position.y) / input.ball.velocity.y);
			Vector2 intersect = Utils.traceToY(input.ball.position.flatten(), input.ball.velocity.flatten(), car.position.y);
			if(ballTime < 1.5 && intersect != null){
				// Check if our X-coordinate is close-by when we should intersect with the ball's path.
				double xDifference = Math.abs(car.position.x - intersect.x);
				boolean closeby = (xDifference < 0.45 * Utils.lerp(Constants.RIPPER.y, Constants.RIPPER.x, Math.abs(car.orientation.forward.flatten().normalized().x)));
				wildfire.renderer.drawLine3d(closeby ? Color.CYAN : Color.BLUE, input.ball.position.flatten(), intersect);
				wildfire.renderer.drawString2d("Stop" + (closeby ? " (" + (int)xDifference + "uu)" : ""), Color.WHITE, new Point(0, 20), 2, 2);
				if(closeby){
					wildfire.sendQuickChat(QuickChatSelection.Information_InPosition);
					return Handling.stayStill(car);
				}else if(xDifference < 700 && car.velocity.magnitude() < 1500){
					return drivePoint(car, intersect.withZ(input.info.impact.getBallPosition().z), true);
				}
			}
		}
		
		double offsetMagnitude = (65 + 45 * Math.pow(input.info.impact.getPosition().minus(car.position).normalized().y, 2));
		if(Behaviour.correctSideOfTarget(car, input.info.impact.getPosition().flatten())){
			Vector2 goal = Behaviour.getTarget(car, input.ball);
			offset = input.info.impact.getPosition().flatten().minus(goal).scaledToMagnitude(offsetMagnitude);
		}else{
			// Back-wall.
			if(Math.abs(input.info.impact.getPosition().y) > 4600){
				if(Math.abs(input.info.impact.getPosition().x) > Constants.GOAL_WIDTH + 20){
					offsetMagnitude += 8;
				}else{
					offsetMagnitude -= 4;
				}
			}
			
			offset  = new Vector2(offsetMagnitude * 
					-Math.signum(Utils.traceToWall(car.position.flatten(), input.info.impact.getPosition().minus(car.position).flatten()).x), 
					0);
		}
		
		// Render the target.
		wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition(), Color.MAGENTA, 140);
		wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition().plus(offset.withZ(0)), Color.RED, 40);
		
		return drivePoint(car, input.info.impact.getPosition().plus(offset.withZ(0)), input.info.impact.getTime() < 1);
	}
	
	private ControlsOutput drivePoint(CarData car, Vector3 destination, boolean allowBackwards){
		if(Math.abs(car.position.y) > Constants.PITCH_LENGTH) destination = destination.withX(Utils.clamp(destination.x, -700, 700));
		
		boolean rush = (car.forwardVelocity > 700);
		
		double radians = Handling.aim(car, destination);
		boolean reverse = (Math.cos(radians) < 0 && allowBackwards);
		
		double throttle;
		if(Handling.insideTurningRadius(car, destination) && !reverse){
			double maxVelocity = Math.min(DrivePhysics.maxVelForTurn(car, destination.withZ(car.position.z)), Constants.SUPERSONIC_VELOCITY);
			double acceleration = (maxVelocity - car.forwardVelocity) / 0.025;
			throttle = Handling.produceAcceleration(car, acceleration);
		}else{
			throttle = (rush || !reverse ? 1 : -1);
		}
		
		ControlsOutput controls = (reverse ? Handling.steeringBackwards(car, destination) : Handling.steering(car, destination));
		return controls.withThrottle(throttle).withBoost(controls.holdBoost() && Math.abs(radians) < (rush ? 0.3 : 0.2));
	}

}