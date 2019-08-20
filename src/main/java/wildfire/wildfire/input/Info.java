package wildfire.wildfire.input;

import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.utils.Behaviour;

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
	}

}