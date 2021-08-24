package wildfire.wildfire.input;

import java.util.ArrayList;

import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import wildfire.boost.BoostManager;
import wildfire.boost.BoostPad;
import wildfire.input.DataPacket;
import wildfire.input.car.CarData;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.SmartDodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.Impact;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.physics.JumpPhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.InterceptCalculator;

public class Info {

	public static final double maxJumpImpactZ = (JumpPhysics.maxJumpHeight
			+ (Constants.BALL_RADIUS + SmartDodgeAction.dodgeDistance) * (SmartDodgeAction.zRatio * 0.51));

	public Wildfire wildfire;

	public BallPrediction ballPrediction;
	public WRenderer renderer;

	public Impact[] impacts;
	public Impact impact, earliestEnemyImpact, jumpImpact;
	public double teammateImpactTime, enemyImpactTime;
	public CarData earliestEnemy;
	public double impactDistance, impactDistanceFlat, impactRadians, jumpImpactHeight;

	private double timeFirstOnGround;
	private boolean onGroundLast;
	public double timeOnGround;

	private double timeFirstDodged;
	private boolean didDodgeLast;
	private double timeSinceDodge;

	public boolean isBoostPickupInevitable;

	public Info(Wildfire wildfire){
		this.wildfire = wildfire;
		this.impacts = null;
		this.timeFirstOnGround = 0;
		this.onGroundLast = false;
		this.timeOnGround = 0;
	}

	public void update(DataPacket input){
		// Get a renderer.
		this.renderer = new WRenderer(this.wildfire, !Behaviour.hasTeammate(input) && wildfire.isTestVersion(),
				wildfire.isTestVersion());

		// Get the ball prediction.
		try{
			this.ballPrediction = RLBotDll.getBallPrediction();
		}catch(RLBotInterfaceException e){
			e.printStackTrace();
			this.ballPrediction = null;
		}

		CarData car = input.car;

		// Impact calculation.
		if(this.impacts == null)
			this.impacts = new Impact[input.cars.length];
		this.teammateImpactTime = Double.MAX_VALUE;
		this.enemyImpactTime = Double.MAX_VALUE;
		try{
			for(int i = 0; i < input.cars.length; i++){
				CarData c = input.cars[i];
				if(c == null)
					continue;

				Impact impact = InterceptCalculator.getImpact(c);
				this.impacts[i] = impact;

				if(i == this.wildfire.playerIndex){
					this.impact = impact;
				}else if(c.team == this.wildfire.team){
					this.teammateImpactTime = Math.min(this.teammateImpactTime, impact.getTime());
				}else{
					if(impact.getTime() < this.enemyImpactTime){
						this.enemyImpactTime = impact.getTime();
						this.earliestEnemy = c;
						this.earliestEnemyImpact = impact;
					}
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

		// Dodge checking.
		if(!this.didDodgeLast && input.car.hasDoubleJumped){
			this.timeFirstDodged = input.elapsedSeconds;
		}
		this.didDodgeLast = input.car.hasDoubleJumped;
		this.timeFirstDodged = (input.elapsedSeconds - this.timeFirstDodged);

		this.jumpImpact = InterceptCalculator.getJumpImpact(car);
		this.jumpImpactHeight = (this.jumpImpact == null ? 0
				: this.jumpImpact.getBallPosition().minus(car.position).dotProduct(car.orientation.up));

		this.isBoostPickupInevitable = isPickupInevitable(car, BoostManager.getFullBoosts());
	}

	public boolean isDodgeTorquing(){
		return this.timeSinceDodge < Constants.DODGE_TORQUE_TIME;
	}

	private boolean isPickupInevitable(CarData car, BoostPad boostPad){
		double u = car.velocityDir(boostPad.getLocation().minus(car.position));
		double s = boostPad.getLocation().distance(car.position);
		double a = (Math.pow(u, 2) / (2 * s));
		return Math.abs(a) > Constants.BRAKE_ACCELERATION;
	}

	private boolean isPickupInevitable(CarData car, ArrayList<BoostPad> boostPads){
		for(BoostPad boostPad : boostPads){
			if(isPickupInevitable(car, boostPad)){
				return true;
			}
		}
		return false;
	}

}
