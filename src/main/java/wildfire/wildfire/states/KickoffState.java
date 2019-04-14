package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.obj.BezierCurve;
import wildfire.wildfire.obj.KickoffSpawn;
import wildfire.wildfire.obj.State;

public class KickoffState extends State {
	
	/**
	 * Used for enabling/disabling the chance of fake kickoffs
	 */
	private final boolean fakeKickoffs = true;
	
	private KickoffSpawn spawn;
	private Random random;
		
	private float timeStarted;
	private boolean timedOut;
	private boolean fake;

	public KickoffState(Wildfire wildfire){
		super("Kickoff", wildfire);
		random = new Random();
	}

	@Override
	public boolean ready(DataPacket input){
		if(!Utils.isKickoff(input)) return false;
		
		wildfire.unlimitedBoost = (input.car.boost > 99);
		getSpawn(input.car.position);
		timeStarted = input.elapsedSeconds;

		//Choosing to fake
		timedOut = false;
		if(Utils.isTeammateCloser(input)){
			fake = true;
			wildfire.sendQuickChat(true, QuickChatSelection.Custom_Useful_Faking, QuickChatSelection.Information_GoForIt);
		}else if(fakeKickoffs){
			fake = ((random.nextFloat() < 0.2F || isUnfairKickoff(input)) && !Utils.hasTeammate(input) && spawn != KickoffSpawn.CORNER && Utils.hasOpponent(input));
//			fake = spawn != KickoffSpawn.CORNER;
		}else{
			fake = false;
		}
		
		return true;
	}
	
	private void getSpawn(Vector3 position){
		int x = (int)Math.abs(position.x);
		if(x > 1200){
			spawn = KickoffSpawn.CORNER;
		}else if(x > 200){
			spawn = KickoffSpawn.CORNERBACK;
		}else{
			spawn = KickoffSpawn.FULLBACK;
		}
	}

	@Override
	public boolean expire(DataPacket input){
		if(!fake) return !Utils.isKickoff(input);
		return input.ball.position.magnitude() > 650; //Distance from origin
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(fake) wildfire.renderer.drawString2d("Fake", Color.WHITE, new Point(0, 20), 2, 2);
		
		//Time-out the fake kickoff if the opponent has taken too long
		if(fake && input.elapsedSeconds - timeStarted > 12){
			fake = false;
			timedOut = true;
			wildfire.sendQuickChat(QuickChatSelection.Reactions_Okay, QuickChatSelection.Custom_Toxic_WasteCPU);
		}
		
		if(!fake){
			Vector2 target;
			boolean dodge;
			
			if(!timedOut){
				if(spawn == KickoffSpawn.CORNER){
					dodge = input.car.position.magnitude() < 2420;
					target = new Vector2(0, dodge ? 0 : input.car.position.y * 0.28);
				}else if(spawn == KickoffSpawn.CORNERBACK){
					dodge = Math.abs(input.car.position.x) < 170;
					target = new Vector2(0, dodge ? 0 : input.car.position.y * 0.7);
				}else{
					target = input.ball.position.flatten();
					double distance = input.car.position.magnitude();
					dodge = (distance < 3900 && distance > 3500) || distance < 2100;
				}
			}else{
				//Generic kickoff
				target = input.ball.position.flatten(); 
				dodge = (input.car.position.magnitude() < 1200 && input.car.velocity.magnitude() > 1000);
			}
			
			//Render
			BezierCurve bezier = new BezierCurve(input.car.position.flatten(), 
					input.car.position.plus(input.car.orientation.noseVector.scaledToMagnitude(250)).flatten(), 
					target, 
					input.ball.position.flatten());
			bezier.render(wildfire.renderer, Color.WHITE);
			wildfire.renderer.drawCircle(Color.LIGHT_GRAY, target, 30);
			
			if(!hasAction() && dodge && Utils.isKickoff(input) && input.car.velocity.magnitude() > 500){
				currentAction = new DodgeAction(this, Utils.aim(input.car, input.ball.position.flatten()) * 2.9F, input);
				currentAction.failed = false;
				if(!currentAction.failed) return currentAction.getOutput(input); //Start overriding
			}
			
			double steerCorrectionRadians = Utils.aim(input.car, target);
	        return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(1).withBoost(true);
		}else{
			//Fake
			if(!BoostManager.getBoosts().get(input.car.team == 0 ? 0 : 33).isActive() || input.car.boost > 99){
				
				boolean reverse = (Math.abs(input.car.position.y) < 5200);
				double steerCorrectionRadians = Utils.aim(input.car, reverse ? Utils.homeGoal(input.car.team) : wildfire.impactPoint.getPosition().flatten());
				if(reverse){
					steerCorrectionRadians = Utils.invertAim(steerCorrectionRadians);
					double forwardMagnitude = input.car.forwardMagnitude();
			        return new ControlsOutput().withSteer((float)steerCorrectionRadians * 3F).withThrottle(-1F).withBoost(false).withSlide(forwardMagnitude < -200 && forwardMagnitude > -600);
				}else{
					return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(1F).withBoost(false).withSlide(false);
				}
			}else{
				
				//Get the boost in front of us
				Vector2 boost = new Vector2(0, -Utils.teamSign(input.car) * 4290);
				wildfire.renderer.drawCircle(Color.WHITE, boost, 50);
				double steerCorrectionRadians = Utils.aim(input.car, boost);
				if(Math.cos(steerCorrectionRadians) > 0){
					return new ControlsOutput().withSteer((float)steerCorrectionRadians * -3F).withThrottle(1F).withBoost(false);
				}else{
					return new ControlsOutput().withSteer((float)Utils.invertAim(steerCorrectionRadians) * 1.5F).withThrottle(-1F).withBoost(false);
				}
			}
		}
	}
	
	private boolean isUnfairKickoff(DataPacket input){
		double distanceBlue = Double.MAX_VALUE, distanceOrange = Double.MAX_VALUE;
		for(byte i = 0; i < input.cars.length; i++){
			if(input.cars[i] == null) continue;
			CarData car = input.cars[i];
			if(input.car.team == 0){
				distanceBlue = Math.min(distanceBlue, car.position.magnitude());
			}else{
				distanceOrange = Math.min(distanceOrange, car.position.magnitude());
			}
		}
//		System.out.println("Blue: " + (int)distanceBlue + ", Orange: " + (int)distanceOrange + " (" + input.cars.length + " Cars)");
		return input.car.team == 0 ? (distanceBlue > distanceOrange + 100) : (distanceOrange > distanceBlue + 100);
	}

}
