package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.WavedashAction;
import wildfire.wildfire.curve.BezierCurve;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.KickoffSpawn;
import wildfire.wildfire.obj.PID;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class KickoffState extends State {

	/**
	 * Used for enabling/disabling the chance of fake kickoffs
	 */
	private final boolean fakeKickoffs = false;
	private final double fakeChance = 0.3;

	private KickoffSpawn spawn;
	private Random random;
	private PID fakeAlignPID;

	private float timeStarted;
	private boolean timedOut;
	private boolean fake;
	private Vector2 randomOffset;

	public KickoffState(Wildfire wildfire){
		super("Kickoff", wildfire);
		random = new Random();
	}

	@Override
	public boolean ready(InfoPacket input){
		if(!Behaviour.isKickoff(input))
			return false;

		wildfire.unlimitedBoost = (input.car.boost > 99);
		getSpawn(input.car.position);
		timeStarted = input.elapsedSeconds;

		// We apply a random-offset to the ball so we're not hard countered continually.
		randomOffset = new Vector2(random.nextDouble() * 2 - 1, random.nextDouble() - input.car.sign)
				.scaledToMagnitude(15);

		// Choosing to fake the kickoff or not.
		timedOut = false;
		if(Behaviour.isTeammateCloser(input) || isUnfairKickoff(input)){
			fake = true;
			wildfire.sendQuickChat(true, QuickChatSelection.Custom_Useful_Faking,
					QuickChatSelection.Information_GoForIt);
		}else if(fakeKickoffs){
			if(!Behaviour.hasTeammate(input) && spawn != KickoffSpawn.CORNER){
				fake = (random.nextDouble() < fakeChance);
			}else{
				fake = false;
			}
		}else{
			fake = false;
		}

		fakeAlignPID = new PID(0.0035, 0, 0.0029);
		return true;
	}

	private void getSpawn(Vector3 position){
		int x = (int)Math.abs(position.x);
		if(x > 1100){
			spawn = KickoffSpawn.CORNER;
		}else if(x > 200){
			spawn = KickoffSpawn.CORNERBACK;
		}else{
			spawn = KickoffSpawn.FULLBACK;
		}
	}

	@Override
	public boolean expire(InfoPacket input){
		if(!fake)
			return !Behaviour.isKickoff(input);
		return input.ball.position.magnitude() > 650; // Distance from origin.
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;

		// Render the name of the kickoff's spawn position.
		String spawnString = spawn.toString();
		spawnString = spawnString.charAt(0) + spawnString.substring(1).toLowerCase();
		wildfire.renderer.drawString2d(spawnString, Color.WHITE, new Point(0, 20), 2, 2);

		// Time-out the fake kickoff if the opponent has taken too long.
		if(fake && input.elapsedSeconds - timeStarted > 12){
			fake = false;
			timedOut = true;
			wildfire.sendQuickChat(QuickChatSelection.Reactions_Okay, QuickChatSelection.Custom_Toxic_WasteCPU);
		}else if(car.position.y * car.sign > 100){
			timedOut = true;
			fake = false;
		}

		if(!fake){
			Vector2 target;
			boolean dodge, wavedash;

			if(!timedOut){
				if(spawn == KickoffSpawn.CORNERBACK && car.velocity.magnitude() < 1000){
					// Collect the boost.
					target = new Vector2(0, car.position.y * 0.62);
				}else if(spawn == KickoffSpawn.CORNER && car.velocity.magnitude() < 1800){
					// Line-up like a pro!
					target = new Vector2(0, car.position.y * 0.15);
				}else{
					if(opponentFakingKickoff(input.car, input.cars)
							&& Math.abs(randomOffset.x) < Constants.BALL_RADIUS * 0.8){
						randomOffset = new Vector2(Constants.BALL_RADIUS * 0.8
								* (randomOffset.x == 0 ? car.sign : Math.signum(randomOffset.x)), 0);
					}
					target = new Vector2(0, Constants.BALL_RADIUS * -car.sign).plus(randomOffset)
							.scaledToMagnitude(Constants.BALL_RADIUS);
				}

				dodge = (car.position.magnitude() < (spawn == KickoffSpawn.CORNER ? 760 : 800));
				wavedash = (!dodge && car.velocity.magnitude() > (spawn == KickoffSpawn.CORNER ? 1200 : 1150)
						&& !car.isSupersonic);
			}else{
				// Generic kickoff.
				target = input.ball.position.flatten();
				if(Behaviour.isTowardsOwnGoal(car, target.withZ(car.position.z))){
					target = target.plus(randomOffset
							.scaled(130 / randomOffset.x * Math.signum(randomOffset.x * (car.position.x - target.x))));
				}else{
					target = target.plus(randomOffset);
				}
				dodge = (input.info.impact.getTime() < 0.24 && car.velocity.magnitude() > 1100);
				wavedash = false;
			}

			// Rendering.
			BezierCurve bezier = new BezierCurve(car.position.flatten(),
					car.position.plus(car.orientation.forward.scaledToMagnitude(250)).flatten(), target,
					input.ball.position.flatten());
			bezier.render(wildfire.renderer, Color.WHITE);
			wildfire.renderer.drawCircle(Color.LIGHT_GRAY, target, 30);

			// Actions.
			if((dodge || wavedash) && Behaviour.isKickoff(input) && car.velocity.magnitude() > 500){
				if(dodge){
					double dodgeAngle = Handling.aim(car,
							(spawn == KickoffSpawn.CORNER
									? new Vector2(-Math.signum(car.velocity.x) * Constants.BALL_RADIUS, 0)
									: target));
					dodgeAngle = Utils.clamp(dodgeAngle * 3.5, -Math.PI, Math.PI);
					currentAction = new DodgeAction(this, dodgeAngle, input, true);
				}else{
					currentAction = new WavedashAction(this, input);
				}

				if(currentAction != null && !currentAction.failed){
					return currentAction.getOutput(input);
				}
				currentAction = null;
			}

			// Controls.
			ControlsOutput controls = Handling.forwardDrive(car, target);
			return controls.withBoost(car.hasWheelContact && car.forwardVelocity < Constants.MAX_CAR_VELOCITY - 5);
		}else{
			// Fake kickoff!
			wildfire.renderer.drawString2d("Fake", Color.WHITE, new Point(0, 40), 2, 2);

			// Get the boost in front of us.
			boolean grabBoost;
			try{
				grabBoost = (BoostManager.getBoosts().get(car.team == 0 ? 0 : 33).isActive() && car.boost < 100);
			}catch(Exception e){
				grabBoost = false; // We're probably on an irregular map.
			}

			Vector2 destination;
			if(grabBoost){
				destination = new Vector2(0, -car.sign * 4290);
				wildfire.renderer.drawCircle(Color.WHITE, destination, 50);
			}else{
				destination = Constants.homeGoal(car.team);
			}

			// Controls.
			boolean reverse = (Math.abs(car.position.y) < Math.abs(destination.y));
			double steer = fakeAlignPID.getOutput(input.elapsedSeconds, car.position.x * -car.sign, 0)
					* (reverse ? -1 : 1);
			double throttleMag = (grabBoost ? 1 : destination.distance(car.position.flatten()) / 1000);
			return new ControlsOutput().withSteer(steer).withThrottle((reverse ? -1 : 1) * throttleMag).withBoost(false)
					.withSlide(false);
		}
	}

	private boolean opponentFakingKickoff(CarData car, CarData[] cars){
		if(spawn == KickoffSpawn.CORNER)
			return false;
		double ourDistance = car.position.magnitude();
		double closestOpponent = -1;
		for(CarData opponent : cars){
			if(opponent == null || opponent.team == car.team)
				continue;
			double distance = opponent.position.magnitude();
			if(closestOpponent < 0 || distance < closestOpponent){
				closestOpponent = distance;
			}
		}
		if(closestOpponent < 0)
			return false;
		return ourDistance * 1.35 < closestOpponent;
	}

	/**
	 * Identify whether one of the opponents has a much better position to reach the
	 * kickoff before any of our teammates.
	 */
	private boolean isUnfairKickoff(InfoPacket input){
		CarData[] cars = input.cars;

		double distanceBlue = Double.MAX_VALUE, distanceOrange = Double.MAX_VALUE;
		for(int i = 0; i < cars.length; i++){
			CarData car = cars[i];
			if(car == null)
				continue;

			double carDistance = car.position.magnitude();

			if(car.team == 0){
				distanceBlue = Math.min(distanceBlue, carDistance);
			}else{
				distanceOrange = Math.min(distanceOrange, carDistance);
			}
		}

		return input.car.team == 0 ? (distanceBlue > distanceOrange + 100) : (distanceOrange > distanceBlue + 100);
	}

}
