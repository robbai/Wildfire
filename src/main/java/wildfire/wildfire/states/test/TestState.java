package wildfire.wildfire.states.test;

import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.actions.HalfFlipAction;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.states.FallbackState;
import wildfire.wildfire.utils.Behaviour;

public class TestState extends FallbackState {

	private float randomTime;

	/*
	 * This state is solely for the purpose of testing
	 */

	public TestState(Wildfire wildfire){
		super(/** "Test", */
				wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
//		return !Behaviour.isKickoff(input);
//		return false;
		return true;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;

		Vector3 target = (input.info.impact == null ? input.ball.position : input.info.impact.getPosition());

		if(!car.hasWheelContact)
			this.startAction(new RecoveryAction(this, car.elapsedSeconds), input);

		if(car.onFlatGround && Math.abs(car.angularVelocity.yaw) < 1){
			if(car.forwardVelocity < -700){
				HalfFlipAction halfFlip = new HalfFlipAction(this, input);
				if(!halfFlip.failed){
					return this.startAction(halfFlip, input);
				}
			}else if(car.forwardVelocity > 1200 && car.forwardVelocity < 2000){
				if((Behaviour.dodgeDistance(car) < target.distance(car.position) && car.boost < 1)
						|| input.info.impact.getTime() < Behaviour.IMPACT_DODGE_TIME){
					DodgeAction dodge = new DodgeAction(this, Handling.aim(car, target), input);
					if(!dodge.failed){
						return this.startAction(dodge, input);
					}
				}
			}
		}

		if(((input.ball.hasBeenTouched && input.car.elapsedSeconds - input.ball.touch.gameSeconds > 20)
				|| !input.ball.hasBeenTouched) && input.car.elapsedSeconds - this.randomTime > 20){
			this.randomTime = input.car.elapsedSeconds;
			return ControlsOutput.random();
		}

//		return Handling.backwardDrive(car, target);
		return Handling.chaosDrive(car, target, true);
	}

}
