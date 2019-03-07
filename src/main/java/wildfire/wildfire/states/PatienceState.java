package wildfire.wildfire.states;

import java.awt.Color;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class PatienceState extends State {
	
	private Vector3 point;

	/**
	 * Don't rush the ball if it's on the backboard
	 */

	public PatienceState(Wildfire wildfire){
		super("Patience", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		point = null;
		
		double impactDistance = input.car.position.distanceFlat(wildfire.impactPoint.getPosition());
		if(impactDistance > 4500) return false;
		
		if(Utils.isInCone(input.car, wildfire.impactPoint.getPosition()) || Math.abs(wildfire.impactPoint.getPosition().x) < 1200) return false;
		if(wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < 4500 || input.car.position.z > 200) return false;
		
		int startFrame = wildfire.impactPoint.getFrame();
		for(int i = startFrame; i < Math.min(wildfire.ballPrediction.slicesLength(), startFrame + 250); i++){
			Vector3 location = Vector3.fromFlatbuffer(wildfire.ballPrediction.slices(i).physics().location());
			if(Math.abs(location.x) < 1100 && (location.y * Utils.teamSign(input.car) < 4200 || location.z < 600)){
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, startFrame, i);
				wildfire.renderer.drawCrosshair(input.car, location, Color.green, 50);
				point = location;
				return true;
			}
		}
		
		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		double steer = Utils.aim(input.car, point.flatten());
		if(Math.abs(steer) > Math.toRadians(40)){
			return new ControlsOutput().withNone().withSteer((float)steer * -3F).withBoost(false).withThrottle(1);
		}else{
			boolean slow = (input.car.forwardMagnitude() > 1100 || input.car.position.distanceFlat(Utils.enemyGoal(input.car.team)) < 2600);
			return new ControlsOutput().withNone().withSteer((float)steer * -3F).withBoost(false).withThrottle(slow ? 0 : 1);
		}
	}

}
