package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
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
		if(Behaviour.isKickoff(input) || Behaviour.isCarAirborne(input.car) || Utils.teamSign(input.car) * input.ball.position.y > 2000 || input.info.impact.getPosition().z > 400){
			return false;
		}
		
		double impactYFlip = input.info.impact.getPosition().y * Utils.teamSign(input.car);
		
		// Near our backboard.
		if(!Behaviour.correctSideOfTarget(input.car, input.ball.position)){
			return impactYFlip < -(Constants.PITCHLENGTH - 340);
		}

		//Check if we have a shot opportunity
		if(!Behaviour.isOpponentBehindBall(input) && input.info.impact.getPosition().distanceFlat(input.car.position) < 3000){
			double aimBall = Handling.aim(input.car, input.info.impact.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.3 && impactYFlip > -3000){
				if(Behaviour.isInCone(input.car, input.info.impact.getPosition())) return false;
			}
		}
		
		onTarget  = Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team);
		if(!onTarget && Utils.teamSign(input.car.team) * input.ball.velocity.y > -1000 && Utils.teamSign(input.car.team) * input.ball.position.y > -4000) return false;
		
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
		if(impactPosition.z > 150 && offset != null){
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
			}
			
			return Handling.arriveAtSmartDodgeCandidate(car, candidate, wildfire.renderer);
		}
		
		double impactRadians = Handling.aim(car, input.info.impact.getPosition().flatten());
				
		// Dodge or half-flip
		if(Handling.canHandbrake(car)){
			boolean travellingToBall = (car.velocity.normalized().dotProduct(impactPosition.minus(car.position).normalized()) > 0.9 && car.forwardVelocityAbs > 1000);
			boolean backflip = (Math.abs(impactRadians) > 0.75 * Math.PI);
			
			if(travellingToBall ? (input.info.impact.getTime() < (backflip ? 0.23 : 0.29)) :
				(input.info.impactDistance < (backflip ? 120 : (input.car.position.z > 80 ? 120 : 160)))
					&& Math.abs(car.position.z - input.info.impact.getPosition().z) < 330){
				currentAction = new DodgeAction(this, impactRadians, input);
			}else if(car.forwardVelocity < -1000){
				currentAction = new HalfFlipAction(this, input);
			}else if(input.info.impact.getTime() > 2 && !car.isSupersonic && Handling.canHandbrake(car)
					&& car.forwardVelocity > (car.boost < 1 ? 1300 : 1500) && Math.abs(impactRadians) < 0.18){
				//Front flip for speed
				currentAction = new DodgeAction(this, 0, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		// Aerial
		double ballSpeedAtCar = input.ball.velocity.magnitude() * Math.cos(input.ball.velocity.flatten().correctionAngle(car.position.minus(input.ball.position).flatten())); 
		if(car.hasWheelContact && input.info.impact.getPosition().z > (ballSpeedAtCar > 1700 ? 300 : 500) && input.info.impact.getPosition().y * Utils.teamSign(car) < 0 && car.position.z < 140){
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
		if(car.position.y * Utils.teamSign(car) > -5050 && Math.abs(impactRadians) > 0.4 * Math.PI && Math.abs(impactRadians) < 0.8 * Math.PI){
			//We don't want to wait too long for the ball to reach us
			double ballTime = Math.abs((car.position.y - input.ball.position.y) / input.ball.velocity.y);
			Vector2 intersect = Utils.traceToY(input.ball.position.flatten(), input.ball.velocity.flatten(), car.position.y);
			if(ballTime < 1.5 && intersect != null){
				// Check if our X-coordinate is close-by when we should intersect with the ball's path.
				double xDifference = Math.abs(car.position.x - intersect.x);
				boolean closeby = (xDifference < 110);
				wildfire.renderer.drawLine3d(closeby ? Color.CYAN : Color.BLUE, input.ball.position.flatten().toFramework(), intersect.toFramework());
				wildfire.renderer.drawString2d("Stop" + (closeby ? " (" + (int)xDifference + ")" : ""), Color.WHITE, new Point(0, 20), 2, 2);
				if(closeby){
					wildfire.sendQuickChat(QuickChatSelection.Information_InPosition);
					return stayStill(input);
				}else if(xDifference < 900 && car.velocity.magnitude() < 1300){
					return drivePoint(input, intersect, true);
				}
			}
		}
		
		double offsetMagnitude = (65 + 35 * Math.pow(input.info.impact.getPosition().minus(car.position).normalized().y, 2));
		if(Behaviour.correctSideOfTarget(car, input.info.impact.getPosition().flatten())){
			Vector2 goal = Behaviour.getTarget(car, input.ball);
			offset = input.info.impact.getPosition().flatten().minus(goal).scaledToMagnitude(offsetMagnitude);
		}else{
			// Back-wall.
			if(Math.abs(input.info.impact.getPosition().y) > 4600){
				if(Math.abs(input.info.impact.getPosition().x) > Constants.GOALHALFWIDTH + 20){
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
		
		return drivePoint(input, input.info.impact.getPosition().flatten().plus(offset), input.info.impact.getTime() < 1);
	}
	
	private ControlsOutput drivePoint(InfoPacket input, Vector2 point, boolean allowBackwards){
		if(Math.abs(input.car.position.y) > Constants.PITCHLENGTH) point = point.withX(Utils.clamp(point.x, -700, 700));
		
		boolean rush = (input.car.forwardVelocity > 700);
		
		double steer = Handling.aim(input.car, point);
		boolean reverse = (Math.cos(steer) < 0 && allowBackwards);
		
		double throttle;
		if(Handling.insideTurningRadius(input.car, point) && !reverse){
			double maxVelocity = Math.min(DrivePhysics.maxVelForTurn(input.car, point.withZ(input.car.position.z)), Constants.SUPERSONIC);
			double acceleration = (maxVelocity - input.car.forwardVelocity) / 0.025;
			throttle = Handling.produceAcceleration(input.car, acceleration);
		}else{
			throttle = (rush ? 1 : (reverse ? -1 : 1));
		}
		
		if(reverse){
			steer = Utils.invertAim(steer);
		}else{
			steer = -steer;
		}
		
		return new ControlsOutput().withThrottle(throttle).withBoost(!reverse && Math.abs(steer) < (rush ? 0.3 : 0.2) && !input.car.isSupersonic)
				.withSteer(steer * 3).withSlide(Math.abs(steer) > 1.2 && Handling.canHandbrake(input.car));
	}
	
	private ControlsOutput stayStill(InfoPacket input){
		return new ControlsOutput().withThrottle((float)-input.car.forwardVelocity / 80).withBoost(false);
	}

}