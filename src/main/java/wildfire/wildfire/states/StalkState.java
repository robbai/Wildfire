package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.BallData;
import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class StalkState extends State {

	/*
	 * Wait for the opponent to clear the ball
	 */
	
	private final double dangerZoneSize = 4000, confine = 500;
	
	private CarData defender;
	private double defenderDistance, defenderVelocity;

	private Vector2 enemyGoal;

	public StalkState(Wildfire wildfire){
		super("Stalk", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(!input.car.hasWheelContact || Behaviour.isOnWall(input.car) 
				|| !Behaviour.correctSideOfTarget(input.car, input.info.impact.getPosition())) return false;
		
		// We want to hit it!
		if(input.info.impact.getTime() < 0.6) return false;
		
		// It's not in their danger zone.
		enemyGoal = Constants.enemyGoal(input.car);
		if(input.ball.position.distanceFlat(enemyGoal) > dangerZoneSize) return false;
		
		// Grab the defender.
		Pair<CarData, Pair<Double, Double>> enemy = getDefender(input.cars, input.ball);
		this.defender = enemy.getOne();
		this.defenderDistance = enemy.getTwo().getOne();
		this.defenderVelocity = enemy.getTwo().getTwo();
		
		return this.defender != null
//				&& this.defenderDistance < input.car.position.distanceFlat(input.ball.position) * (input.car.isSupersonic ? 1.2 : 1.4);
				&& this.defenderDistance < input.car.position.distanceFlat(input.ball.position) * (Behaviour.correctSideOfTarget(this.defender, input.ball.position) ? 1.2 : 0.65);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		Vector2 defenderPosition = defender.position.flatten();
		
		// How far should we be?
		double sitbackDistance = getSitbackDistance();
		
		// Find the "optimal" position
		Vector2 defenderToBall = input.ball.position.flatten().minus(defenderPosition);
		Vector2 destination = input.ball.position.flatten().plus(defenderToBall.scaledToMagnitude(sitbackDistance));
		if(Math.abs(destination.x) > Constants.PITCHWIDTH - confine) {
			destination = destination.plus(new Vector2(-destination.x + Math.signum(destination.x) * (Constants.PITCHWIDTH - confine), 
					Math.signum(defenderToBall.y) * (Math.abs(destination.x) - (Constants.PITCHWIDTH - confine))));
		}
		destination = destination.confine(confine);
		double destinationDistance = input.car.position.distanceFlat(destination);
		
		// Render
		wildfire.renderer.drawString2d("Sitback Distance: " + (int)sitbackDistance + "uu", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawCircle(Color.GRAY, destination, 120);
		wildfire.renderer.drawLine3d(Color.BLACK, defenderPosition, Utils.traceToWall(defenderPosition, defenderToBall));
		wildfire.renderer.drawCircle(Color.RED, enemyGoal, dangerZoneSize);
		
		// Controller
		ControlsOutput controller = Handling.arriveDestination(input.car, destination, false);
		double throttleCap = (destinationDistance / 2000);
		return controller.withThrottle(Utils.clamp(controller.getThrottle(), -throttleCap, throttleCap));
	}
	
	private double getSitbackDistance(){
		return 2400 + (Math.max(this.defenderVelocity / 1.2, 1200) + this.defenderDistance / 1.6) * 1.25;
	}

	private Pair<CarData, Pair<Double, Double>> getDefender(CarData[] cars, BallData ball){
		Vector2 ballPosition = ball.position.flatten();
		boolean backboard = (Math.abs(ballPosition.y) > 4700 && Math.abs(ballPosition.x) > 1000);
		
		/*
		 * Best defender info
		 */
		CarData bestDefender = null;
		double bestDistance = 0, bestVelocity = 0;
		
		for(CarData car : cars){
			if(car == null || car.team == wildfire.team) continue;
			
			// Some conditions.
			if(!car.hasWheelContact) continue;
			if(!backboard) continue;
//			if(!backboard && !Behaviour.correctSideOfTarget(car, ballPosition)) continue;
						
			double ballDistance = car.position.distanceFlat(ballPosition);
			double velocity = car.velocity.flatten().magnitudeInDirection(ballPosition.minus(car.position.flatten()));
			
//			if((bestDefender == null && velocity > -1100)
			if(bestDefender == null
					|| (ballDistance < bestDistance || velocity > bestVelocity * 2.5)){
				bestDefender = car;
				bestDistance = ballDistance;
				bestVelocity = velocity;
			}
		}
		
		return new Pair<CarData, Pair<Double, Double>>(bestDefender, new Pair<Double, Double>(bestDistance, bestVelocity));
	}

}
