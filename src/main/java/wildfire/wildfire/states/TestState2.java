package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.PredictionSlice;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.BezierCurve;
import wildfire.wildfire.obj.DiscreteCurve;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class TestState2 extends State {
	
	/*
	 * Testing state
	 */
	
	private final double steerUnits = 410, speedUnits = 20;
	private final boolean verboseRender = false, dodge = true;

	public TestState2(Wildfire wildfire){
		super("Test2", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && !input.car.hasWheelContact){
			this.currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return this.currentAction.getOutput(input);
		}
		
		double u = input.car.forwardMagnitude();
		Vector2[] points = null;
		DiscreteCurve curve = null;
		if(input.ball.velocity.isZero()){
			// Get the points.
			points = getPoints(input.car, input.ball.position);
		
			// Get the curve.
			curve = new DiscreteCurve(u, wildfire.unlimitedBoost ? -1 : input.car.boost, points, Double.MAX_VALUE);
		}else{
			for(int i = wildfire.impactPoint.getFrame(); i < wildfire.ballPrediction.slicesLength(); i++){
				PredictionSlice slice = wildfire.ballPrediction.slices(i);
				double time = slice.gameSeconds() - input.elapsedSeconds;
				
				// Get the points.
				points = getPoints(input.car, Vector3.fromFlatbuffer(slice.physics().location()));
			
				// Get the curve.
				curve = new DiscreteCurve(u, wildfire.unlimitedBoost ? -1 : input.car.boost, points, time);
				if(!curve.isValid()) continue;
				if(curve.getTime() <= time
						|| Vector3.fromFlatbuffer(slice.physics().velocity()).isZero()) {
					wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.BLACK, 0, i);
					break;
				}
			}
//			return Handling.atba(input, input.ball.position); 
		}
		
		
		// Get some info.
		Vector2 target = curve.S(Math.min(curve.getDistance(), steerUnits));
		if(target == null) return Handling.atba(input, input.ball.position); 
		double acc = curve.getAcceleration(speedUnits / curve.getDistance());
		
		// Render.
		if(verboseRender){
			wildfire.renderer.drawString2d("Current Vel.: " + (int)u + "uu/s", Color.WHITE, new Point(0, 80), 2, 2);
			wildfire.renderer.drawString2d("Desired Acc.: " + (int)acc + "uu/s^2", Color.WHITE, new Point(0, 100), 2, 2);
		}
		wildfire.renderer.drawString2d("Distance: " + (int)curve.getDistance() + "uu", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Time: " + Utils.round(curve.getTime()) + "s", Color.WHITE, new Point(0, 40), 2, 2);
		curve.render(wildfire.renderer, Color.BLUE);
		wildfire.renderer.drawCircle(Color.CYAN, target, 10);
		wildfire.renderer.drawCircle(Color.RED, curve.S(Math.min(curve.getDistance(), speedUnits)), 5);
		
		// Handling.
		double radians = Handling.aimLocally(input.car, target);
		if(!hasAction() && Math.abs(radians) < 0.4 && dodge){
			boolean dodgeIntoBall = (curve.getTime() < 0.22);
//			boolean dodgeForSpeed = (curve.getTime() > 1.4 && curve.getSpeed(700 / curve.getDistance()) - u > 500 && !input.car.isSupersonic);
			if(dodgeIntoBall){
				this.currentAction = new DodgeAction(this, radians * 2.5, input);
				return this.currentAction.getOutput(input);
			}
		}
		double curveCorrect = (input.car.orientation.noseVector.flatten().angle(points[1].minus(points[0])));
		double throttle = Handling.produceAcceleration(input.car, acc);
		return new ControlsOutput()
				.withSteer(radians * -3)
				.withThrottle(throttle)
				.withBoost(throttle > 1 && input.car.hasWheelContact 
					&& (curveCorrect < Math.PI * 0.65 || wildfire.stateSetting.getCooldown(input) > 1))
				.withSlide(curveCorrect > Math.PI * 0.85 && wildfire.stateSetting.getCooldown(input) < 1);
	}
	
	/**
	 * Just an example for a discrete curve.
	 */
	private Vector2[] getPoints(CarData car, Vector3 ball){
		Vector2 carLocation = car.position.flatten();
		Vector2 ballLocation = ball.flatten();
		Vector2 enemyGoal = Constants.enemyGoal(car);
		
//		ArrayList<Vector2> points = new ArrayList<Vector2>(); 
//		double distance = carLocation.distance(ballLocation);
//		while(distance > Constants.BALLRADIUS){
//			double angle = enemyGoal.minus(ballLocation).angle(carLocation.minus(ballLocation).scaled(-1));
//			double offset = (Math.max(distance, Math.sin(angle) * 2200) * angle * Math.max(angle, 1) * 0.9);
//			Vector2 destination = ballLocation.plus(ballLocation.minus(enemyGoal).scaledToMagnitude(offset));
//			
//			carLocation = carLocation.plus(destination.minus(carLocation).scaledToMagnitude(Math.min(40, 15 + distance - Constants.BALLRADIUS)));
//			points.add(carLocation);
//			
//			distance = carLocation.distance(ballLocation);			
//		}
//		return points.toArray(new Vector2[points.size()]);
		
		double distance = carLocation.distance(ballLocation);
		double angle = enemyGoal.minus(ballLocation).angle(carLocation.minus(ballLocation).scaled(-1));
		double offset = (distance * angle * 0.9);
		Vector2 destination = ballLocation.plus(ballLocation.minus(enemyGoal).scaledToMagnitude(offset));
		BezierCurve bezier = new BezierCurve(
				carLocation,
				carLocation.plus(car.orientation.noseVector.flatten().scaledToMagnitude(Utils.clamp((distance - Constants.BALLRADIUS) * 0.25, 0, 400))),
				destination,
				ballLocation.plus(ballLocation.minus(enemyGoal).scaledToMagnitude(Constants.BALLRADIUS))
				);
		return bezier.discrete(50);
	}

}