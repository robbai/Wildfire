package wildfire.wildfire.states;

import java.util.OptionalDouble;

import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.curve.CompositeArc;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.mechanics.FollowDiscreteMechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class PathState extends State {	
	
	/**
	 * Although this state doesn't hit the ball right on the penny every time,
	 * it's okay because normally it will be swapped out right before the shot is made
	 * it kind of assists the other states
	 */
	
	private static final double maxExtraTime = 1.5;
	private static final OptionalDouble maxFinalVelocity = OptionalDouble.of(1700);
//	private final OptionalDouble maxFinalVelocity = OptionalDouble.empty();
	
	private boolean force, dodge = false;
	private Vector3 slicePosition;

	private Vector2 enemyGoal;

	private double globalPathTime;

	public PathState(Wildfire wildfire, boolean force){
		super("Path", wildfire);
		this.force = force;
		this.enemyGoal = Constants.enemyGoal(wildfire.team);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(!force){
			if(!requirements(input)) return false;
		}
		
		int startLow = input.info.impact.getFrame(), low = startLow;
		int high = wildfire.ballPrediction.slicesLength();
		if(!force) high = Math.min(high, (int)Math.ceil(input.info.impact.getFrame() + maxExtraTime * 60));
		
		double finalVelocity;
		double maxVelocityTime = DrivePhysics.maxVelocity(input.car.forwardVelocityAbs, input.car.boost, Math.ceil(high / 60D));
		if(maxFinalVelocity.isPresent()){
			finalVelocity = Utils.clamp(maxVelocityTime, 1, maxFinalVelocity.getAsDouble());
		}else{
			finalVelocity = Math.max(maxVelocityTime, 1);
		}
		
		CompositeArc[] results = new CompositeArc[high - low + 1];
		
		// Generate the path.
		while(low < high){
			int middle = Math.floorDiv(low + high, 2); 
					
			rlbot.flat.PredictionSlice rawSlice = wildfire.ballPrediction.slices(middle);
			
			Vector3 slicePosition = Vector3.fromFlatbuffer(rawSlice.physics().location());
			double time = (rawSlice.gameSeconds() - input.elapsedSeconds);
			time -= 1D / 60;
			
			Vector2 ballPosition = slicePosition.flatten();
			ballPosition = offsetBall(ballPosition, enemyGoal);

			CompositeArc compositeArc = CompositeArc.create(input.car, ballPosition, enemyGoal, finalVelocity, 0, Constants.RIPPER.y * 2);
			results[middle - startLow] = compositeArc;
			
			if(compositeArc.minTravelTime(input.car, true, true) > time){
				low = middle + 1;
			}else{
				high = middle;
			}
		}
		
		CompositeArc compositeArc = results[low - startLow];
		if(compositeArc == null) return false;
		DiscreteCurve discreteCurve = new DiscreteCurve(input.car.forwardVelocityAbs, input.car.boost, compositeArc);
		
		// Extra conditions.
		double pathTime = (wildfire.ballPrediction.slices(low).gameSeconds() - input.elapsedSeconds);
		if(pathTime > input.info.enemyImpactTime - 0.2) return false;
		Vector3 slicePosition = Vector3.fromFlatbuffer(wildfire.ballPrediction.slices(low).physics().location());
		if(Utils.toLocal(input.car, slicePosition).z > 130) return false;
		if(curveOutOfBounds(discreteCurve)) return false;
		
//		System.out.println("startLow = " + startLow + ", low = " + low + ", high = " + high + ", mid = " + Math.floorDiv(low + high, 2));
		
		// Start the mechanic!
		this.slicePosition = slicePosition;
		this.globalPathTime = (pathTime + input.elapsedSeconds);
		FollowDiscreteMechanic follow = new FollowDiscreteMechanic(this, discreteCurve, input.elapsedSeconds, this.dodge, pathTime);
		follow.linearTarget = true;
		follow.renderPredictionToTargetTime = true;
		this.currentMechanic = follow;
		if(this.runningMechanic()){
			return true;
		}else{
			this.currentMechanic = null;
			return false;
		}
	}

	private Vector2 offsetBall(Vector2 ballPosition, Vector2 enemyGoal){
		return ballPosition.plus(ballPosition.minus(enemyGoal).scaledToMagnitude(Constants.BALL_RADIUS + Constants.RIPPER.y * 0.5 + Constants.RIPPER_OFFSET.y));
	}

	private boolean curveOutOfBounds(DiscreteCurve discreteCurve){
		Vector2[] points = discreteCurve.getPoints();
		for(int i = 0; i < points.length; i += 5){
			if(points[i].isOutOfBounds()) return true;
		}
		return false;
	}

	@Override
	public boolean expire(InfoPacket input){	
		if(!force && !requirements(input)) return true;
		
//		// Somebody messed it up.
		if(this.slicePosition != null && !Behaviour.isOnPredictionAroundGlobalTime(wildfire.ballPrediction, this.slicePosition, this.globalPathTime, 30)){
			return true;
		}
		
		return !this.runningMechanic();
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		// Let the mechanics handle it ;P
		return new ControlsOutput();
	}
	
	private boolean requirements(InfoPacket input){
		if(!this.runningMechanic() && (input.info.impactDistance < 1000 || input.info.impact.getTime() > input.info.enemyImpactTime)) return false;
//		return input.car.forwardVelocity > (input.car.boost < 5 ? 1350 : 1550);
//		return input.car.forwardVelocity > 1300;
		return true;
		
//		if(Behaviour.isBallAirborne(input.ball) || Behaviour.isKickoff(input) || input.ball.velocity.flatten().magnitude() > 3200
//				|| Utils.distanceToWall(input.car.position) < 400 || input.car.velocityDir(input.ball.position.minus(input.car.position).flatten()) < -1000) return false;
//		
//		boolean opponentBehind = Behaviour.isOpponentBehindBall(input);
//		
//		//This state isn't exactly the best defence
//		boolean correctSide = Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition());
//		double opponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
//		boolean opponentClose = (opponentDistance < (opponentBehind ? 2800 : 1000));
//		if(!correctSide && opponentClose && Utils.teamSign(input.car) * input.info.impact.getPosition().y < 4100) return false;
//		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
//		if(Utils.teamSign(input.car) * input.ball.velocity.y < (correctSide ? -1800 : -1200)) return false;
//		if(Utils.teamSign(input.car) * input.ball.position.y < (opponentClose ? -2750 : -3500)) return false; // && (opponentClose || Math.abs(input.info.impact.getPosition().x) < 1700)
//		
//		double impactDistance = input.info.impact.getPosition().distanceFlat(input.car.position);
//		
//		Vector2 carGoal = Constants.enemyGoal(input.car).minus(input.car.position.flatten());
//		Vector2 carBall = input.info.impact.getPosition().minus(input.car.position).flatten();
//		if(impactDistance < 1200 && carBall.angle(carGoal) > 1.6){
//			return false;
//		}
//		
//		Vector2 trace = Utils.traceToY(input.car.position.flatten(), input.info.impact.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
//		
//		// Correct ourself to not save our own shot.
//		if(Behaviour.isOnTarget(wildfire.ballPrediction, 1 - input.car.team)){
//			return trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS - 20;
//		}
//		
//		// This occurs when we have to correct to face the ball, but its in a shooting position.		
//		if(trace != null && Math.abs(trace.x) < Constants.GOALHALFWIDTH - 140 && input.car.isSupersonic){
//			double steerImpact = Handling.aim(input.car, input.info.impact.getPosition().flatten());
//			return impactDistance < 6000 && (impactDistance > 2200 || Math.abs(steerImpact) > Math.toRadians(70));
//		}
//		
//		// Shoot from the wing.
//		if(Math.abs(input.info.impact.getPosition().x) > Constants.PITCHWIDTH - 1300){
//			return Utils.teamSign(input.car) * input.info.impact.getPosition().y > -1000 && impactDistance < 8000 && Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition()); 
//		}
//		
//		// Slight shot correction.
//		if(trace != null && Math.abs(trace.x) < 1630 && Math.abs(trace.x) > 1050){
//			return true;
//		}	
//		
//		// Arc to hit the ball.
//		Vector2 goalBall = input.info.impact.getPosition().flatten().minus(Constants.enemyGoal(input.car.team));
//		Vector2 ballCar = input.car.position.minus(input.info.impact.getPosition()).flatten();
//		double arc = goalBall.angle(ballCar);
//		return impactDistance < 2900 && arc > Math.toRadians(40) && (arc < Math.toDegrees(85) || Constants.enemyGoal(input.car.team).distance(input.info.impact.getPosition().flatten()) < 2300);
	}

}