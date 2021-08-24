package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.Physics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class StalkState extends State {

	/*
	 * Wait for the opponent to clear the ball, then pounce!
	 */

	private final double dangerZoneMinSize = 1000, dangerZoneMaxSize = 3200, confine = 500;

	private int defender; // Index.
	private double defenderDistance, defenderVelocity;

	private Vector2 enemyGoal;

	public StalkState(Wildfire wildfire){
		super("Stalk", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		boolean correctSide = Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition());
		if(!input.car.onFlatGround)
			return false;

//		// We want to hit it!
		if(input.info.impact.getTime() < (correctSide ? 0.35 : 0.2))
			return false;

		if(input.info.impact.getTime() < input.info.enemyImpactTime - 0.06)
			return false;

		// It's not in their danger zone.
		enemyGoal = Constants.enemyGoal(input.car);
		double impactGoalDistance = input.info.impact.getPosition().distanceFlat(enemyGoal);
		if(impactGoalDistance < dangerZoneMinSize || impactGoalDistance > dangerZoneMaxSize)
			return false;

		// Grab the defender.
		Pair<Integer, Pair<Double, Double>> enemy = getDefender(input.cars, input.info.impacts);
		this.defender = enemy.getOne();
		this.defenderDistance = enemy.getTwo().getOne();
		this.defenderVelocity = enemy.getTwo().getTwo();

		return this.defender != -1
//				&& this.defenderDistance < input.car.position.distanceFlat(input.ball.position) * (input.car.isSupersonic ? 1.2 : 1.4);
//				&& this.defenderDistance < input.car.position.distanceFlat(input.ball.position) * (Behaviour.correctSideOfTarget(input.cars[defender], input.ball.position) ? 1.1 : 0.8)
		;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		Vector2 defenderPosition = input.cars[defender].position.flatten();

		// How far should we be?
		double sitbackDistance = getSitbackDistance(input.car.position.distanceFlat(defenderPosition));

		// Find the "optimal" position
		Vector2 defenderImpact = input.info.impacts[defender].getPosition().flatten();
		Vector2 defenderToBall = defenderImpact.minus(defenderPosition);
		Vector2 destination = defenderImpact.normalised().plus(Vector2.Y.scaled(-car.sign))
				.plus(defenderToBall.scaledToMagnitude(sitbackDistance));
		if(Math.abs(destination.x) > Constants.PITCH_WIDTH - confine){
			destination = destination.plus(new Vector2(
					-destination.x + Math.signum(destination.x) * (Constants.PITCH_WIDTH - confine),
					Math.signum(defenderToBall.y) * (Math.abs(destination.x) - (Constants.PITCH_WIDTH - confine))));
		}
		destination = destination.confine(confine);
		double destinationDistance = car.position.distanceFlat(destination);

		// Render
		wildfire.renderer.drawString2d("Sitback Distance: " + (int)sitbackDistance + "uu", Color.WHITE,
				new Point(0, 20), 2, 2);
		wildfire.renderer.drawCircle(Color.GRAY, destination, 120);
		wildfire.renderer.drawLine3d(Color.BLACK, defenderPosition,
				Utils.traceToWall(defenderPosition, defenderToBall));
		wildfire.renderer.drawCircle(Color.RED, enemyGoal, dangerZoneMinSize);
		wildfire.renderer.drawCircle(Color.RED, enemyGoal, dangerZoneMaxSize);

		// Controller
//		ControlsOutput controls = Handling.chaosDrive(car, destination, false);
		ControlsOutput controls = Handling.forwardDrive(car, destination);
		double throttleCap = (destinationDistance / 2000);
		return controls.withThrottle(Utils.clamp(controls.getThrottle(), -throttleCap, throttleCap))
				.withBoost(controls.holdBoost() && throttleCap > 0.9);
	}

	private double getSitbackDistance(double carDistance){
		return 1300 + (Math.max(this.defenderVelocity / 1.4, 1100) + this.defenderDistance / 1.8) * 0.4
				+ carDistance * 0.2;
	}

	private Pair<Integer, Pair<Double, Double>> getDefender(CarData[] cars, Impact[] impacts){
		/*
		 * Best defender info
		 */
		int bestDefender = -1;
		double bestDistance = 0, bestVelocity = 0;

		for(int i = 0; i < cars.length; i++){
			CarData car = cars[i];
			Impact impact = impacts[i];
			if(car == null || car.team == wildfire.team)
				continue;

			Vector2 ballPosition = impact.getBallPosition().flatten();

			// Some conditions.
			boolean backboard = (Math.abs(ballPosition.y) > 4700 && Math.abs(ballPosition.x) > 1000);
			if(Physics.timeToHitGround(car) > 0.5)
				continue;
//			if(!backboard) continue;
			if(!backboard && !Behaviour.correctSideOfTarget(car, ballPosition))
				continue;

			double ballDistance = car.position.distanceFlat(ballPosition);
			double velocity = car.velocity.flatten().magnitudeInDirection(ballPosition.minus(car.position.flatten()));

//			if((bestDefender == null && velocity > -1100)
			if(bestDefender == -1 || (ballDistance < bestDistance || velocity > bestVelocity * 1.5)){
				bestDefender = i;
				bestDistance = ballDistance;
				bestVelocity = velocity;
			}
		}

		return new Pair<Integer, Pair<Double, Double>>(bestDefender,
				new Pair<Double, Double>(bestDistance, bestVelocity));
	}

}
