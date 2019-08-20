package wildfire.wildfire.states;

import java.util.OptionalDouble;

import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.curve.CompositeArc;
import wildfire.wildfire.curve.DiscreteCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.mechanics.FollowDiscreteMechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class PathState extends State {	
	
	/**
	 * Although this state doesn't hit the ball right on the penny every time,
	 * it's okay because normally it will be swapped out right before the shot is made
	 * it kind of assists the other states
	 */
	
	private static final double maxExtraTime = 1.25;
	
	private boolean force, dodge = true;
	private Vector3 slicePosition;

	public PathState(Wildfire wildfire, boolean force){
		super("Path", wildfire);
		this.force = force;
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(!force){
			// This is to avoid starting a path when there is a shooter
			if(Behaviour.closestOpponentDistance(input, input.ball.position) < 1700 && Behaviour.isOpponentBehindBall(input)){
				//&& Utils.teamSign(input.car) * input.info.impact.getPosition().y < 1200
				return false;
			}
			
			if(!requirements(input)) return false;
		}
		
		CompositeArc compArc = null;
		OptionalDouble pathTime = null;
		DiscreteCurve discrete = null;
		
		// Generate the path
		boolean ballStill = (input.ball.velocity.magnitude() < 1);
		Vector3 slicePosition = null;
		for(int i = input.info.impact.getFrame(); i < Math.min(wildfire.ballPrediction.slicesLength(), input.info.impact.getFrame() + maxExtraTime * 60); i++){
			rlbot.flat.PredictionSlice rawSlice = wildfire.ballPrediction.slices(i);
			if(Behaviour.isBallAirborne(rawSlice)) continue;
			
			slicePosition = Vector3.fromFlatbuffer(rawSlice.physics().location());
			double time = (rawSlice.gameSeconds() - input.elapsedSeconds);
			
			Vector2 enemyGoal = Behaviour.getTarget(input.car, slicePosition.flatten(), -400);
//			Vector2 enemyGoal = Constants.enemyGoal(input.car);
			Vector2 ball = slicePosition.flatten().plus(slicePosition.flatten().minus(enemyGoal).scaledToMagnitude(Constants.BALLRADIUS));

			compArc = CompositeArc.create(input.car, ball, enemyGoal, dodge ? 30 : 0);
			if(compArc == null) continue;
			
			if(!ballStill){
				// This check is much more lightweight than the discrete analysis.
				if(compArc.minTravelTime(input.car, false, false) > time) continue;
				
				discrete = new DiscreteCurve(input.car.forwardVelocity, input.car.boost, compArc);
				if(!discrete.isValid() || discrete.getTime() > time) continue;
			}
			
			pathTime = (ballStill? OptionalDouble.empty() : OptionalDouble.of(time));
			break;
		}
		
		if(pathTime == null) return false;
		
		// Start the mechanic!
		this.slicePosition = slicePosition;
//		this.currentMechanic = new FollowDiscreteMechanic(this, discrete, input.elapsedSeconds, this.dodge, pathTime);
		this.currentMechanic = new FollowDiscreteMechanic(this, discrete, input.elapsedSeconds, this.dodge);
		return this.runningMechanic();
	}

	@Override
	public boolean expire(InfoPacket input){	
		if(!force && !requirements(input)) return true;
		
		// Somebody messed it up.
		if(this.slicePosition != null && !Behaviour.isOnPrediction(wildfire.ballPrediction, this.slicePosition)){
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
		if(Behaviour.isBallAirborne(input.ball) || Behaviour.isKickoff(input) || input.ball.velocity.flatten().magnitude() > 3200
				|| Utils.distanceToWall(input.car.position) < 400 || input.car.velocityDir(input.ball.position.minus(input.car.position).flatten()) < -1000) return false;
		
		boolean opponentBehind = Behaviour.isOpponentBehindBall(input);
		
		//This state isn't exactly the best defence
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition());
		double opponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		boolean opponentClose = (opponentDistance < (opponentBehind ? 2800 : 1000));
		if(!correctSide && opponentClose && Utils.teamSign(input.car) * input.info.impact.getPosition().y < 4100) return false;
		if(Behaviour.isOnTarget(wildfire.ballPrediction, input.car.team)) return false;
		if(Utils.teamSign(input.car) * input.ball.velocity.y < (correctSide ? -1800 : -1200)) return false;
		if(Utils.teamSign(input.car) * input.ball.position.y < (opponentClose ? -2750 : -3500)) return false; // && (opponentClose || Math.abs(input.info.impact.getPosition().x) < 1700)
		
		double impactDistance = input.info.impact.getPosition().distanceFlat(input.car.position);
		
		Vector2 carGoal = Constants.enemyGoal(input.car).minus(input.car.position.flatten());
		Vector2 carBall = input.info.impact.getPosition().minus(input.car.position).flatten();
		if(impactDistance < 1200 && carBall.angle(carGoal) > 1.6){
			return false;
		}
		
		Vector2 trace = Utils.traceToY(input.car.position.flatten(), input.info.impact.getPosition().minus(input.car.position).flatten(), Utils.teamSign(input.car) * Constants.PITCHLENGTH);
		
		// Correct ourself to not save our own shot.
		if(Behaviour.isOnTarget(wildfire.ballPrediction, 1 - input.car.team)){
			return trace == null || Math.abs(trace.x) > Constants.GOALHALFWIDTH - Constants.BALLRADIUS - 20;
		}
		
		// This occurs when we have to correct to face the ball, but its in a shooting position.		
		if(trace != null && Math.abs(trace.x) < Constants.GOALHALFWIDTH - 140 && input.car.isSupersonic){
			double steerImpact = Handling.aim(input.car, input.info.impact.getPosition().flatten());
			return impactDistance < 6000 && (impactDistance > 2200 || Math.abs(steerImpact) > Math.toRadians(70));
		}
		
		// Shoot from the wing.
		if(Math.abs(input.info.impact.getPosition().x) > Constants.PITCHWIDTH - 1300){
			return Utils.teamSign(input.car) * input.info.impact.getPosition().y > -1000 && impactDistance < 8000 && Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition()); 
		}
		
		// Slight shot correction.
		if(trace != null && Math.abs(trace.x) < 1630 && Math.abs(trace.x) > 1050){
			return true;
		}	
		
		// Arc to hit the ball.
		Vector2 goalBall = input.info.impact.getPosition().flatten().minus(Constants.enemyGoal(input.car.team));
		Vector2 ballCar = input.car.position.minus(input.info.impact.getPosition()).flatten();
		double arc = goalBall.angle(ballCar);
		return impactDistance < 2900 && arc > Math.toRadians(40) && (arc < Math.toDegrees(85) || Constants.enemyGoal(input.car.team).distance(input.info.impact.getPosition().flatten()) < 2300);
	}

}