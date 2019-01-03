package wildfire.wildfire.obj;

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

	public void airDribble(DataPacket input){
		if(input.ball.position.z < 99){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(0, -800, 10).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, Math.PI / 2, 0).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 200, 200).toDesired()).withVelocity(new Vector3(0, 0, Utils.random(1000, 1500)).toDesired())));
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

}
