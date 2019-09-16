package wildfire.wildfire.input;

import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Info {

	public Wildfire wildfire;
	
	public BallPrediction ballPrediction;
	public WRenderer renderer;
	
	private Impact[] impacts;
	public Impact impact;
	public double teammateImpactTime, enemyImpactTime;

	private double timeFirstOnGround;
	private boolean onGroundLast;
	public double timeOnGround;

	public double impactDistance, impactDistanceFlat;

	public Impact jumpImpact;

	public double impactRadians;

	public Info(Wildfire wildfire){
		this.wildfire = wildfire;
		this.impacts = null;
		this.timeFirstOnGround = 0;
		this.onGroundLast = false;
		this.timeOnGround = 0;
	}

	public void update(DataPacket input){
		// Get a renderer.
		this.renderer = new WRenderer(this.wildfire, !Behaviour.hasTeammate(input) && wildfire.isTestVersion(), wildfire.isTestVersion());
    	
    	// Get the ball prediction.
    	try{
    		this.ballPrediction = RLBotDll.getBallPrediction();
    	}catch(RLBotInterfaceException e){
    		e.printStackTrace();
    		this.ballPrediction = null;
    	}
		
		CarData car = input.car;
		
		// Impact calculation.
		if(this.impacts == null) this.impacts = new Impact[input.cars.length];
		this.teammateImpactTime = Double.MAX_VALUE;
		this.enemyImpactTime = Double.MAX_VALUE;
		try{
			for(int i = 0; i < input.cars.length; i++){
				Impact impact = Behaviour.getEarliestImpactPoint(input.cars[i], ballPrediction);
				this.impacts[i] = impact;
				
				if(i == this.wildfire.playerIndex){
					this.impact = impact;
				}else if(input.cars[i].team == this.wildfire.team){
					this.teammateImpactTime = Math.min(this.teammateImpactTime, impact.getTime());
				}else{
					this.enemyImpactTime = Math.min(this.enemyImpactTime, impact.getTime());
				}
			}
			
			this.impactDistance = car.position.distance(this.impact.getPosition());
			this.impactDistanceFlat = car.position.distance(this.impact.getPosition());
			this.impactRadians = Handling.aim(car, this.impact.getPosition());
		}catch(Exception e){
			e.printStackTrace();
			
			this.impact = null;
			this.impactDistance = 0;
			this.impactDistanceFlat = 0;
		}
		
		// Wheel-contact checking.
		if(!this.onGroundLast && input.car.hasWheelContact){
			this.timeFirstOnGround = input.elapsedSeconds;
		}
		this.onGroundLast = input.car.hasWheelContact;
		this.timeOnGround = (input.elapsedSeconds - this.timeFirstOnGround);
		
		this.jumpImpact = getJumpImpact(car);
	}

	private Impact getJumpImpact(CarData car){
		for(int i = 0; i < ballPrediction.slicesLength(); i++){
			rlbot.flat.PredictionSlice rawSlice = ballPrediction.slices(i);
			
			Vector3 slicePosition = Vector3.fromFlatbuffer(rawSlice.physics().location());
			Vector3 localSlice = Utils.toLocal(car, slicePosition);
			
			double time = (rawSlice.gameSeconds() - car.elapsedSeconds);
			double jumpHeight = localSlice.z;
			if(jumpHeight > JumpPhysics.maxJumpHeight + (Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance) * (SmartDodgeAction.zRatio * 0.72)) continue;
//			if(jumpHeight > JumpPhysics.maxJumpHeight) continue;
			double peakTime = JumpPhysics.getFastestTimeZ(jumpHeight);
			double driveTime = (time - peakTime);
			double fullDistance = localSlice.flatten().magnitude();
			double initialVelocity = car.velocityDir(slicePosition.minus(car.position).flatten());
			double finalVelocity = (2 * fullDistance - driveTime * initialVelocity) / (driveTime + 2 * peakTime);
			double acceleration = ((finalVelocity - initialVelocity) / driveTime);
			
			if(finalVelocity < DrivePhysics.maxVelocity(initialVelocity, car.boost, time) && acceleration < 1800){
//				Vector3 impactPosition = slicePosition.plus(car.position.minus(slicePosition).scaledToMagnitude(Constants.BALLRADIUS));
				Vector3 impactPosition = slicePosition.plus(slicePosition.minus(Constants.enemyGoal(car).withZ(slicePosition.z)).scaledToMagnitude(Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance * 0.405));
//				Vector3 impactPosition = slicePosition;
				return new Impact(impactPosition, rawSlice, time);
			}
		}
		
		return null; // Uh oh.
	}

}