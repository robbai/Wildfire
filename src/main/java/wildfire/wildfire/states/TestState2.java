package wildfire.wildfire.states;

import java.awt.Color;

import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.obj.State;

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
		double aim = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		
		if(!hasAction() && wildfire.impactPoint.getTime() < 1.25 && Math.cos(aim) < 0){
			currentAction = new HalfFlipAction(this, input.elapsedSeconds);
			if(!currentAction.failed){
				return currentAction.getOutput(input); 
			}
		}
		
		boolean forward = input.car.forwardMagnitude() > 1100;
		
		if(forward){
			aim = -aim;
		}else{
			aim = Utils.invertAim(aim);
		}
		
		return new ControlsOutput().withSteer((float)aim * 3F).withBoost(false).withThrottle(forward ? 1 : -1);
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
				double s = (float)-Utils.aimFromPoint(start.flatten(), rotation, input.ball.position.flatten()) * sharpness;
				rotation = rotation.rotate(-Utils.clampSign(s) / (0.4193 / scale));
				Vector3 end = start.plus(rotation.withZ(0));
				wildfire.renderer.drawLine3d(i % 2 == 0 ? (c.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, start.toFramework(), end.toFramework());
				start = end;
			}
		}
	}

}
