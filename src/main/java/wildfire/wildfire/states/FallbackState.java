package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class FallbackState extends State {
	
	private int targetPly = 9;

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
			if(distance < (wall ? 250 : 900) && input.car.magnitudeInDirection(wildfire.impactPoint.getPosition().minus(input.car.position).flatten()) > 900){ //|| input.ball.position.z > 500
				double dodgeAngle = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
				double goalAngle = Vector2.angle(wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), goal.minus(input.car.position.flatten()));
				
				Vector2 trace = Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH);
				boolean shotOpportunity = (trace != null && Math.abs(trace.x) < 1200);
				
				if((Math.abs(dodgeAngle) < 0.3 && !shotOpportunity) || goalAngle < 0.5 || Utils.teamSign(input.car) * input.ball.velocity.y < -500 ||  Utils.teamSign(input.car) * input.car.position.y < -3000){
					//If the dodge angle is small, make it big - trust me, it works
					if(Math.abs(dodgeAngle) < 1.22){ //70 degrees
						dodgeAngle = Utils.clamp(dodgeAngle * 3.25D, -Math.PI, Math.PI);
					}
					
					currentAction = new DodgeAction(this, dodgeAngle, input);
				}
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}else if(wall && Math.abs(input.car.position.x) < Utils.GOALHALFWIDTH - 50){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Drive down the wall
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
    	//Impact point
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 125);
		
		//Target
		Vector2 target = getPosition(input.car.position.flatten(), goal, 0);
		wildfire.renderer.drawCircle(Color.ORANGE, target, Utils.BALLRADIUS * 0.25F);
        
		double steer = Utils.aim(input.car, target);
        return new ControlsOutput().withSteer((float)-steer * 2F).withThrottle(1).withBoost(Math.abs(steer) < 0.2F && !input.car.isSupersonic).withSlide(Math.abs(steer) > 1.2F);
	}
	
	private Vector2 getPosition(Vector2 start, Vector2 goal, int ply){
		if(ply >= 30) return null;
		
		double distance = wildfire.impactPoint.getPosition().distanceFlat(start);
		
		Vector2 end = wildfire.impactPoint.getPosition().flatten().plus(wildfire.impactPoint.getPosition().flatten().minus(goal).scaledToMagnitude(distance * 0.39));
		end = start.plus(end.minus(start).scaled(0.2)).confine();
		
		//Clamp the X when stuck in goal
		boolean stuck = Math.abs(start.y) > Utils.PITCHLENGTH; 
		if(stuck){
			end = new Vector2(Math.max(-780, Math.min(780, end.x)), Math.signum(end.y) * Utils.PITCHLENGTH);
		}
		
		wildfire.renderer.drawLine3d(Color.RED, start.toFramework(), end.toFramework());
		Vector2 next = getPosition(end, goal, ply + 1);
		return ply < targetPly && !stuck ? next : end;
	}

}
