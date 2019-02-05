package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.input.CarOrientation;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

public class WallHitState extends State {
	
	private final double maxWallDistance = 250;

	public WallHitState(Wildfire wildfire){
		super("Wall Hit", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		//The ball must be in an appropriate wall hit position
		if(!isAppropriateWallHit(input.car, wildfire.impactPoint.getPosition())) return false;
		
		//We would like the hit to be constructive
		return Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < -4000 || Utils.teamSign(input.car) * (wildfire.impactPoint.getPosition().y - input.car.position.y) > -600;
	}
	
	@Override
	public boolean expire(DataPacket input){
		return input.car.position.z < 200 && !isAppropriateWallHit(input.car, wildfire.impactPoint.getPosition());
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		boolean wall = input.car.hasWheelContact && input.car.position.z > 180;
		
		if(wall){
			//Drive down the wall when the opportunity is gone (we might've hit it!)
			if(!isAppropriateWallHit(input.car, wildfire.impactPoint.getPosition())){
				wildfire.renderer.drawString2d("Abandon", Color.WHITE, new Point(0, 20), 2, 2);
				return Utils.driveDownWall(input);
			}else{
				wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.CYAN, 125);
				Vector3 localTarget = local(input.car, wildfire.impactPoint.getPosition());
				wildfire.renderer.drawString2d("Local = " + localTarget.toString(), Color.WHITE, new Point(0, 20), 2, 2);
				
				Vector2 forward = new Vector2(0, 1);
				double aim = forward.correctionAngle(localTarget.flatten());
				
				//Dodge
				if((localTarget.z > 130 || input.car.position.z > 1000) && wildfire.impactPoint.getPosition().distance(input.car.position) < (input.car.velocity.magnitude() > 750 ? 400 : 200)){
					currentAction = new DodgeAction(this, aim, input);
					if(!currentAction.failed){
						if(input.car.position.z > 1000) wildfire.sendQuickChat(QuickChatSelection.Reactions_Calculated);
						return currentAction.getOutput(input);
					}
				}
				
				return new ControlsOutput().withThrottle(1).withBoost(Math.abs(aim) < 0.3).withSteer((float)-aim * 2F).withSlide(false);
			}
		}else{
			//Drive towards the wall
			wildfire.renderer.drawString2d("Approach", Color.WHITE, new Point(0, 20), 2, 2);
			
			if(input.car.forwardMagnitude() > 2000){
				wildfire.sendQuickChat(QuickChatSelection.Information_IGotIt, QuickChatSelection.Information_Incoming);
			}
			
			boolean sideWall = (Utils.PITCHWIDTH - Math.abs(wildfire.impactPoint.getPosition().x) < maxWallDistance);
			boolean backWall = (Utils.PITCHLENGTH - Math.abs(wildfire.impactPoint.getPosition().y) < maxWallDistance);
			
			Vector2 destination = wildfire.impactPoint.getPosition().minus(input.car.position).scaled(100).plus(input.car.position).flatten();
			if(sideWall) destination = destination.withY(input.car.position.y);
			if(backWall) destination = destination.withX(Math.abs(wildfire.impactPoint.getPosition().x) > 1200 ? wildfire.impactPoint.getPosition().x : Math.signum(wildfire.impactPoint.getPosition().x) * 1200);
			
			float steer = (float)Utils.aim(input.car, destination);
			boolean reverse = false;
			
			//Dodging and half-flipping
			if(input.car.boost < 10 && !input.car.isSupersonic){
				//This should be replaced with the distance to the entry point, at some point
				double wallDistance = Utils.distanceToWall(input.car.position);
				
				if(wallDistance > Math.max(2500, 1.35 * input.car.velocity.magnitude())){ //Max = 3105
					if(Math.abs(steer) < 0.12){
						if(input.car.velocity.magnitude() > 900){
							currentAction = new DodgeAction(this, steer, input);
						}
					}else if(Math.abs(steer) > 2.6){
						reverse = true;
						if(input.car.forwardMagnitude() < -400){
							currentAction = new HalfFlipAction(this, input.elapsedSeconds);
						}
					}
					if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
				}
			}
			
			if(reverse) steer = (float)Utils.invertAim(steer);
			return new ControlsOutput().withThrottle(reverse ? -1 : 1).withBoost(Math.abs(steer) < 0.2).withSteer(-steer * 2F).withSlide(!reverse && Math.abs(steer) > Math.PI * 0.6);
		}
	}
	
	private boolean isAppropriateWallHit(CarData car, Vector3 target){
		if(Utils.distanceToWall(target) > maxWallDistance) return false;
		
		boolean backWall = (target.y * Utils.teamSign(car) < -4400);
		
		if(target.z < Math.max(backWall ? 250 : 400, car.position.distanceFlat(target) / 6)) return false;
		if(Math.abs(target.y) < 4350) return true;
		
		//Away from our back wall
		return (Math.abs(target.x) > 1200 || car.position.z > 900) && backWall; 
	}
	
	private Vector3 local(CarData car, Vector3 ball){
		Vector3 carPosition = car.position;
		CarOrientation carOrientation = car.orientation;
		Vector3 relative = ball.minus(carPosition);
		
		double localRight = carOrientation.rightVector.x * relative.x + carOrientation.rightVector.y * relative.y + carOrientation.rightVector.z * relative.z;
		double localNose = carOrientation.noseVector.x * relative.x + carOrientation.noseVector.y * relative.y + carOrientation.noseVector.z * relative.z;
		double localRoof = carOrientation.roofVector.x * relative.x + carOrientation.roofVector.y * relative.y + carOrientation.roofVector.z * relative.z;
		
		return new Vector3(localRight, localNose, localRoof);
	}

}
