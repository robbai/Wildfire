package wildfire.wildfire.states;

import java.awt.Color;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.WavedashAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class TestState2 extends State {
	
	/*
	 * Action testing state
	 */
	
	@SuppressWarnings("unused")
	private static final Vector2 origin = new Vector2();

	public TestState2(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return true;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		double aim = Handling.aim(input.car, new Vector2(Math.signum(input.car.velocity.x) * 1000, Math.signum(input.car.velocity.y) * 1000));
		
		if(!hasAction() && wildfire.impactPoint.getTime() < 1.25 && Math.cos(aim) < 0){
			currentAction = new WavedashAction(this, input);
			if(!currentAction.failed){
				return currentAction.getOutput(input); 
			}else{
				currentAction = null;
			}
		}
		
		return new ControlsOutput().withSteer((float)aim * -3F).withBoost(input.car.velocity.magnitude() < 2000).withThrottle(1);
	}
	
	@SuppressWarnings("unused")
	private void coolRender(DataPacket input){
		final float sharpness = 2F;
		for(CarData c : input.cars){
			if(c == null) continue;
			Vector3 start = c.position;
			double scale = 0.05;
			Vector2 rotation = c.velocity.scaled(scale).flatten();
			for(int i = 0; i < 100; i++){
				double s = (float)-Handling.aimFromPoint(start.flatten(), rotation, input.ball.position.flatten()) * sharpness;
				rotation = rotation.rotate(-Utils.clampSign(s) / (0.4193 / scale));
				Vector3 end = start.plus(rotation.withZ(0));
				wildfire.renderer.drawLine3d(i % 2 == 0 ? (c.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, start.toFramework(), end.toFramework());
				start = end;
			}
		}
	}

}
