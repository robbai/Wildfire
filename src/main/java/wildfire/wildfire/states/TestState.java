package wildfire.wildfire.states;

import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class TestState extends FallbackState {
	
	/*
	 * This state is solely for the purpose of testing
	 */

	public TestState(Wildfire wildfire){
		super(/**"Test", */wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
//		return !Behaviour.isKickoff(input);
		return false;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		SmartDodgeAction smartDodge = new SmartDodgeAction(this, input, false);
		if(!smartDodge.failed){
			return this.startAction(smartDodge, input);
		}
		
		if(input.info.jumpImpact == null || 
				input.info.jumpImpact.getBallPosition().minus(car.position).dotProduct(car.orientation.roofVector) < 130){ 
			
			Vector2 ourWallTrace = Utils.traceToY(car.position.flatten(), input.info.impact.getBallPosition().minus(car.position).flatten(), -Utils.teamSign(car) * Constants.PITCH_LENGTH);
			
			Vector3 offset;
			if(ourWallTrace == null){
				Vector2 goal = Behaviour.getTarget(car, input.info.impact.getBallPosition().flatten());
				offset = input.info.impact.getBallPosition().flatten().minus(goal).withZ(0).scaledToMagnitude(60);
			}else{
				offset = new Vector3(-Math.signum(ourWallTrace.x) * 100, 0, 0);
			}
			
			return Handling.forwardDrive(car, input.info.impact.getBallPosition().plus(offset));
		}
				
		return Handling.arriveAtSmartDodgeCandidate(car, input.info.jumpImpact);
	}

}
