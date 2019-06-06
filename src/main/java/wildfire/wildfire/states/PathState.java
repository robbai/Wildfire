package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.Path;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class PathState extends State {
	
	/*
	 * Fresh and new path planning state
	 */
		
	private final boolean force = false;
	private Path path;

	public PathState(Wildfire wildfire){
		super("Path", wildfire);
		path = null;
	}
	
	@Override
	public boolean ready(DataPacket input){
		//This is to avoid starting a path when there is a shooter
		if(!force){
			if(Behaviour.closestOpponentDistance(input, input.ball.position) < 1700 && Behaviour.isOpponentBehindBall(input)){ //&& Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < 1200 
				return false;
			}
			if(!requirements(input)) return false;
		}
		
		//Generate the path
		path = Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), Math.max(input.car.velocity.flatten().magnitude(), 1410));
				
		//Good/bad path
		return path != null && (!path.isBadPath() || force);
	}

	@Override
	public boolean expire(DataPacket input){	
		if(!requirements(input)){
			if(!force) return true;
		}
		path = Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), Math.max(input.car.velocity.flatten().magnitude(), 1410));
		return path == null || (path.isBadPath() && !force);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		path.renderPath(wildfire.renderer);
		wildfire.renderer.drawTurningRadius(Color.WHITE, input.car);
		
		if(!hasAction()){
			if(Behaviour.isCarAirborne(input.car)){
				currentAction = new RecoveryAction(this, input.elapsedSeconds);
			}
//			else if(input.car.position.z > 400){
//				currentAction = new HopAction(this, input, wildfire.impactPoint.getPosition().flatten());
//			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		}
		
		boolean cone = Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition());
		wildfire.renderer.drawString2d("Path Distance: " + (int)path.getDistance() + "uu", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Time: " + Utils.round(path.getTime()) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawLine3d((cone ? Color.GREEN : Color.YELLOW), input.car.position.flatten().toFramework(), Utils.traceToWall(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten()).toFramework());
		
		//Impact point
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition(), Color.WHITE, 90);
		
		//Make a target from the path
		double targetPly = getTargetPly(input.car);
		Vector2 target = path.getPly(targetPly);
//		wildfire.renderer.drawString2d("Target Ply: " + Utils.round(targetPly), Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawCircle(input.car.team == 0 ? Color.CYAN : Color.RED, target, 10);
		
//		double steer = Handling.aim(input.car, target);
		double steer = new Vector2(0, 1).correctionAngle(Utils.toLocal(input.car, target.withZ(0)).flatten());
		
		Path maxPath = (input.car.boost == 0 || input.car.isSupersonic || Math.abs(steer) > 0.3 ? null : Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), path.getVelocity() + Constants.BOOSTACC * (4D / 60)));
		if(maxPath != null) wildfire.renderer.drawString2d("Boost Time: " + Utils.round(maxPath.getTime()) + "s", Color.WHITE, new Point(0, 60), 2, 2);
		return new ControlsOutput().withSteer((float)(steer * -3)).withThrottle(1)
				.withBoost(maxPath != null && path.getTime() >= maxPath.getTime());
	}
	
	private double getTargetPly(CarData car){
		if(path.isBadPath()) return 0; //Only when forcing is enabled
		double targetUnits = 450;
		double velocity = car.velocity.magnitude();
		return (targetUnits / (velocity * Path.scale));
	}
	
	private boolean requirements(DataPacket input){
		if(Behaviour.isBallAirborne(input.ball) || input.ball.velocity.flatten().magnitude() > 3200 || Behaviour.isKickoff(input) || Utils.distanceToWall(input.car.position) < 400 || input.car.magnitudeInDirection(input.ball.position.minus(input.car.position).flatten()) < -1100) return false;
		
		//This state isn't exactly the best defence
		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
		if(Utils.teamSign(input.car) * input.ball.velocity.y < (Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()) ? -1800 : -1200)) return false;
		if(Utils.teamSign(input.car) * input.ball.position.y < -3500 && Math.abs(wildfire.impactPoint.getPosition().x) < 1700) return false;
		
		double impactDistance = wildfire.impactPoint.getPosition().distanceFlat(input.car.position);
		
//		//Avoid making an endless path
//		Vector2 carGoal = Utils.enemyGoal(input.car.team).minus(input.car.position.flatten());
//		Vector2 carBall = wildfire.impactPoint.getPosition().minus(input.car.position).flatten();
//		if(impactDistance < 1200 && carBall.angle(carGoal) > 1.6){
//			return false;
//		}
		
		Vector2 trace = Utils.traceToY(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
		
		//Correct ourself to not save our own shot
		if(Behaviour.isOnTarget(wildfire.ballPrediction, 1 - input.car.team)){
//			return !Utils.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition());
			return trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS - 20;
		}
		
		//This occurs when we have to correct to face the ball, but its in a shooting position		
		if(trace != null && Math.abs(trace.x) < Constants.GOALHALFWIDTH - 140){
			double steerImpact = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			return impactDistance < 6000 && (impactDistance > 2200 || Math.abs(steerImpact) > Math.toRadians(60));
		}
		
		//Shoot from the wing
		if(Math.abs(wildfire.impactPoint.getPosition().x) > Constants.PITCHWIDTH - 1300){
			return Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y > -1000 && impactDistance < 8000 && Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()); 
		}
		
		//Slight shot correction
		if(trace != null && Math.abs(trace.x) < 1630 && Math.abs(trace.x) > 620){
			return true;
		}	
		
		//Arc to hit the ball
		Vector2 goalBall = wildfire.impactPoint.getPosition().flatten().minus(Constants.enemyGoal(input.car.team));
		Vector2 ballCar = input.car.position.minus(wildfire.impactPoint.getPosition()).flatten();
		double arc = goalBall.angle(ballCar);
		return impactDistance < 2900 && arc > 0.785398 && (arc < 1.48353 || Constants.enemyGoal(input.car.team).distance(wildfire.impactPoint.getPosition().flatten()) < 2300); //45, 85
	}

}