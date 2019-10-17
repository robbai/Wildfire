package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class WallHitState extends State {
	
	private final double maxWallDistance = 230;

	public WallHitState(Wildfire wildfire){
		super("Wall Hit", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(Math.abs(input.car.position.y) > Constants.PITCH_LENGTH + 10) return false;
		
		// The ball must be in an appropriate wall hit position.
		if(!isAppropriateWallHit(input.car, input.info.impact.getPosition())) return false;
		
		// We would like the hit to be constructive
		return input.car.sign * input.info.impact.getPosition().y < -4000 || 
				input.car.sign * (input.info.impact.getPosition().y - input.car.position.y) > (Behaviour.hasTeammate(input) ? -900 : -600);
	}
	
	@Override
	public boolean expire(InfoPacket input){
		if(Math.abs(input.car.position.y) > Constants.PITCH_LENGTH + 10) return false;
		return !isAppropriateWallHit(input.car, input.info.impact.getPosition());
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		boolean wall = (car.hasWheelContact && car.position.z > 150);
		
		if(wall){
			// Drive down the wall when the opportunity is gone (we might've hit it!).
			if(!isAppropriateWallHit(car, input.info.impact.getPosition())){
				wildfire.renderer.drawString2d("Abandon", Color.WHITE, new Point(0, 20), 2, 2);
				
				if(input.info.impact.getTime() > 4 && (car.position.z > 1000 || car.velocity.magnitude() < 600)){
					currentAction = new HopAction(this, input, input.info.impact.getPosition().flatten());
					return currentAction.getOutput(input);
				}
				
				return Handling.driveDownWall(input);
			}else{
				wildfire.renderer.drawCrosshair(car, input.info.impact.getPosition(), Color.CYAN, 125);
				
				Vector3 target = input.info.impact.getPosition();
				if(Math.abs(target.y) < Constants.PITCH_LENGTH - 800){
					target = target.plus(new Vector3(0, 85 * -car.sign, 0));
				}else{
					target = target.plus(new Vector3(75 * Math.signum(car.position.x - input.info.impact.getPosition().x), 0, 0));
				}
								
				double radians = Handling.aim(car, target);
				
				// Dodge.
				Vector3 localTarget = Utils.toLocal(car, input.info.impact.getPosition());
				boolean highCar = (car.position.z > 1000);
				if((localTarget.z > 85 || highCar) && localTarget.flatten().magnitude() < (car.velocity.magnitude() > 950 ? 380 : 270) && Math.abs(radians) < Math.toRadians(60)){
//				if((localTarget.z > 85 || highCar) && input.info.impact.getTime() < Behaviour.IMPACT_DODGE_TIME - 0.05 && Math.abs(radians) < Math.toRadians(60)){
					currentAction = new DodgeAction(this, radians, input);
					if(!currentAction.failed){
						if(highCar) wildfire.sendQuickChat(QuickChatSelection.Reactions_Calculated);
						return currentAction.getOutput(input);
					}
				}
				
//				return new ControlsOutput().withThrottle(1).withBoost(Math.abs(radians) < 0.16).withSteer(radians * -4).withSlide(Math.abs(radians) > 1.2 && Handling.canHandbrake(car));
				return Handling.forwardDrive(car, target, false);
			}
		}else{
			/*
			 *  Drive towards the wall.
			 */
			
			wildfire.renderer.drawString2d("Approach", Color.WHITE, new Point(0, 20), 2, 2);
			
			if(car.forwardVelocity > 2000){
				wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Information_Incoming);
			}
			
			boolean sideWall = (Constants.PITCH_WIDTH - Math.abs(input.info.impact.getPosition().x) < maxWallDistance + 50);
			boolean backWall = (Constants.PITCH_LENGTH - Math.abs(input.info.impact.getPosition().y) < maxWallDistance + 50) || !sideWall;
			wildfire.renderer.drawString2d((sideWall ? "Side" : (backWall ? "Back" : "Unknown")) + " Wall", Color.WHITE, new Point(0, 40), 2, 2);
			
			// Define the destination.
			Vector2 destination = Utils.traceToWall(input.car.position.flatten(), input.info.impact.getBallPosition().minus(input.car.position).flatten());
			if(sideWall){
				if(Behaviour.correctSideOfTarget(car, input.info.impact.getPosition())){
					destination = destination.withY(Utils.lerp(car.position.y, input.info.impact.getPosition().y, 0.1));
				}else{
					destination = destination.withY(input.info.impact.getPosition().y - car.sign * 150);
				}
			}
			destination = destination.withX(Math.max(Math.abs(input.info.impact.getPosition().x), 1200) * Math.signum(input.info.impact.getPosition().x));
			
			if(car.position.distanceFlat(destination) < 1200){
				if(sideWall) destination = destination.withX(destination.x * 1.5);
				if(backWall) destination = destination.withY(destination.y * 1.5);
			}
			
			double radians = Handling.aim(car, destination);
			boolean reverse = false;
			
			// Dodging and half-flipping.
			if(car.hasWheelContact){
				double destinationDistance = car.position.distanceFlat(destination.confine());
				
//				// Aerial from our back wall.
//				if(backWall && Math.abs(radians) < 0.1 && !car.isSupersonic && destinationDistance > 1000){
//					AerialAction aerial = AerialAction.fromBallPrediction(this, car, wildfire.ballPrediction, destinationDistance < 2000);
//					if(aerial != null) currentAction = aerial;
//				}
				
				if(currentAction == null && destinationDistance > Math.max(2500, 1.35 * car.forwardVelocityAbs)){
					if(Math.abs(radians) < 0.12){
						if(car.forwardVelocity > 900 && car.forwardVelocity < 2000 && car.boost < 15){
							currentAction = new DodgeAction(this, radians, input);
						}
					}else if(Math.abs(radians) > 2.6){
						reverse = true;
						if(car.forwardVelocity < -500){
							currentAction = new HalfFlipAction(this, input);
						}
					}
				}
				
				if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
				currentAction = null;
			}
			
			// Controls.
			if(reverse) radians = Utils.invertAim(radians);
			return new ControlsOutput().withThrottle(reverse ? -1 : 1).withBoost(Math.abs(radians) < 0.1 && !car.isSupersonic)
					.withSteer(-radians * 3F).withSlide(!reverse && Math.abs(radians) > Math.PI * 0.6);
		}
	}
	
	private boolean isAppropriateWallHit(CarData car, Vector3 target){
		if(Utils.distanceToWall(target) > maxWallDistance) return false;
		
		boolean backWall = (target.y * car.sign < -4400);
		
		if(target.z < Math.max(backWall ? 330 : 400, car.position.distanceFlat(target) / 5)) return false;
		if(Math.abs(target.y) < 4350) return true;
		
		// Away from our back wall.
		return (Math.abs(target.x) > 1000 || car.position.z > 900) && backWall; 
	}

}
