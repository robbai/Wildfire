package wildfire.wildfire.states;

import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class BoostTestState extends BoostState {

	private final double maxBoost = 55;

	public BoostTestState(Wildfire wildfire){
		super("Boost (Greedy)", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(input.car.boost > maxBoost) return false;
		boost = getBoost(input);
		return boost != null;
	}

	private BoostPad getBoost(InfoPacket input){
		Impact impact = input.info.impact;
		double impactTime = impact.getTime();
		if(Math.abs(impact.getPosition().y) > (Constants.PITCH_LENGTH - 1100)) impactTime *= 1.2;
		if(Math.abs(impact.getPosition().x) > (Constants.PITCH_WIDTH - 950)) impactTime *= 1.3;
		if(impact.getPosition().z > 260) impactTime *= 1.35;
		
		boolean carCorrectSide = Behaviour.correctSideOfTarget(input.car, impact.getBallPosition());
		
		BoostPad bestBoost = null;
		double bestBoostTime = 0;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			if(!boost.isActive()) continue;
			
			boolean boostCorrectSide = ((boost.getLocation().y - impact.getBallPosition().y) * Utils.teamSign(input.car) < 0);
			
			double distance = boost.getLocation().distance(input.car.position);
			double maxVel = DrivePhysics.maxVelocityDist(input.car.velocityDir(boost.getLocation().minus(input.car.position)), input.car.boost, distance);
			double travelTime = DrivePhysics.minTravelTime(input.car, distance);
			Impact newImpact = Behaviour.getEarliestImpactPoint(boost.getLocation(), 100, boost.getLocation().minus(input.car.position).scaledToMagnitude(maxVel), input.car.elapsedSeconds + travelTime, wildfire.ballPrediction);
			if(newImpact == null) continue;
			travelTime += newImpact.getTime();
			
			if(!boostCorrectSide) travelTime *= (carCorrectSide ? 1.4 : 1.3);
			
			if(travelTime < impactTime && (bestBoost == null || travelTime < bestBoostTime - 0.05)){
				bestBoost = boost;
				bestBoostTime = travelTime;
			}
		}
		
		return bestBoost;
	}

}