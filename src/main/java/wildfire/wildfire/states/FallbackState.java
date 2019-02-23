package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.obj.State;

public class FallbackState extends State {
	
	private int targetPly = 6;

	public FallbackState(Wildfire wildfire){
		super("Fallback", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		boolean wall = Utils.isOnWall(input.car);
		double distance = wildfire.impactPoint.getPosition().distance(input.car.position);
		
		//Goal
		Vector2 goal = Utils.getTarget(input.car, input.ball);
		wildfire.renderer.drawCrosshair(input.car, goal.withZ(Utils.BALLRADIUS), Color.WHITE, 125);				
		
		if(!hasAction()){
			double velocityTowardsImpact = input.car.magnitudeInDirection(wildfire.impactPoint.getPosition().minus(input.car.position).flatten());
			if(distance < (wall ? 270 : (input.car.isSupersonic ? 800 : 600)) && velocityTowardsImpact > 900){
				if(wildfire.impactPoint.getPosition().z - input.car.position.z > 240){
					currentAction = new SmartDodgeAction(this, input);
				}else{
					double dodgeAngle = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
					double goalAngle = Vector2.angle(wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), goal.minus(input.car.position.flatten()));
					
					//Check if we can go for a shot
					Vector2 trace = Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH);
					boolean shotOpportunity = (trace != null && Math.abs(trace.x) < 1200);
					
					if((Math.abs(dodgeAngle) < 0.3 && !shotOpportunity) || goalAngle < 0.5 || Utils.teamSign(input.car) * input.ball.velocity.y < -500 ||  Utils.teamSign(input.car) * input.car.position.y < -3000){
						//If the dodge angle is small, make it big - trust me, it works
						if(Math.abs(dodgeAngle) < 1.22){ //70 degrees
							dodgeAngle = Utils.clamp(dodgeAngle * 3.25D, -Math.PI, Math.PI);
						}
						currentAction = new DodgeAction(this, dodgeAngle, input);
					}
				}
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}else if(wall && Math.abs(input.car.position.x) < Utils.GOALHALFWIDTH - 50){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
			}else if(distance > 3700 && !input.car.isSupersonic && input.car.boost < 45 && velocityTowardsImpact > 1250 && 0.2 > Math.abs(Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten()))){
				//Front flip for speed
				currentAction = new DodgeAction(this, 0, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Drive down the wall
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
    	//Impact point
		Vector3 impactPoint = wildfire.impactPoint.getPosition();
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.yellow, 0, wildfire.impactPoint.getFrame());
		wildfire.renderer.drawCrosshair(input.car, impactPoint, Color.MAGENTA, 125);
		Vector2 trace = Utils.traceToY(input.car.position.flatten(), impactPoint.minus(input.car.position).flatten(), Utils.teamSign(input.car) * -Utils.PITCHLENGTH);
		if(trace != null && Math.abs(trace.x) < Utils.GOALHALFWIDTH + 400){
			impactPoint = new Vector3(impactPoint.x - Math.signum(trace.x) * Math.max(60, Math.min(1200, distance / 3.6)), impactPoint.y, impactPoint.z);
			wildfire.renderer.drawCrosshair(input.car, impactPoint, Color.PINK, 125);
		}
		
		//Target
		Vector2 target = getPosition(input.car.position.flatten(), goal, 0, impactPoint);
		wildfire.renderer.drawCircle(Color.ORANGE, target, Utils.BALLRADIUS * 0.2F);
        
		double steer = Utils.aim(input.car, target);
        return new ControlsOutput().withSteer((float)-steer * 3F).withThrottle(1).withBoost(Math.abs(steer) < 0.2F && !input.car.isSupersonic).withSlide(Math.abs(steer) > 1.2F && input.car.forwardMagnitude() > 0);
	}
	
	private Vector2 getPosition(Vector2 start, Vector2 goal, int ply, Vector3 impactPoint){
		if(ply >= 30) return null;
		
		double distance = impactPoint.distanceFlat(start);
		
		Vector2 end = impactPoint.flatten().plus(impactPoint.flatten().minus(goal).scaledToMagnitude(distance * 0.39));
		end = start.plus(end.minus(start).scaled(0.2)).confine(35, 50);
		
		//Clamp the X when stuck in goal
		boolean stuck = Math.abs(start.y) > Utils.PITCHLENGTH; 
		if(stuck){
			end = new Vector2(Math.max(-750, Math.min(750, end.x)), Math.signum(end.y) * Utils.PITCHLENGTH);
		}
		
		wildfire.renderer.drawLine3d(Color.RED, start.toFramework(), end.toFramework());
		Vector2 next = getPosition(end, goal, ply + 1, impactPoint);
		return ply < targetPly && !stuck ? next : end;
	}

}
