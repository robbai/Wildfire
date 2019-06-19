package wildfire.wildfire.states;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.AerialAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class TestState extends State {
	
	/*
	 * This state is solely for the purpose of testing actions
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
		if(!hasAction() && input.car.hasWheelContact && (input.car.velocity.magnitude() > 800 || input.car.position.z > 400) && (input.ball.position.z > 800 || input.car.position.z > 2000)){		
			currentAction = AerialAction.fromBallPrediction(this, input.car, wildfire.ballPrediction, false);
			
			if(currentAction != null && !currentAction.failed){
				return currentAction.getOutput(input); //Start overriding
			}else{
				currentAction = null;
			}
		}
		
		double steer = Handling.aim(input.car, wildfire.impactPoint.getPosition().flatten());
		double throttle = Math.signum(Math.cos(steer));
		return new ControlsOutput().withSteer((float)(throttle < 0 ? Utils.invertAim(steer) : -steer) * 3F).withThrottle((float)throttle).withBoost(throttle > 0.9 && !Behaviour.isBallAirborne(input.ball));
	}

}
