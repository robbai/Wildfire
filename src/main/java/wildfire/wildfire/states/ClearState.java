package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class ClearState extends State {
	
	private final double homeZoneSize = 3500D;
	
	private boolean onTarget = false;
	private Vector2 offset = null;

	public ClearState(Wildfire wildfire){
		super("Clear", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		// General conditions.
		if(Behaviour.isKickoff(input) || Behaviour.isCarAirborne(input.car) || Utils.teamSign(input.car) * input.ball.position.y > 2000 || wildfire.impactPoint.getPosition().z > 400){
			return false;
		}
		
		double impactYFlip = wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car);
		
		// Near our backboard.
		if(!Behaviour.correctSideOfTarget(input.car, input.ball.position)){
			return impactYFlip < -(Constants.PITCHLENGTH - 340);
		}

		//Check if we have a shot opportunity
		if(!Behaviour.isOpponentBehindBall(input) && wildfire.impactPoint.getPosition().distanceFlat(input.car.position) < 3000){
			double aimBall = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			if(Math.abs(aimBall) < Math.PI * 0.3 && impactYFlip > -3000){
				if(Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition())) return false;
			}
		}
		
		onTarget  = Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team);
		if(!onTarget && Utils.teamSign(input.car.team) * input.ball.velocity.y > -1000 && Utils.teamSign(input.car.team) * input.ball.position.y > -4000) return false;
		
		return Behaviour.defendNotReturn(input, wildfire.impactPoint.getPosition(), homeZoneSize, onTarget);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		// Drive down the wall.
		boolean onWall = Behaviour.isOnWall(input.car);
		if(onWall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}
		
		// Smart-dodge.
		if(!hasAction() && Behaviour.isBallAirborne(input.ball) && offset != null){
			currentAction = new SmartDodgeAction(this, input, false);
			if(!currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
			
			Vector2 trace = Utils.traceToWall(input.car.position.flatten(), wildfire.impactPoint.getPosition().plus(offset.withZ(0)).minus(input.car.position).flatten());
			PredictionSlice candidate = SmartDodgeAction.getCandidateLocation(wildfire.ballPrediction, input.car, trace);
			if(candidate != null) return Handling.arriveAtSmartDodgeCandidate(input.car, candidate, wildfire.renderer);
		}
		
		double angleImpact = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
				
		// Dodge or half-flip
		if(!hasAction() && !input.car.isDrifting()){
			double impactDistance = input.car.position.distanceFlat(wildfire.impactPoint.getPosition());
			double forwardVelocity = input.car.forwardMagnitude();
			
			boolean likelyBackflip = (Math.abs(angleImpact) > 0.75 * Math.PI);
//			boolean chip = (Math.abs(angleImpact) < 0.1 && forwardVelocity > 1600 && !input.car.isDrifting() &&
//					wildfire.impactPoint.getPosition().minus(input.car.position).normalized().y * Utils.teamSign(input.car) > 0.9);
			
			if(impactDistance < (likelyBackflip ? 290 : (input.car.position.z > 80 ? 250 : 290))
					&& Math.abs(input.car.position.z - wildfire.impactPoint.getPosition().z) < 350){
//				if(!chip) currentAction = new DodgeAction(this, angleImpact * (forwardVelocity > 1200 && !likelyBackflip ? 2 : 1), input);
				currentAction = new DodgeAction(this, angleImpact * (forwardVelocity > 1200 && !likelyBackflip ? 1.75 : 1), input);
			}else if(impactDistance > (onTarget ? 3500 : 2200) && forwardVelocity < -900){
				currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			}else if(wildfire.impactPoint.getTime() > 1.8 && !input.car.isSupersonic 
					&& input.car.forwardMagnitude() > (input.car.boost == 0 ? 1200 : 1500) && Math.abs(angleImpact) < 0.25){
				//Front flip for speed
				currentAction = new DodgeAction(this, 0, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		// Aerial
		double ballSpeedAtCar = input.ball.velocity.magnitude() * Math.cos(input.ball.velocity.flatten().correctionAngle(input.car.position.minus(input.ball.position).flatten())); 
		if(!hasAction() && input.car.hasWheelContact && wildfire.impactPoint.getPosition().z > (ballSpeedAtCar > 1700 ? 300 : 500) && wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < 0 && input.car.position.z < 140){
			double maxRange = wildfire.impactPoint.getPosition().z * 4;
			double minRange = wildfire.impactPoint.getPosition().z * 1.3;
			if(Utils.isPointWithinRange(input.car.position.flatten(), wildfire.impactPoint.getPosition().flatten(), minRange, maxRange)){
				currentAction = AerialAction.fromBallPrediction(this, input.car, wildfire.ballPrediction, wildfire.impactPoint.getPosition().z > 800);
				if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
				currentAction = null;
			}
		}
		
		wildfire.renderer.drawCircle(Color.GREEN, Constants.homeGoal(input.car.team), homeZoneSize);
		
		// We are in position for the ball to hit us (and we can't quickly turn towards the ball)
		if(input.car.position.y * Utils.teamSign(input.car) > -5050 && Math.abs(angleImpact) > 0.4 * Math.PI && Math.abs(angleImpact) < 0.8 * Math.PI){
			//We don't want to wait too long for the ball to reach us
			double ballTime = Math.abs((input.car.position.y - input.ball.position.y) / input.ball.velocity.y);
			Vector2 intersect = Utils.traceToY(input.ball.position.flatten(), input.ball.velocity.flatten(), input.car.position.y);
			if(ballTime < 1.5 && intersect != null){
				//Check if our X-coordinate is close-by when we should intersect with the ball's path
				double xDifference = Math.abs(input.car.position.x - intersect.x);
				boolean closeby = (xDifference < 120);
				wildfire.renderer.drawLine3d(closeby ? Color.CYAN : Color.BLUE, input.ball.position.flatten().toFramework(), intersect.toFramework());
				wildfire.renderer.drawString2d("Stop" + (closeby ? " (" + (int)xDifference + ")" : ""), Color.WHITE, new Point(0, 20), 2, 2);
				if(closeby){
					wildfire.sendQuickChat(QuickChatSelection.Information_InPosition);
					return stayStill(input);
				}else if(xDifference < 800 && input.car.velocity.magnitude() < 1200){
					return drivePoint(input, intersect);
				}
			}
		}
		
		double offsetMagnitude = (75 + 40 * Math.pow(wildfire.impactPoint.getPosition().minus(input.car.position).normalized().y, 2));
		if(Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition().flatten())){
			Vector2 goal = Behaviour.getTarget(input.car, input.ball);
			offset = wildfire.impactPoint.getPosition().flatten().minus(goal).scaledToMagnitude(offsetMagnitude);
		}else{
			if(Math.abs(wildfire.impactPoint.getPosition().y) > 4600) offsetMagnitude += 10; // Back-wall bonus.
			offset  = new Vector2(offsetMagnitude * 
					-Math.signum(Utils.traceToWall(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten()).x), 
					0);
		}
		
		//Tad bit o' rendering
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 140);
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition().plus(offset.withZ(0)), Color.RED, 40);
		
		return drivePoint(input, wildfire.impactPoint.getPosition().flatten().plus(offset));
	}
	
	private ControlsOutput drivePoint(DataPacket input, Vector2 point){
		if(Math.abs(input.car.position.y) > Constants.PITCHLENGTH) point.withX(Utils.clamp(point.x, -700, 700));
		
		boolean rush = input.car.forwardMagnitude() > 700;
		
		float steer = (float)Handling.aim(input.car, point);
		
		float throttle;
		if(Handling.insideTurningRadius(input.car, point)){
			throttle = (float)(-input.car.forwardMagnitude() / 1000);
		}else{
			throttle = (rush ? 1 : (float)Math.signum(Math.cos(steer)));
		}
		boolean reverse = (throttle < 0);
		
		if(reverse){
			steer = (float)Utils.invertAim(steer);
		}else{
			steer = -steer;
		}
		
		return new ControlsOutput().withThrottle(throttle).withBoost(!reverse && Math.abs(steer) < (rush ? 0.3 : 0.2) && !input.car.isSupersonic)
				.withSteer(steer * 3F).withSlide(Math.abs(steer) > Math.PI * 0.4 && !input.car.isDrifting());
	}
	
	private ControlsOutput stayStill(DataPacket input){
		return new ControlsOutput().withThrottle((float)-input.car.forwardMagnitude() / 80).withBoost(false);
	}

}