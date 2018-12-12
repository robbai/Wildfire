package rlbotexample.wildfire.states;

import java.awt.Color;

import rlbotexample.input.CarData;
import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.vector.Vector3;
import rlbotexample.wildfire.State;
import rlbotexample.wildfire.Utils;
import rlbotexample.wildfire.Wildfire;

public class TestState2 extends State {
	
	/*
	 * This state just drives towards the centre of the arena,
	 * and performs an action when it gets close enough,
	 * this is for the purpose of testing actions
	 */

	public TestState2(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return true;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
//		if(input.car.position.magnitude() < 2000 && !hasAction()){
//			currentAction = new HopAction(this, input.car.position.flatten().scaled(2), input.car.velocity);
//			if(!currentAction.failed) return currentAction.getOutput(input); 
//		}
		
		final float sharpness = 2F;
		
//		double steerCorrectionRadians = Utils.aim(input.car, new Vector2(0, 0));
		double steerCorrectionRadians = Utils.aim(input.car, wildfire.impactPoint.flatten());
		double steer = (float)-steerCorrectionRadians * sharpness;
//		steer += Math.sin((double)System.currentTimeMillis() / 1000D);
		
		//Render
		for(CarData c : input.cars){
			if(c == null) continue;
			Vector3 start = c.position;
			double scale = 0.05;
			Vector2 rotation = c.velocity.scaled(scale).flatten();
			for(int i = 0; i < 100; i++){
				double s = (float)-Utils.aimFromPoint(start.flatten(), rotation, input.ball.position.flatten()) * sharpness;
				rotation = rotation.rotate(-Utils.clamp(s) / (0.4193 / scale));
				Vector3 end = start.plus(rotation.withZ(0));
				wildfire.renderer.drawLine3d(i % 2 == 0 ? (c.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, start.toFramework(), end.toFramework());
				start = end;
			}
		}
		
		//return new ControlsOutput().withSteer0F).withThrottle((float)-input.car.forwardMagnitude() / 100).withBoost(false);
        return new ControlsOutput().withSteer((float)steer).withThrottle(1F).withBoost(true);
	}

}
