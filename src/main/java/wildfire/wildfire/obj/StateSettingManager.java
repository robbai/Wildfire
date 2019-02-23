package wildfire.wildfire.obj;

import java.util.Random;

import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;
import wildfire.input.CarOrientation;
import wildfire.input.DataPacket;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;

public class StateSettingManager {
	
	private Wildfire wildfire;
	private float lastTime;

	public StateSettingManager(Wildfire wildfire){
		this.wildfire = wildfire;
		this.lastTime = -1000F;
	}
	
	private void resetCooldown(float lastTime){
		this.lastTime = lastTime;
	}
	
	public float getCooldown(DataPacket input){
		return input.elapsedSeconds - lastTime;
	}
	
	public void freezeBallAtOrigin(DataPacket input){
		GameState gameState = new GameState();
    	gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Utils.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired())));
    	RLBotDll.setGameState(gameState.buildPacket());    	
	}
	
	public void aerial(DataPacket input){
		if((!Utils.isBallAirborne(input.ball) || input.car.position.y > 0) && input.car.hasWheelContact && !Utils.isOnTarget(wildfire.ballPrediction, 0) && !Utils.isOnTarget(wildfire.ballPrediction, 1)){
    		GameState gameState = new GameState();
    		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(Utils.random(-3500, 3500), -4000, 10).toDesired()).withVelocity(new Vector3(Utils.random(-500, 500), Utils.random(-500, 500), 0).toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, Utils.randomRotation(), 0).toDesired())));
    		double xVel = Utils.random(-1000, 1000);
    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(Utils.clamp(-xVel * 2.75, 200 - Utils.PITCHWIDTH, Utils.PITCHWIDTH - 200), 0, 100).toDesired()).withVelocity(new Vector3(xVel, 0, Utils.random(1200, 2000)).toDesired())));
    		RLBotDll.setGameState(gameState.buildPacket());
    	}
	}

	public void dribbleCatch(DataPacket input){
		int teamSign = Utils.teamSign(input.car);
		if((input.ball.position.z < 99 && Math.signum(input.ball.velocity.y) != teamSign) || input.ball.velocity.isZero()){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(0, -800 * teamSign, 10).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, teamSign * Math.PI / 2, 0).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 1200 * teamSign, 200).toDesired()).withVelocity(new Vector3(Utils.random(-100, 100), 0, Utils.random(1000, 1500)).toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
		}
	}

	public void hop(DataPacket input) {
		boolean velocity = false;
		double distance = 2000;
		if(input.car.hasWheelContact && getCooldown(input) > 1){
			GameState gameState = new GameState();
			double angle = Utils.randomRotation();
			gameState.withCarState(wildfire.playerIndex, new CarState().withPhysics(new PhysicsState().withLocation(new Vector3(Math.sin(angle) * distance, Math.cos(angle) * distance, 10).toDesired()).withVelocity(velocity ? new Vector3(Utils.random(-1500, 1500), Utils.random(-1500, 1500), 0).toDesired() : new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, Utils.randomRotation(), 0).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Utils.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void recovery(DataPacket input) {
//		if(input.car.hasWheelContact && input.car.velocity.z > 0){
		if(input.car.hasWheelContact && input.car.velocity.flatten().magnitude() > 1500){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Utils.random(800, 1800)).toDesired()).withVelocity(new Vector3(Utils.random(-1000, 1000), Utils.random(-1000, 1000), Utils.random(600, 900)).toDesired()).withAngularVelocity(new Vector3(Utils.random(-Math.PI, Math.PI) * 2, Utils.random(-Math.PI, Math.PI) * 2, Utils.random(-Math.PI, Math.PI) * 2).toDesired()).withRotation(CarOrientation.convert(Math.PI * 2, Math.PI * 2, Math.PI * 2).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(1000, 0, Utils.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
		}
	}

	public void kickoffSpawn(DataPacket input, KickoffSpawn kickoffSpawn){
		if(getCooldown(input) > 4.5){
			boolean blue = (input.car.team == 0);
			boolean left = (new Random()).nextBoolean();
			double sideSign = (left ? (blue ? 1 : -1) : (blue ? -1 : 1));
			
			GameState gameState = new GameState();
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Utils.BALLRADIUS).toDesired()).withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired())));
			
			CarState carState = new CarState().withJumped(false).withDoubleJumped(false).withBoostAmount(33F);			
			PhysicsState carPhysics = new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired());
			switch(kickoffSpawn){
				case CORNER:
					carPhysics.withLocation(new Vector3(sideSign * 1952, Utils.teamSign(input.car) * -2464, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? (left ? 0.75 : 0.25) : (left ? -0.25 : -0.75)), 0).toDesired());
					break;
				case CORNERBACK:
					carPhysics.withLocation(new Vector3(sideSign * 256, Utils.teamSign(input.car) * -3840, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? 0.5 : -0.5), 0).toDesired());
					break;
				case FULLBACK:
					carPhysics.withLocation(new Vector3(0, Utils.teamSign(input.car) * -4608, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? 0.5 : -0.5), 0).toDesired());
					break;
				default:
					break;			
			}			
			carState.withPhysics(carPhysics);
			gameState.withCarState(input.playerIndex, carState);
			
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void orientationController(DataPacket input){
		GameState gameState = new GameState();
		gameState.withCarState(input.playerIndex, new CarState().withPhysics(new PhysicsState().withVelocity(new Vector3().toDesired()).withLocation(new Vector3(0, 0, 1500).toDesired())));    	
		PhysicsState ballPhysics = new PhysicsState().withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired());
		if(getCooldown(input) > 2.5){
			double ballDistance = 1000;
			Vector3 newBallLocation = new Vector3(Utils.random(-1, 1), Utils.random(-1, 1), Utils.random(-1, 1)).scaledToMagnitude(ballDistance);
			ballPhysics.withLocation(newBallLocation.plus(new Vector3(0, 0, 1500)).toDesired());
			resetCooldown(input.elapsedSeconds);
		}
		gameState.withBallState(new BallState().withPhysics(ballPhysics));		
		RLBotDll.setGameState(gameState.buildPacket());    	
	}

}
