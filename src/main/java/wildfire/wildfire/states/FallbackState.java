package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.WavedashAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class FallbackState extends State {
	
	/*
	 * These two mystical values hold the secrets to this state
	 */
	private static final double dropoff = 0.165, scope = 0.39;
	
	/*
	 * Yeah this one too, I guess
	 */
	private final int targetPly = 6;

	public FallbackState(Wildfire wildfire){
		super("Fallback", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		boolean wall = Behaviour.isOnWall(input.car);
		double distance = wildfire.impactPoint.getPosition().distance(input.car.position);
		
		//Goal
		Vector2 goal = Behaviour.getTarget(input.car, input.ball);
		wildfire.renderer.drawCrosshair(input.car, goal.withZ(Constants.BALLRADIUS), Color.WHITE, 125);
		
		//Drive down the wall
		if(wall){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Handling.driveDownWall(input);
		}

		//Impact point
		Vector3 impactPoint = wildfire.impactPoint.getPosition();
		wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.yellow, 0, wildfire.impactPoint.getFrame());
		wildfire.renderer.drawCrosshair(input.car, impactPoint, Color.MAGENTA, 125);

		//Avoid own-goaling
		Vector2 trace = Utils.traceToY(input.car.position.flatten(), impactPoint.minus(input.car.position).flatten(), Utils.teamSign(input.car) * -Constants.PITCHLENGTH);
		boolean avoidOwnGoal = (trace != null && Math.abs(trace.x) < Constants.GOALHALFWIDTH + 900);
		if(avoidOwnGoal){
			impactPoint = new Vector3(impactPoint.x - Math.signum(trace.x) * Utils.clamp(distance / 3.6, 70, 1000), impactPoint.y, impactPoint.z);
			wildfire.renderer.drawCrosshair(input.car, impactPoint, Color.PINK, 125);
		}

		//Target
		Vector2 target = getPosition(input.car.position.flatten(), goal, 0, impactPoint);
		wildfire.renderer.drawCircle(Color.ORANGE, target, Constants.BALLRADIUS * 0.2F);

		//Handling
		double steer = Handling.aim(input.car, target);
		double throttle = (Handling.insideTurningRadius(input.car, target) && Math.abs(steer) > 0.24 && input.car.velocity.magnitude() > 1600 ? -1 : 1);
		if(throttle < 1) wildfire.renderer.drawTurningRadius(Color.WHITE, input.car);
		
		//Action
		if(!hasAction()){
			double velocityTowardsImpact = input.car.magnitudeInDirection(wildfire.impactPoint.getPosition().minus(input.car.position).flatten());
			double velocity = input.car.velocity.magnitude();
			double forwardsComponent = Math.cos(input.car.orientation.noseVector.flatten().correctionAngle(input.car.velocity.flatten()));
			double aimImpact = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			if(distance < (wall ? 380 : (input.car.isSupersonic ? 800 : 500)) && velocityTowardsImpact > 1200 && forwardsComponent > 0.95){
				double dodgeAngle = aimImpact;
				double goalAngle = Vector2.angle(wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), goal.minus(input.car.position.flatten()));
				
				//Check if we can go for a shot
				Vector2 traceGoal = Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
				boolean shotOpportunity = (traceGoal != null && Math.abs(traceGoal.x) < 1200);
				
				if((Math.abs(dodgeAngle) < 0.3 && !shotOpportunity) || goalAngle < 0.5 || Utils.teamSign(input.car) * input.ball.velocity.y < -500 ||  Utils.teamSign(input.car) * input.car.position.y < -3000){
					//If the dodge angle is small, make it big - trust me, it works
					if(Math.abs(dodgeAngle) < 1.39626){ //80 degrees
						dodgeAngle = Utils.clamp(dodgeAngle * 3.25D, -Math.PI, Math.PI);
					}
					currentAction = new DodgeAction(this, dodgeAngle, input);
				}
			}else if(Behaviour.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}else if(wall && Math.abs(input.car.position.x) < Constants.GOALHALFWIDTH - 50){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
			}else if(wildfire.impactPoint.getTime() > (avoidOwnGoal ? 1.4 : 1.7) && !input.car.isSupersonic 
					&& velocity > (input.car.boost == 0 ? 1200 : 1500) && Math.abs(aimImpact) < 0.2){
				//Front flip for speed
				if(input.car.boost < 10 || Math.abs(aimImpact) > 0.05 || velocity > 1300){
					currentAction = new DodgeAction(this, 0, input);
				}else{
					currentAction = new WavedashAction(this, input);
				}
			}
			
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		//Controller
        return new ControlsOutput().withSteer((float)-steer * 3F).withThrottle((float)throttle).withBoost(Math.abs(steer) < 0.2F && !input.car.isSupersonic && throttle >= 1)
        		.withSlide(Math.abs(steer) > (input.car.isSupersonic ? 1.6 : 1.2) && input.car.forwardMagnitude() > 900);
	}
	
	private Vector2 getPosition(Vector2 start, Vector2 goal, int ply, Vector3 impactPoint){
		if(ply >= 30) return null;
		
		double distance = impactPoint.distanceFlat(start);
		
		Vector2 end = impactPoint.flatten().plus(impactPoint.flatten().minus(goal).scaledToMagnitude(distance * scope));
		end = start.plus(end.minus(start).scaled(dropoff)).confine(35, 50);
		
		wildfire.renderer.drawLine3d(Color.RED, start.toFramework(), end.toFramework());
		Vector2 next = getPosition(end, goal, ply + 1, impactPoint);
		return ply < targetPly ? (ply == targetPly ? start : next) : end;
	}

}
