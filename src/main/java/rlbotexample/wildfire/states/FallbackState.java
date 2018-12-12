package rlbotexample.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.DodgeAction;
import rlbotexample.wildfire.actions.HopAction;
import rlbotexample.wildfire.actions.RecoveryAction;

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
			if(distance < (input.car.position.z > 200 ? 250 : 900) && (input.car.velocity.magnitude() > 1300 || Utils.isBallAirborne(input.ball))){
				currentAction = new DodgeAction(this, Utils.aim(input.car, wildfire.impactPoint.flatten()) * 3.5D, input);
			}else if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this);
			}else if(wall && Math.abs(input.car.position.x) < Utils.GOALHALFWIDTH){
				currentAction = new HopAction(this, wildfire.impactPoint.flatten(), input.car.velocity);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Drive down the wall
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
		
//		targetPly = (int)(input.car.velocity.magnitude() / 300D) + 1;
//		wildfire.renderer.drawString2d("Target Ply: " + targetPly, Color.WHITE, new Point(0, 20), 2, 2);
		
		Utils.drawCrosshair(wildfire.renderer, input.car, wildfire.impactPoint, Color.MAGENTA, 125);
		Vector2 target = getPosition(input.car.position.flatten(), 0);
		Utils.drawCircle(wildfire.renderer, Color.ORANGE, target, Utils.BALLRADIUS * 0.25F);
        
		double aimBall = Utils.aim(input.car, target);
        return new ControlsOutput().withSteer((float)-aimBall * 2F).withThrottle(1).withBoost(Math.abs(aimBall) < 0.2F).withSlide(Math.abs(aimBall) > 1.4F);
	}
	
	private Vector2 getPosition(Vector2 start, int ply){
		if(ply >= 30) return null;
		
		double distance = wildfire.impactPoint.distanceFlat(start);
		
		Vector2 end = wildfire.impactPoint.flatten().plus(wildfire.impactPoint.flatten().minus(wildfire.target).scaledToMagnitude(distance * 0.38));
		end = start.plus(end.minus(start).scaled(0.4)).confine();
		
		//Clamp the X when stuck in goal
		boolean stuck = Math.abs(start.y) > Utils.PITCHLENGTH; 
		if(stuck){
			end = new Vector2(Math.max(-780, Math.min(780, end.x)), Math.signum(end.y) * Utils.PITCHLENGTH);
		}
		
		wildfire.renderer.drawLine3d(Color.RED, start.toFramework(), end.toFramework());
		Vector2 next = getPosition(end, ply + 1);
		return ply < targetPly && !stuck ? next : end;
	}

}
