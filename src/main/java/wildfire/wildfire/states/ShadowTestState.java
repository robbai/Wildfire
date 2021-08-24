package wildfire.wildfire.states;

import wildfire.input.car.CarData;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.utils.Behaviour;

public class ShadowTestState extends ShadowState {

	public ShadowTestState(Wildfire wildfire){
		super("Shadow (Test)", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		CarData car = input.car;
		Vector3 impactPosition = input.info.impact.getPosition();
//		Vector3 localImpact = Utils.toLocal(car, impactPosition);
//		Vector3 toImpact = impactPosition.minus(car.position);
//		return localImpact.normalized().y * Utils.teamSign(car) < (Constants.PITCHLENGTH - input.info.impact.getPosition().y * Utils.teamSign(car)) / Constants.PITCHLENGTH;
//		return localImpact.normalized().y * Utils.teamSign(car) < Math.abs(impactPosition.x) / Constants.PITCHWIDTH;
		return (Math.abs(input.info.impactRadians) > Math.toDegrees(110) && input.info.impact.getTime() > 1.1)
				|| (!Behaviour.correctSideOfTarget(car, impactPosition) && Math.abs(impactPosition.x) > 1400);
	}

	@Override
	public boolean expire(InfoPacket input){
		return !this.ready(input);
	}

}
