package wildfire.wildfire.states;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction2;
import wildfire.wildfire.obj.State;

public class TestState extends State {
	
	/*
	 * This state is solely for the purpose of testing actions
	 */

	public TestState(Wildfire wildfire){
		super("Test", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		return !Utils.isKickoff(input);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
//		boolean positioned = input.car.position.distanceFlat(wildfire.impactPoint.getPosition()) < wildfire.impactPoint.getPosition().z * 4;
		boolean positioned = (input.car.velocity.magnitude() > 800 && Math.abs(input.car.velocity.z) < 10);
		if(!hasAction() && input.car.hasWheelContact && positioned){
//			currentAction = new AerialAction(this, input, input.car.position.z < 500 && wildfire.impactPoint.getPosition().z > 500);			
			currentAction = AerialAction2.fromBallPrediction(this, input.car, wildfire.ballPrediction, input.car.position.z < 500 && wildfire.impactPoint.getPosition().z > 500);
			
			if(currentAction != null && !currentAction.failed){ //((AerialAction2)currentAction).averageAcceleration > Utils.BOOSTACC - 10
				return currentAction.getOutput(input); //Start overriding
			}else{
				currentAction = null;
			}
		}
		
		double steer = Utils.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double throttle = Math.cos(steer);
		return new ControlsOutput().withSteer((float)(throttle < 0 ? Utils.invertAim(steer) : -steer) * 2F).withThrottle((float)throttle).withBoost(false);
	}

}
