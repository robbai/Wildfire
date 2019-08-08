package wildfire.wildfire.states;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.curve.*;
import wildfire.wildfire.mechanics.FollowDiscreteMechanic;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;

public class TestState extends State {
	
	/*
	 * This state is solely for the purpose of testing
	 */

	public TestState(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return !Behaviour.isKickoff(input);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		CarData car = input.car;
		Vector2 enemyGoal = Constants.enemyGoal(car);
		
		DiscreteCurve discrete = null;
		BallPrediction ballPrediction = wildfire.ballPrediction;
		for(int i = Math.max(0, wildfire.impactPoint.getFrame() - 1); i < ballPrediction.slicesLength(); i++){
			Vector2 ballPosition = Vector2.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			double time = (ballPrediction.slices(i).gameSeconds() - car.elapsedSeconds);
			
			Curve curve = new Biarc(car.position.flatten(), car.orientation.noseVector.flatten(), ballPosition.plus(ballPosition.minus(enemyGoal).scaledToMagnitude(Constants.BALLRADIUS)), enemyGoal.minus(ballPosition));
//			Curve curve = new BezierCurve(car.position.flatten(), ballPosition.plus(ballPosition.minus(enemyGoal).scaledToMagnitude(car.position.flatten().distance(ballPosition) * 0.75)), ballPosition.plus(ballPosition.minus(enemyGoal).scaledToMagnitude(Constants.BALLRADIUS)));
			
			discrete = new DiscreteCurve(car.forwardVelocity, car.boost, curve);
			if(discrete.getTime() < time || input.ball.velocity.magnitude() < 1) break;
		}
		
		return this.startMechanic(new FollowDiscreteMechanic(this, discrete, input.elapsedSeconds, true), input);
	}

}
