package rlbotexample.wildfire.states;

import java.awt.Color;

import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;
import rlbotexample.wildfire.actions.AerialAction;

public class TestState extends State {
	
	/*
	 * This state just drives towards the centre of the arena,
	 * and performs an action when it gets close enough,
	 * this is for the purpose of testing actions
	 */

	public TestState(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return true;
//		return input.ball.position.flatten().isZero() || (wildfire.impactPoint.z > 200 && input.car.boost > 20D * (wildfire.impactPoint.distanceFlat(input.car.position) / 2300D));
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double steer = -Utils.aim(input.car, wildfire.impactPoint.flatten()) * 2;
//		double steer = -Utils.aim(input.car, input.car.position.flatten().scaled(2)) * 2;
		
		boolean onTarget = Math.abs(Utils.aim(input.car, wildfire.impactPoint.flatten()) - Utils.aim(input.car, new Vector2(0, Utils.PITCHLENGTH * Utils.teamSign(input.car)))) < 0.4;
		
		double distance = input.car.position.distanceFlat(wildfire.impactPoint);
		double maxDistance = input.ball.position.z * 5.75;
		Utils.drawCircle(wildfire.renderer, Color.YELLOW, wildfire.impactPoint.flatten(), maxDistance);
		if(!hasAction() && Math.abs(steer) < 0.8 && onTarget && ((distance > 300 && distance < maxDistance) || (input.car.position.z > 800 && input.car.hasWheelContact)) && Utils.isBallAirborne(input.ball)){
			currentAction = new AerialAction(this, input, (wildfire.impactPoint.z > 800 || input.ball.position.isZero()) && input.car.position.z < 1500);
			if(!currentAction.failed) return currentAction.getOutput(input); //Start overriding
		}
		
        return new ControlsOutput().withSteer((float)steer).withThrottle(1F).withBoost(!Utils.isBallAirborne(input.ball) && input.car.hasWheelContact).withSlide(Math.abs(steer) > 2);
//		return new ControlsOutput().withSteer(0F).withThrottle(1F).withBoost(input.car.position.z < 600).withSlide(false);
	}

}
