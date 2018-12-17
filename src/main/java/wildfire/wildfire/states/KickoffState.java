package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.KickoffSpawn;
import wildfire.wildfire.State;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;

public class KickoffState extends State {
	
	private Random random;
	private boolean fake = false;
	private KickoffSpawn spawn;

	public KickoffState(Wildfire wildfire){
		super("Kickoff", wildfire);
		random = new Random();
	}

	@Override
	public boolean ready(DataPacket input){
		boolean ready = Utils.isKickoff(input);
		getSpawn(input.car.position);
		if(ready) fake = Utils.isTeammateCloser(input) || (random.nextFloat() < 0.2F && !Utils.hasTeammate(input) && spawn != KickoffSpawn.CORNER && Utils.hasOpponent(input));
		return ready;
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
		wildfire.renderer.drawString2d(fake ? "Fake" : "Normal", Color.WHITE, new Point(0, 20), 2, 2);
		if(!fake){
			Vector2 target;
			boolean dodge;
			
			if(spawn == KickoffSpawn.CORNER){
				dodge = input.car.position.magnitude() < 2500;
				target = new Vector2(0, dodge ? 0 : input.car.position.y * 0.28);
			}else if(spawn == KickoffSpawn.CORNERBACK){
				dodge = Math.abs(input.car.position.x) < 130;
				target = new Vector2(0, dodge ? 0 : input.car.position.y * 0.7);
			}else{
				target = input.ball.position.flatten();
				double distance = input.car.position.magnitude();
				dodge = (distance < 3900 && distance > 3500) || distance < 2100;
			}
			
			//Render
			wildfire.renderer.drawLine3d(Color.WHITE, input.car.position.flatten().toFramework(), target.toFramework());
			Utils.drawCircle(wildfire.renderer, Color.WHITE, target, 70);
			
			if(!hasAction() && dodge && Utils.isKickoff(input)){
				currentAction = new DodgeAction(this, Utils.aim(input.car, input.ball.position.flatten()) * 2.9F, input);
				currentAction.failed = false;
				if(!currentAction.failed) return currentAction.getOutput(input); //Start overriding
			}
			
			double steerCorrectionRadians = Utils.aim(input.car, target);
	        return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(1).withBoost(true);
		}else{
			//Fake
			if(input.car.boost > 40){
				boolean reverse = (Math.abs(input.car.position.y) < 5200);
				double steerCorrectionRadians = Utils.aim(input.car, reverse ? Utils.homeGoal(input.car.team) : wildfire.impactPoint.flatten());
				if(reverse){
					steerCorrectionRadians = Utils.invertAim(steerCorrectionRadians);
			        return new ControlsOutput().withSteer((float)steerCorrectionRadians).withThrottle(-1F).withBoost(false);
				}else{
					return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle(1F).withBoost(false);
				}
			}else{
				//Get the boost in front of us
				Vector2 boost = new Vector2(0, -Utils.teamSign(input.car) * 4290);
				Utils.drawCircle(wildfire.renderer, Color.WHITE, boost, 90);
				double steerCorrectionRadians = Utils.aim(input.car, boost);
				return new ControlsOutput().withSteer((float)-steerCorrectionRadians * 2F).withThrottle((float)Math.cos(steerCorrectionRadians)).withBoost(false);
			}
		}
	}

}
