package wildfire.wildfire.obj;

import java.util.Random;

import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.DesiredRotation;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;
import wildfire.input.CarOrientation;
import wildfire.input.DataPacket;
import wildfire.vector.Vector2;
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
		if(((!Utils.isBallAirborne(input.ball) || input.car.position.y > 0) && input.car.hasWheelContact) || getCooldown(input) > 18 || Math.abs(input.ball.position.y) > 4900){
    		GameState gameState = new GameState();
    		Vector3 carPosition = new Vector3(Utils.random(-3500, 3500), -4000, 10);
    		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.toDesired()).withVelocity(new Vector3(Utils.random(-500, 500), Utils.random(-500, 500), 0).toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(new DesiredRotation(0F, (float)carPosition.flatten().correctionAngle(new Vector2()), 0F))));
    		
    		Vector3 ballVelocity = new Vector3(Utils.random(-2750, 2750), Utils.random(-1500, 1500), Utils.random(1200, 2000));
    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballVelocity.scaled(-1.5).confine().toDesired()).withVelocity(ballVelocity.toDesired())));
    		
    		RLBotDll.setGameState(gameState.buildPacket());
    		resetCooldown(input.elapsedSeconds);
    	}
	}

	public void catchTest(DataPacket input, boolean dribble){
		int teamSign = Utils.teamSign(input.car);
		if((dribble && input.ball.position.z < 99) || input.ball.position.magnitude() > 3000){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(Utils.random(-2500, 2500), Utils.random(200, 4900) * -teamSign, 10).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, teamSign * Math.PI / 2, 0).toDesired())));
			Vector2 ballLocation = new Vector2(Utils.random(-1500, 1500), Utils.random(-1500, 1500));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(ballLocation.x, ballLocation.y, 200).toDesired()).withVelocity(new Vector3(-ballLocation.x / 2, -ballLocation.y / 2, Utils.random(1000, 1500)).toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
		}
	}

	public void hop(DataPacket input){
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
			
			RLBotDll.setGameState(gameState.buildPacket());
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

	public void path(DataPacket input){
		//!input.ball.velocity.isZero() || 
		if(getCooldown(input) > 10 || Math.abs(input.ball.position.y) > 4800){ // || input.ball.position.z > Utils.BALLRADIUS + 5){
			final double border = 1000;
					
			GameState gameState = new GameState();
			Vector3 ballLocation = new Vector3(Utils.random(-Utils.PITCHWIDTH + border, Utils.PITCHWIDTH - border), Utils.random(-Utils.PITCHLENGTH + border, Utils.PITCHLENGTH - border), Utils.BALLRADIUS);
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(ballLocation.withZ(0).scaled(-0.5).toDesired()).withLocation(ballLocation.toDesired())));
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired()).withLocation(new Vector3(Utils.random(-Utils.PITCHWIDTH + border, Utils.PITCHWIDTH - border), Utils.random(-Utils.PITCHLENGTH + border, Utils.PITCHLENGTH - border), 20).toDesired())));
			
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	//TODO
//	public void rollingShot(DataPacket input){
//		if(getCooldown(input) > 10 || Utils.distanceToWall(input.ball.position) < 200 || input.ball.position.isOutOfBounds()){
//			final double carDistanceGoal = 3000;
//			
//			Vector2 enemyGoal = Utils.enemyGoal(input.car);
//			
//			Vector2 carPosition = new Vector2(Utils.random(-1, 1), Utils.random(0, -Utils.teamSign(input.car))).scaledToMagnitude(carDistanceGoal);
//			carPosition = enemyGoal.plus(carPosition);
//			
//			Vector2 carAngle = enemyGoal.minus(carPosition).normalized();
//			
//			Vector2 ballAngle = input.b
//		}
//	}

}
