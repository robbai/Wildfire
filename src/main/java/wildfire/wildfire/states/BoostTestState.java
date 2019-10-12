package wildfire.wildfire.states;

import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.InterceptCalculator;

public class BoostTestState extends BoostState {

	private final double maxBoost = 55;

	public BoostTestState(Wildfire wildfire){
		super("Boost (Greedy)", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		if(input.info.isBoostPickupInevitable) return false;
		if(Math.abs(input.car.position.y) > Constants.PITCH_LENGTH) return false;
		if(input.car.boost > maxBoost) return false;
		boost = getBoost(input);
		return boost != null;
	}
	
	@Override
	public boolean expire(InfoPacket input){
		return !this.ready(input);
	}

	private BoostPad getBoost(InfoPacket input){
		Impact impact = input.info.impact;
		double impactTime = impact.getTime();
		if(Math.abs(impact.getPosition().y) > (Constants.PITCH_LENGTH - 1100)) impactTime *= 1.15;
		if(Math.abs(impact.getPosition().x) > (Constants.PITCH_WIDTH - 950)) impactTime *= 1.2;
		if(impact.getPosition().z > 260) impactTime *= 1.2;
		
		boolean carCorrectSide = Behaviour.correctSideOfTarget(input.car, impact.getBallPosition());
		
		BoostPad bestBoost = null;
		double bestBoostTime = 0;
		for(BoostPad boost : BoostManager.getFullBoosts()){
			if(!boost.isActive()) continue;
			
			double distance = boost.getLocation().distance(input.car.position);
			double maxVel = DrivePhysics.maxVelocityDist(input.car.velocityDir(boost.getLocation().minus(input.car.position)), input.car.boost, distance);
			double travelTime = DrivePhysics.minTravelTime(input.car, distance);
			Impact newImpact = InterceptCalculator.getImpact(boost.getLocation(), 100, boost.getLocation().minus(input.car.position).scaledToMagnitude(maxVel).withZ(0), input.car.elapsedSeconds + travelTime);
			if(newImpact == null) continue;
			travelTime += newImpact.getTime();
			
			boolean boostCorrectSide = ((boost.getLocation().y - newImpact.getBallPosition().y) * input.car.sign < 0);
			
//			if(!boostCorrectSide) travelTime *= (carCorrectSide ? 1.5 : 1.45);
			if(!boostCorrectSide && carCorrectSide) continue;
//			if(!boostCorrectSide) continue;
			
			if(travelTime < impactTime && (bestBoost == null || travelTime < bestBoostTime + 0.05)){
				bestBoost = boost;
				bestBoostTime = travelTime;
			}
		}
		
		return bestBoost;
	}

}