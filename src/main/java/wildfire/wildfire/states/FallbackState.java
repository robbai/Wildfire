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
	
	private int targetPly = 2;

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
		double distance = input.ball.position.distance(input.car.position);
		
		if(!hasAction()){
			if(distance < (wall ? 250 : 900) && (input.car.velocity.magnitude() > 1300 || Utils.isBallAirborne(input.ball))){
				//My reason for the angle coefficient: "use whatever works" -Marvin
				double dodgeAngle = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten()) * (wildfire.isTestVersion() ? 1D : 3.25D);
				dodgeAngle = Utils.clamp(dodgeAngle, -Math.PI, Math.PI);
				
				currentAction = new DodgeAction(this, dodgeAngle, input);
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this);
			}else if(wall && Math.abs(input.car.position.x) < Utils.GOALHALFWIDTH - 50){
				currentAction = new HopAction(this, wildfire.impactPoint.getPosition().flatten(), input.car.velocity);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Drive down the wall
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
		//Goal-target
		Vector2 goal = Utils.getTarget(input.car, input.ball);
    	wildfire.renderer.drawCrosshair(input.car, goal.withZ(Utils.BALLRADIUS), Color.WHITE, 125);
		
    	//Impact point
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.MAGENTA, 125);
		
		//Target
		Vector2 target = getPosition(input.car.position.flatten(), goal, 0);
		wildfire.renderer.drawCircle(Color.ORANGE, target, Utils.BALLRADIUS * 0.25F);
        
		double steer = Utils.aim(input.car, target);
        return new ControlsOutput().withSteer((float)-steer * 2F).withThrottle(1).withBoost(Math.abs(steer) < 0.2F && !input.car.isSupersonic).withSlide(Math.abs(steer) > 1.4F);
	}
	
	private Vector2 getPosition(Vector2 start, Vector2 goal, int ply){
		if(ply >= 30) return null;
		
		double distance = wildfire.impactPoint.getPosition().distanceFlat(start);
		
		Vector2 end = wildfire.impactPoint.getPosition().flatten().plus(wildfire.impactPoint.getPosition().flatten().minus(goal).scaledToMagnitude(distance * 0.38));
		end = start.plus(end.minus(start).scaled(0.4)).confine();
		
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
