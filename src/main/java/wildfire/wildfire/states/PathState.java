package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.HopAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.Path;
import wildfire.wildfire.obj.State;

public class PathState extends State {
	
	/*
	 * Fresh and new path planning state
	 */
		
	private Path path;

	public PathState(Wildfire wildfire){
		super("Path", wildfire);
		path = null;
	}
	
	@Override
	public boolean ready(DataPacket input){
		//This is to avoid starting a path when there is a shooter
		if(Utils.closestOpponentDistance(input, input.ball.position) < 1700 && Utils.isOpponentBehindBall(input)){ //&& Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < 1200 
			return false;
		}
		
		if(!requirements(input)) return false;
		
		//Generate the path
		path = Path.fromBallPrediction(wildfire, input.car, Utils.enemyGoal(input.car.team));
				
		//Good/bad path
		return path != null && !path.isBadPath();
	}

	@Override
	public boolean expire(DataPacket input){	
		if(!requirements(input)) return true;
		path = Path.fromBallPrediction(wildfire, input.car, Utils.enemyGoal(input.car.team));
		return path == null || path.isBadPath();
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		path.renderPath(wildfire.renderer);
		
		if(!hasAction()){
			if(Utils.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}else if(input.car.position.z > 400){
				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		boolean cone = Utils.isInCone(input.car, wildfire.impactPoint.getPosition());
		wildfire.renderer.drawString2d("Path Distance: " + (int)path.getDistance() + "uu", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Time: " + Utils.round(path.getTime()) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawLine3d((cone ? Color.GREEN : Color.YELLOW), input.car.position.flatten().toFramework(), Utils.traceToWall(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten()).toFramework());
		
		//Impact point
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.WHITE, 90);
		
		//Make a target from the path
		int targetPly = getTargetPly(input.car);
		wildfire.renderer.drawString2d("Target Ply: " + targetPly, Color.WHITE, new Point(0, 40), 2, 2);
		Vector2 target = path.getPly(targetPly);
		wildfire.renderer.drawCircle(input.car.team == 0 ? Color.CYAN : Color.RED, target, 15);
		
		double distanceImpact = input.car.position.distanceFlat(wildfire.impactPoint.getPosition());
		double steer = Utils.aim(input.car, target);
		return new ControlsOutput().withSteer((float)(steer * -3F)).withThrottle(1).withBoost(!input.car.isSupersonic && Math.abs(steer) < 0.09 && (distanceImpact > 1500 || cone));
	}
	
	private int getTargetPly(CarData car){
		double velocity = car.velocity.magnitude();
		if(velocity < 1500){
			return 8;
		}else if(velocity < 2000){
			return 8 + (int)(((velocity - 1500) / 300) * 3); //Max 11
		}else{
			return 11 + (int)(((velocity - 2000) / 400) * 2); //Max 13
		}
	}
	
	private boolean requirements(DataPacket input){
		if(Utils.isBallAirborne(input.ball) || input.ball.velocity.flatten().magnitude() > 2800 || Utils.isKickoff(input) || Utils.distanceToWall(input.car.position) < 440 || input.car.magnitudeInDirection(input.ball.position.minus(input.car.position).flatten()) < -800) return false;
		
		//This state isn't exactly the best defence
		if(Utils.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
		if(Utils.teamSign(input.car) * input.ball.velocity.y < (Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()) ? -1800 : -1200)) return false;
		if(Utils.teamSign(input.car) * input.ball.position.y < -3200 && Math.abs(wildfire.impactPoint.getPosition().x) < 1700) return false;
		
		double impactDistance = wildfire.impactPoint.getPosition().distanceFlat(input.car.position);
		
//		//Avoid making an endless path
//		Vector2 carGoal = Utils.enemyGoal(input.car.team).minus(input.car.position.flatten());
//		Vector2 carBall = wildfire.impactPoint.getPosition().minus(input.car.position).flatten();
//		if(impactDistance < 1200 && carBall.angle(carGoal) > 1.6){
//			return false;
//		}
		
		Vector2 trace = Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Utils.PITCHLENGTH);
		
		//Correct ourself to not save our own shot
		if(Utils.isOnTarget(wildfire.ballPrediction, 1 - input.car.team)){
//			return !Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition());
			return trace == null || Math.abs(trace.x) > Utils.GOALHALFWIDTH - Utils.BALLRADIUS - 20;
		}
		
		//This occurs when we have to correct to face the ball, but its in a shooting position		
		if(trace != null && Math.abs(trace.x) < Utils.GOALHALFWIDTH - 140){
			double steerImpact = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			return impactDistance < 6000 && (impactDistance > 2200 || Math.abs(steerImpact) > 60 * (Math.PI / 180));
		}
		
		//Shoot from the wing
		if(Math.abs(wildfire.impactPoint.getPosition().x) > Utils.PITCHWIDTH - 1200){
			return Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y > -1000 && impactDistance < 8000 && Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()); 
		}
		
		//Slight shot correction
		if(trace != null && Math.abs(trace.x) < 1580 && Math.abs(trace.x) > 660){
			return true;
		}	
		
		//Arc to hit the ball
		Vector2 goalBall = wildfire.impactPoint.getPosition().flatten().minus(Utils.enemyGoal(input.car.team));
		Vector2 ballCar = input.car.position.minus(wildfire.impactPoint.getPosition()).flatten();
		double arc = goalBall.angle(ballCar);
		return impactDistance < 2800 && arc > 0.785398 && (arc < 1.48353 || Utils.enemyGoal(input.car.team).distance(wildfire.impactPoint.getPosition().flatten()) < 2000); //45, 85
	}

}