package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.curve.Path;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class PathState extends State {
	
	/*
	 * Fresh and new path planning state
	 */
		
	private final boolean force;
	
	private Path path;

	public PathState(Wildfire wildfire, boolean force){
		super("Path", wildfire);
		this.force = force;
		path = null;
	}
	
	@Override
	public boolean ready(DataPacket input){
		//This is to avoid starting a path when there is a shooter
		if(!force){
			if(Behaviour.closestOpponentDistance(input, input.ball.position) < 1700 && Behaviour.isOpponentBehindBall(input)){
				//&& Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < 1200
				return false;
			}
			if(!requirements(input)) return false;
		}
		
		//Generate the path
		path = Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), Math.max(input.car.velocity.flatten().magnitude(), 1410), this.force);
				
		//Good/bad path
		return path != null && (!path.isBadPath() || force);
	}

	@Override
	public boolean expire(DataPacket input){	
		if(!requirements(input)){
			if(!force) return true;
		}
		path = Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), Math.max(input.car.velocity.flatten().magnitude(), 1410), this.force);
		return path == null || (path.isBadPath() && !force);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		path.renderPath(wildfire.renderer);
		wildfire.renderer.drawTurningRadius(Color.WHITE, input.car);
		
		if(Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		// Render.
		boolean cone = Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition());
		wildfire.renderer.drawString2d("Path Distance: " + (int)path.getDistance() + "uu", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Time: " + Utils.round(path.getTime()) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawLine3d((cone ? Color.GREEN : Color.YELLOW), input.car.position.flatten().toFramework(), Utils.traceToWall(input.car.position.flatten(), wildfire.impactPoint.getPosition().minus(input.car.position).flatten()).toFramework());
		wildfire.renderer.drawCrosshair(input.car, wildfire.impactPoint.getPosition().withZ(Constants.BALLRADIUS), Color.WHITE, 90);
		
		// Make a target from the path.
		double targetPly = getTargetPly(input.car, input.car.velocity.flatten().magnitude());
		Vector2 target = path.getPly(targetPly);
		wildfire.renderer.drawCircle(input.car.team == 0 ? Color.CYAN : Color.RED, target, 10);
		
		// Controller.
		double steer = Handling.aim(input.car, target);
		double boostedVelocity = (input.car.velocity.flatten().magnitude() + Constants.BOOSTACC * (10D / 120));
		Path boostPath = (input.car.boost == 0 || input.car.isSupersonic || Math.abs(steer) > 0.5 ? null : Path.fromBallPrediction(wildfire, input.car, Constants.enemyGoal(input.car.team), boostedVelocity, false));
		boolean boost = false;
		if(boostPath != null && path.getTime() >= boostPath.getTime() + 0.035){
			steer = Handling.aim(input.car, boostPath.getPly(getTargetPly(input.car, boostedVelocity)));
			wildfire.renderer.drawString2d("Boost Time: " + Utils.round(boostPath.getTime()) + "s", Color.WHITE, new Point(0, 60), 2, 2);
			boost = true;
		}
		return new ControlsOutput().withSteer(steer * -5).withThrottle(1).withBoost(boost);
	}
	
	private double getTargetPly(CarData car, double velocity){
		if(path.isBadPath()) return 0; // Only when forcing is enabled.
		double targetUnits = 340;
		return (targetUnits / (velocity * Path.scale));
	}
	
	private boolean requirements(DataPacket input){
		if(Behaviour.isBallAirborne(input.ball) || Behaviour.isKickoff(input) || input.ball.velocity.flatten().magnitude() > 3200
				|| Utils.distanceToWall(input.car.position) < 400 || input.car.velocityDir(input.ball.position.minus(input.car.position).flatten()) < -1000) return false;
		
		boolean opponentBehind = Behaviour.isOpponentBehindBall(input);
		
		//This state isn't exactly the best defence
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition());
		double opponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		boolean opponentClose = (opponentDistance < (opponentBehind ? 2800 : 1000));
		if(!correctSide && opponentClose && Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y < 4100) return false;
		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
		if(Utils.teamSign(input.car) * input.ball.velocity.y < (correctSide ? -1800 : -1200)) return false;
		if(Utils.teamSign(input.car) * input.ball.position.y < (opponentClose ? -2750 : -3500)) return false; // && (opponentClose || Math.abs(wildfire.impactPoint.getPosition().x) < 1700)
		
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
			return trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS - 20;
		}
		
		//This occurs when we have to correct to face the ball, but its in a shooting position		
		if(trace != null && Math.abs(trace.x) < Constants.GOALHALFWIDTH - 140 && input.car.isSupersonic){
			double steerImpact = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
			return impactDistance < 6000 && (impactDistance > 2200 || Math.abs(steerImpact) > Math.toRadians(70));
		}
		
		//Shoot from the wing
		if(Math.abs(wildfire.impactPoint.getPosition().x) > Constants.PITCHWIDTH - 1300){
			return Utils.teamSign(input.car) * wildfire.impactPoint.getPosition().y > -1000 && impactDistance < 8000 && Behaviour.correctSideOfTarget(input.car, wildfire.impactPoint.getPosition()); 
		}
		
		//Slight shot correction
		if(trace != null && Math.abs(trace.x) < 1630 && Math.abs(trace.x) > 500){
			return true;
		}	
		
		//Arc to hit the ball
		Vector2 goalBall = wildfire.impactPoint.getPosition().flatten().minus(Constants.enemyGoal(input.car.team));
		Vector2 ballCar = input.car.position.minus(wildfire.impactPoint.getPosition()).flatten();
		double arc = goalBall.angle(ballCar);
		return impactDistance < 2900 && arc > Math.toRadians(40) && (arc < Math.toDegrees(85) || Constants.enemyGoal(input.car.team).distance(wildfire.impactPoint.getPosition().flatten()) < 2300);
	}

}