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
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.pitch.Pitch;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class StateSettingManager {
	
	private Wildfire wildfire;
	private float lastTime;
	private Random random;

	public StateSettingManager(Wildfire wildfire){
		this.wildfire = wildfire;
		this.lastTime = -1000F;
		this.random = new Random();
	}
	
	private void resetCooldown(float lastTime){
		this.lastTime = lastTime;
	}
	
	public float getCooldown(DataPacket input){
		return input.elapsedSeconds - lastTime;
	}
	
	public void freezeBallAtOrigin(DataPacket input){
		GameState gameState = new GameState();
    	gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired())));
    	RLBotDll.setGameState(gameState.buildPacket());    	
	}
	
	public void aerial(DataPacket input){
		if(((!Behaviour.isBallAirborne(input.ball) || Utils.teamSign(input.car) * input.car.position.y > 0) && input.car.hasWheelContact) || getCooldown(input) > 18 || Math.abs(input.ball.position.y) > 4900){
    		GameState gameState = new GameState();
    		Vector3 carPosition = new Vector3(Utils.random(-3500, 3500), Utils.random(3000, 5000) * -Utils.teamSign(input.car), 10);
    		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.toDesired()).withVelocity(new Vector3(Utils.random(-500, 500), Utils.random(-500, 500), 0).toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(new DesiredRotation(0F, (float)carPosition.flatten().correctionAngle(new Vector2()), 0F))));
    		
    		Vector3 ballVelocity = new Vector3(Utils.random(-2000, 2000), Utils.random(-1500, 1500), Utils.random(1200, 2000));
    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballVelocity.scaled(-2).confine().toDesired()).withVelocity(ballVelocity.toDesired())));
    		
    		RLBotDll.setGameState(gameState.buildPacket());
    		resetCooldown(input.elapsedSeconds);
    	}
	}

	public void catchTest(DataPacket input, boolean dribble){
		int teamSign = Utils.teamSign(input.car);
		if((dribble && input.ball.position.z < 99) || input.ball.position.magnitude() > 3000){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(Utils.random(-2500, 2500), Utils.random(3000, 4900) * -teamSign, 10).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, teamSign * Math.PI / 2, 0).toDesired())));
			Vector2 ballLocation = new Vector2(Utils.random(-1500, 1500), Utils.random(-1500, 1500));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(ballLocation.x, ballLocation.y, 200).toDesired()).withVelocity(new Vector3(-ballLocation.x / 2, -ballLocation.y / 2, Utils.random(1000, 1400)).toDesired())));
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
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void recovery(DataPacket input){
		if(input.car.hasWheelContact && (input.car.velocity.flatten().magnitude() > 900 || getCooldown(input) > 5) && input.car.position.z < 1000){
			GameState gameState = new GameState();
			double horizontalVelocity = 3000;
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withDoubleJumped(true).withPhysics(new PhysicsState()
					.withLocation(new Vector3(0, 0, 800).toDesired())
					.withVelocity(new Vector3(Utils.random(-horizontalVelocity, horizontalVelocity), Utils.random(-horizontalVelocity, horizontalVelocity), Utils.random(300, 1900)).toDesired())
					.withAngularVelocity(new Vector3(Utils.random(-Math.PI, Math.PI) * 2, Utils.random(-Math.PI, Math.PI) * 2, Utils.random(-Math.PI, Math.PI) * 2).toDesired())
					.withRotation(CarOrientation.convert(Math.PI * 2, Math.PI * 2, Math.PI * 2).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState()
					.withLocation(new Vector3(Utils.random(-500, 500), Utils.random(-500, 500), Constants.BALLRADIUS).toDesired())
					.withVelocity(new Vector3().toDesired())
					.withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void kickoffSpawn(DataPacket input, KickoffSpawn kickoffSpawn){
		if(input.ball.position.magnitude() > (input.car.hasWheelContact ? 1000 : 3500)){
			boolean blue = (input.car.team == 0);
			boolean left = (new Random()).nextBoolean();
			double sideSign = (left ? (blue ? 1 : -1) : (blue ? -1 : 1));
			
			GameState gameState = new GameState();
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALLRADIUS).toDesired()).withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired())));
			
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

	public void orient(DataPacket input, boolean smoothMove){
		final double ballDistance = 1000, z = 1100;
		
		GameState gameState = new GameState();
		gameState.withCarState(input.playerIndex, new CarState().withPhysics(new PhysicsState()
				.withVelocity(new Vector3().toDesired())
				.withLocation(new Vector3(0, 0, z).toDesired())));    	
		PhysicsState ballPhysics = new PhysicsState().withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired());
		
		// Every 2.5 seconds.
		if(getCooldown(input) > 2.5){
			if(!smoothMove){
				Vector3 newBallLocation = new Vector3(Utils.random(-1, 1), Utils.random(-1, 1), Utils.random(-1, 1)).scaledToMagnitude(ballDistance);
				ballPhysics.withLocation(newBallLocation.plus(new Vector3(0, 0, z)).toDesired());
			}
			resetCooldown(input.elapsedSeconds);
		}
		
		if(smoothMove){
			double angle = input.elapsedSeconds / 1.25;
			Vector3 ballLocation = new Vector3(Math.sin(angle) * ballDistance, 
					Math.cos(angle * 1.5) * ballDistance, z + Math.sin(angle * 2.5) * z / 2);
			ballPhysics.withLocation(ballLocation.toDesired());
		}
		
		gameState.withBallState(new BallState().withPhysics(ballPhysics));		
		RLBotDll.setGameState(gameState.buildPacket());    	
	}

	public void path(DataPacket input, boolean boost){
		//!input.ball.velocity.isZero() || 
		if(getCooldown(input) > (input.car.velocity.magnitude() < 100 ? 2 : 10) || Math.abs(input.ball.position.y) > 4800){ // || input.ball.position.z > Utils.BALLRADIUS + 5){
			final double border = 1000;
					
			GameState gameState = new GameState();
			Vector3 ballLocation = new Vector3(Utils.random(-Constants.PITCHWIDTH + border, Constants.PITCHWIDTH - border), Utils.random(-Constants.PITCHLENGTH + border, Constants.PITCHLENGTH - border), Constants.BALLRADIUS);
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(ballLocation.withZ(0).scaled(-0.5).toDesired()).withLocation(ballLocation.toDesired())));
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(boost ? 100F : 0).withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired()).withLocation(new Vector3(Utils.random(-Constants.PITCHWIDTH + border, Constants.PITCHWIDTH - border), Utils.random(-Constants.PITCHLENGTH + border, Constants.PITCHLENGTH - border), 20).toDesired())));
			
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void shoot(DataPacket input, boolean simpleRoll){
		if(getCooldown(input) > 6 || Utils.distanceToWall(input.ball.position) < 200 || input.ball.velocity.magnitude() < 10){
			final double carDistanceGoal = Utils.random(2500, 4800), ballSpeed = Utils.random(500, 2000), secondsToMiddle = Utils.random(1.25, 3.5);
			
			Vector2 enemyGoal = Constants.enemyGoal(input.car);
			
			//The position of the car
			Vector2 carPosition = null;
			while(!isInsideArena(carPosition)){
				carPosition = new Vector2(Utils.random(-1, 1), Utils.random(0, -Utils.teamSign(input.car))).scaledToMagnitude(carDistanceGoal);
				carPosition = enemyGoal.plus(carPosition).confine(500);
			}
			
			//The direction that the car will face
			Vector2 carDirection = carPosition.minus(enemyGoal).normalized();
			if(!simpleRoll) carDirection = carDirection.plus(new Vector2(Utils.random(-1, 1), Utils.random(-1, 1)).scaledToMagnitude(2)).normalized();
			
			Vector2 ballPosition = null;
			Vector2 ballDirection = null;
			while((simpleRoll && ballPosition == null) || !isInsideArena(ballPosition)){
				//The direction that the ball will roll
				if(simpleRoll){
					ballDirection = carDirection.rotate(Math.signum(random.nextDouble() - 0.5) * Math.PI / 2).scaledToMagnitude(ballSpeed);
				}else{
					ballDirection = carDirection.rotate((random.nextDouble() - 0.5) * Math.PI * 0.75).scaledToMagnitude(ballSpeed);
				}
			
				//The position of the ball
				ballPosition = carPosition.plus(enemyGoal).scaled(0.5);
				if(!ballDirection.isZero()) ballPosition = ballPosition.plus(ballDirection.scaledToMagnitude(-ballSpeed * secondsToMiddle));
				ballPosition = ballPosition.confine(500);
			}
			
			GameState gameState = new GameState();
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.withZ(30).toDesired()).withRotation(CarOrientation.fromVector(carDirection).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballPosition.withZ(Constants.BALLRADIUS).toDesired()).withVelocity(ballDirection.toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void clear(DataPacket input) {
		if((input.ball.velocity.y * Utils.teamSign(input.car) > 200 && input.car.hasWheelContact && input.ball.velocity.z < 500) || Behaviour.isKickoff(input)){
			Vector2 goal = Constants.homeGoal(input.car.team);
			
			Vector3 carPosition = goal.withZ(30);
//			Vector2 carDirection = carPosition.flatten().scaled(-1).normalized();
			Vector2 carDirection = new Vector2(Utils.random(-1, 1), Utils.random(-1, 1)).normalized();
			
			double ballDistance = Utils.random(3600, 6000), ballSpeed = 2500;
			Vector2 ballPosition = goal.plus(new Vector2(Utils.random(-1, 1), Utils.random(0.5, 1) * Utils.teamSign(input.car)).scaledToMagnitude(ballDistance)).confine(Constants.BALLRADIUS);
			Vector3 ballVelocity = goal.minus(ballPosition).withZ(Utils.random(0, 1200)).scaledToMagnitude(ballSpeed);
			
			GameState gameState = new GameState();
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.toDesired()).withRotation(CarOrientation.fromVector(carDirection).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballPosition.withZ(Constants.BALLRADIUS).toDesired()).withVelocity(ballVelocity.toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void resetToKickoff(DataPacket input){
		if(input.ball.position.magnitude() < 2000 || getCooldown(input) < 6) return;
		GameState gameState = new GameState();
		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, (Constants.PITCHLENGTH + Constants.BALLRADIUS + 10) * Math.signum(input.ball.position.y), Constants.BALLRADIUS).toDesired())));
		RLBotDll.setGameState(gameState.buildPacket());
		resetCooldown(input.elapsedSeconds);
	}
	
	private boolean isInsideArena(Vector3 vec){
		if(vec == null || vec.confine().distance(vec) > 50) return false;
		return Pitch.segmentIntersect(new Vector3(0, 0, Constants.CEILING / 2), vec) == null;
	}
	
	private boolean isInsideArena(Vector2 vec){
		if(vec == null) return false;
		return isInsideArena(vec.withZ(Constants.BALLRADIUS));
	}

}
