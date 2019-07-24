package wildfire.wildfire.states;

import java.awt.Color;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.curve.Biarc;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Physics;

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
		Biarc biarc = new Biarc(input.car.position.flatten(), input.car.orientation.noseVector.flatten(), input.ball.position.flatten(), Constants.enemyGoal(input.car).minus(input.ball.position.flatten()));
		biarc.render(wildfire.renderer, true);
		
		double t = 0.225;
		Vector2 target = biarc.T(t);
		wildfire.renderer.drawCircle(Color.GREEN, target, 25);
		
		double steer = Handling.aimLocally(input.car, target) * -3;
		Pair<Double, Double> radii = biarc.getRadii();
		double targetVelocity = Math.max(250, Physics.getSpeedFromRadius(Math.max(radii.getOne(), radii.getTwo())));
		double throttle = Handling.produceAcceleration(input.car, (targetVelocity - input.car.velocity.magnitude()) * 60);
		return new ControlsOutput().withSteer(steer).withThrottle(throttle).withBoost(throttle > 1);
	}

}
