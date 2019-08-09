package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class WallHitState extends State {
	
	private final double maxWallDistance = 230;

	public WallHitState(Wildfire wildfire){
		super("Wall Hit", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		// The ball must be in an appropriate wall hit position.
		if(!isAppropriateWallHit(input.car, wildfire.impactPoint.getPosition())) return false;
		
		// We would like the hit to be constructive
		return Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < -4000 || 
				Utils.teamSign(input.car) * (wildfire.impactPoint.getPosition().y - input.car.position.y) > (Behaviour.hasTeammate(input) ? -900 : -600);
	}
	
	@Override
	public boolean expire(DataPacket input){
		return input.car.position.z < 200 && !isAppropriateWallHit(input.car, wildfire.impactPoint.getPosition());
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		CarData car = input.car;
		
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		boolean wall = car.hasWheelContact && car.position.z > 180;
		
		if(wall){
			// Drive down the wall when the opportunity is gone (we might've hit it!).
			if(!isAppropriateWallHit(car, wildfire.impactPoint.getPosition())){
				wildfire.renderer.drawString2d("Abandon", Color.WHITE, new Point(0, 20), 2, 2);
				
				if(car.velocity.z < -500 && wildfire.impactPoint.getTime() > 4){
					currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
					return currentAction.getOutput(input);
				}
				
				return Handling.driveDownWall(input);
			}else{
				wildfire.renderer.drawCrosshair(car, wildfire.impactPoint.getPosition(), Color.CYAN, 125);
				Vector3 target = wildfire.impactPoint.getPosition();
				if(Math.abs(target.y) < Constants.PITCHLENGTH - 800) target = target.plus(new Vector3(0, 85 * -Utils.teamSign(car), 0));
								
				double aim = Handling.aim(car, target);
				
				// Dodge.
				Vector3 localTarget = Utils.toLocal(car, wildfire.impactPoint.getPosition());
				boolean highCar = (car.position.z > 1000);
				if((localTarget.z > 85 || highCar) && wildfire.impactPoint.getPosition().distance(car.position) < (car.velocity.magnitude() > 950 ? 460 : 320)){
					currentAction = new DodgeAction(this, aim, input);
					if(!currentAction.failed){
						if(highCar) wildfire.sendQuickChat(QuickChatSelection.Reactions_Calculated);
						return currentAction.getOutput(input);
					}
				}
				
				return new ControlsOutput().withThrottle(1).withBoost(Math.abs(aim) < 0.2).withSteer(-aim * 3).withSlide(Math.abs(aim) > 1.2 && Handling.canHandbrake(car));
			}
		}else{
			// Drive towards the wall
			wildfire.renderer.drawString2d("Approach", Color.WHITE, new Point(0, 20), 2, 2);
			
			if(car.forwardVelocity > 2000){
				wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Information_Incoming);
			}
			
			boolean sideWall = (Constants.PITCHWIDTH - Math.abs(wildfire.impactPoint.getPosition().x) < maxWallDistance + 50);
			boolean backWall = (Constants.PITCHLENGTH - Math.abs(wildfire.impactPoint.getPosition().y) < maxWallDistance + 50) || !sideWall;
			wildfire.renderer.drawString2d((sideWall ? "Side" : (backWall ? "Back" : "Unknown")) + " Wall", Color.WHITE, new Point(0, 40), 2, 2);
			
			Vector2 destination = wildfire.impactPoint.getPosition().minus(car.position).scaled(100).plus(car.position).flatten();
			if(sideWall) destination = destination.withY(car.position.y);
			if(backWall) destination = destination.withX(Math.max(Math.abs(wildfire.impactPoint.getPosition().x), 1000) * Math.signum(wildfire.impactPoint.getPosition().x));
			
			float steer = (float)Handling.aim(car, destination);
			boolean reverse = false;
			
			// Dodging and half-flipping
			if(car.boost < 10 && !car.isSupersonic){
				//This should be replaced with the distance to the entry point, at some point
				double wallDistance = Utils.distanceToWall(car.position);
				
				Vector3 carVelocity = car.velocity;
				
				// Aerial from our back wall
				if(backWall && Math.abs(steer) < 0.1 && !car.isSupersonic && wallDistance > 1000){
					AerialAction aerial = AerialAction.fromBallPrediction(this, car, wildfire.ballPrediction, wallDistance < 2000);
					if(aerial != null) currentAction = aerial;
				}
				
				if(wallDistance > Math.max(2500, 1.35 * carVelocity.magnitude())){ //Max = 3105
					if(Math.abs(steer) < 0.12){
						if(carVelocity.magnitude() > 900){
							currentAction = new DodgeAction(this, steer, input);
						}
					}else if(Math.abs(steer) > 2.6){
						reverse = true;
						if(carVelocity.flatten().magnitudeInDirection(car.orientation.noseVector.flatten()) < -400){
							currentAction = new HalfFlipAction(this, input.elapsedSeconds);
						}
					}
					if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
					currentAction = null;
				}
			}
			
			if(reverse) steer = (float)Utils.invertAim(steer);
			return new ControlsOutput().withThrottle(reverse ? -1 : 1).withBoost(Math.abs(steer) < 0.1).withSteer(-steer * 3F).withSlide(!reverse && Math.abs(steer) > Math.PI * 0.6);
		}
	}
	
	private boolean isAppropriateWallHit(CarData car, Vector3 target){
		if(Utils.distanceToWall(target) > maxWallDistance) return false;
		
		boolean backWall = (target.y * Utils.teamSign(car) < -4400);
		
		if(target.z < Math.max(backWall ? 330 : 400, car.position.distanceFlat(target) / 5)) return false;
		if(Math.abs(target.y) < 4350) return true;
		
		// Away from our back wall.
		return (Math.abs(target.x) > 1000 || car.position.z > 900) && backWall; 
	}

}
