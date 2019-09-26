package wildfire.wildfire.obj;

import java.util.Random;

import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.DesiredRotation;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;
import wildfire.input.CarData;
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
    	gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALL_RADIUS).toDesired()).withVelocity(new Vector3().toDesired())));
    	RLBotDll.setGameState(gameState.buildPacket());    	
	}
	
	public void aerial(DataPacket input, boolean close){
		if(((!Behaviour.isBallAirborne(input.ball) || input.car.sign * input.car.position.y > 0) && input.car.hasWheelContact && input.car.position.z < 500) || getCooldown(input) > 18 || Math.abs(input.ball.position.y) > 4900){
    		GameState gameState = new GameState();
    		Vector3 carPosition = new Vector3((close ? 0.5 : 1) * random(-3500, 3500), (close ? 0.5 : 1) * random(3000, 5000) * -input.car.sign, 18);
    		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.toDesired()).withVelocity(new Vector3(random(-500, 500), random(-500, 500), 0).toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(new DesiredRotation(0F, (float)carPosition.flatten().correctionAngle(new Vector2()), 0F))));
    		
    		Vector3 ballVelocity = new Vector3(random(-2000, 2000), random(-1500, 1500), random(1200, 2000));
    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballVelocity.scaled(-2).confine().toDesired()).withVelocity(ballVelocity.toDesired())));
    		
    		RLBotDll.setGameState(gameState.buildPacket());
    		resetCooldown(input.elapsedSeconds);
    	}
	}

	public void catchBall(DataPacket input, boolean dribble){
		if((dribble && input.ball.position.z < 99) || input.ball.position.magnitude() > 3000){
			GameState gameState = new GameState();
			gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(random(-2500, 2500), random(500, 4900) * -input.car.sign, 10).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, input.car.sign * Math.PI / 2, 0).toDesired())));
			Vector2 ballLocation = new Vector2(random(-1500, 1500), random(-1500, 1500));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(ballLocation.x, ballLocation.y, 200).toDesired()).withVelocity(new Vector3(-ballLocation.x / 2, -ballLocation.y / 2, random(1000, 1400)).toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
		}
	}
	
	public void airRoll(DataPacket input){
		if(Math.abs(input.ball.position.y) < Constants.PITCH_LENGTH - 150 && this.getCooldown(input) < 8) return; //input.ball.position.z > 100 && 
		
		GameState gameState = new GameState();
		
		double teamSign = input.car.sign;
		
		double ballSignX = Math.signum(this.random.nextDouble() - 0.5);
		Vector3 ballPosition = new Vector3((Constants.PITCH_WIDTH - random(700, 1200)) * ballSignX, (Constants.PITCH_LENGTH - random(800, 1200)) * teamSign, Constants.BALL_RADIUS);
		Vector3 ballVelocity = new Vector3(random(800, 1600) * -ballSignX, random(0, 500) * -teamSign, random(1000, 1700));
		Vector3 ballAngVelocity = Vector3.random(100);
		gameState.withBallState(new BallState().withPhysics(new PhysicsState()
				.withLocation(ballPosition.toDesired())
				.withVelocity(ballVelocity.toDesired())
				.withAngularVelocity(ballAngVelocity.toDesired())));
		
		Vector3 carPosition = new Vector3(random(-1, 1) * (Constants.PITCH_WIDTH - 1000), random(0, 3500) * teamSign, Constants.CAR_HEIGHT);
		Vector3 carVelocity = new Vector3(0, 0, -100);
		Vector3 carAngVelocity = new Vector3();
		CarOrientation carOrientation = CarOrientation.fromVector(new Vector2(0, teamSign));
		float boost = 100;
		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(boost).withPhysics(new PhysicsState()
				.withLocation(carPosition.toDesired())
				.withVelocity(carVelocity.toDesired())
				.withRotation(carOrientation.toDesired())
				.withAngularVelocity(carAngVelocity.toDesired())));
		
		RLBotDll.setGameState(gameState.buildPacket());
		
		resetCooldown(input.elapsedSeconds);
	}

	public void hop(DataPacket input){
		boolean velocity = false;
		double distance = 2000;
		if(input.car.hasWheelContact && getCooldown(input) > 1){
			GameState gameState = new GameState();
			double angle = randomRotation();
			gameState.withCarState(wildfire.playerIndex, new CarState().withPhysics(new PhysicsState().withLocation(new Vector3(Math.sin(angle) * distance, Math.cos(angle) * distance, 10).toDesired()).withVelocity(velocity ? new Vector3(random(-1500, 1500), random(-1500, 1500), 0).toDesired() : new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, randomRotation(), 0).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALL_RADIUS).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
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
					.withVelocity(new Vector3(random(-horizontalVelocity, horizontalVelocity), random(-horizontalVelocity, horizontalVelocity), random(300, 1900)).toDesired())
					.withAngularVelocity(new Vector3(random(-Math.PI, Math.PI) * 2, random(-Math.PI, Math.PI) * 2, random(-Math.PI, Math.PI) * 2).toDesired())
					.withRotation(CarOrientation.convert(Math.PI * 2, Math.PI * 2, Math.PI * 2).toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState()
					.withLocation(new Vector3(random(-500, 500), random(-500, 500), Constants.BALL_RADIUS).toDesired())
					.withVelocity(new Vector3().toDesired())
					.withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}
	
	public double randomRotation(){
    	return random(-Math.PI, Math.PI);
    }
	
	public double random(double min, double max){
		return (this.random.nextDouble() * Math.abs(max - min) + Math.min(min, max));
	}

	public void kickoffSpawn(DataPacket input, KickoffSpawn kickoffSpawn){
		if(input.ball.position.magnitude() > (input.car.hasWheelContact ? 1000 : 3500)){
			boolean blue = (input.car.team == 0);
			boolean left = (new Random()).nextBoolean();
			double sideSign = (left ? (blue ? 1 : -1) : (blue ? -1 : 1));
			
			GameState gameState = new GameState();
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Constants.BALL_RADIUS).toDesired()).withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired())));
			
			CarState carState = new CarState().withJumped(false).withDoubleJumped(false).withBoostAmount(33F);			
			PhysicsState carPhysics = new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired());
			switch(kickoffSpawn){
				case CORNER:
					carPhysics.withLocation(new Vector3(sideSign * 1952, input.car.sign * -2464, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? (left ? 0.75 : 0.25) : (left ? -0.25 : -0.75)), 0).toDesired());
					break;
				case CORNERBACK:
					carPhysics.withLocation(new Vector3(sideSign * 256, input.car.sign * -3840, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? 0.5 : -0.5), 0).toDesired());
					break;
				case FULLBACK:
					carPhysics.withLocation(new Vector3(0, input.car.sign * -4608, 40).toDesired()).withRotation(CarOrientation.convert(0, Math.PI * (blue ? 0.5 : -0.5), 0).toDesired());
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
				Vector3 newBallLocation = new Vector3(random(-1, 1), random(-1, 1), random(-1, 1)).scaledToMagnitude(ballDistance);
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
		path(input, boost, false);
	}

	public void path(DataPacket input, boolean boost, boolean ballStill){
		//!input.ball.velocity.isZero() || 
		if(getCooldown(input) > (input.car.velocity.magnitude() < 50 ? 2 : (ballStill ? 16 : 20)) || (Math.abs(input.ball.position.y) > 4700 && Math.abs(input.ball.position.x) < 1000)){ // || input.ball.position.z > Utils.BALLRADIUS + 5){
			final double border = 1000;
					
			GameState gameState = new GameState();
			Vector3 ballLocation = new Vector3(random(-Constants.PITCH_WIDTH + border, Constants.PITCH_WIDTH - border), random(-Constants.PITCH_LENGTH + border, Constants.PITCH_LENGTH - border), Constants.BALL_RADIUS);
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(ballStill ? new Vector3().toDesired() : ballLocation.withZ(0).scaled(-0.5).toDesired()).withLocation(ballLocation.toDesired())));
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(boost ? 100F : 0).withPhysics(new PhysicsState().withAngularVelocity(new Vector3().toDesired()).withVelocity(new Vector3().toDesired()).withLocation(new Vector3(random(-Constants.PITCH_WIDTH + border, Constants.PITCH_WIDTH - border), random(-Constants.PITCH_LENGTH + border, Constants.PITCH_LENGTH - border), 20).toDesired())));
			
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void shoot(DataPacket input, boolean simpleRoll){
		if(getCooldown(input) > 6 || Utils.distanceToWall(input.ball.position) < 200 || input.ball.velocity.magnitude() < 10){
			final double carDistanceGoal = random(2500, 4800), ballSpeed = random(500, 2000), secondsToMiddle = random(1.25, 3.5);
			
			Vector2 enemyGoal = Constants.enemyGoal(input.car);
			
			//The position of the car
			Vector2 carPosition = null;
			while(!isInsideArena(carPosition)){
				carPosition = new Vector2(random(-1, 1), random(0, -input.car.sign)).scaledToMagnitude(carDistanceGoal);
				carPosition = enemyGoal.plus(carPosition).confine(500);
			}
			
			//The direction that the car will face
			Vector2 carDirection = carPosition.minus(enemyGoal).normalized();
			if(!simpleRoll) carDirection = carDirection.plus(new Vector2(random(-1, 1), random(-1, 1)).scaledToMagnitude(2)).normalized();
			
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
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballPosition.withZ(Constants.BALL_RADIUS).toDesired()).withVelocity(ballDirection.toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void clear(DataPacket input) {
		if((input.ball.velocity.y * input.car.sign > 200 && input.car.hasWheelContact && input.ball.velocity.z < 500) || Behaviour.isKickoff(input)){
			Vector2 goal = Constants.homeGoal(input.car.team);
			
			Vector3 carPosition = goal.withZ(30);
//			Vector2 carDirection = carPosition.flatten().scaled(-1).normalized();
			Vector2 carDirection = new Vector2(random(-1, 1), random(-1, 1)).normalized();
			
			double ballDistance = random(3600, 6000), ballSpeed = 2500;
			Vector2 ballPosition = goal.plus(new Vector2(random(-1, 1), random(0.5, 1) * input.car.sign).scaledToMagnitude(ballDistance)).confine(Constants.BALL_RADIUS);
			Vector3 ballVelocity = goal.minus(ballPosition).withZ(random(0, 1200)).scaledToMagnitude(ballSpeed);
			
			GameState gameState = new GameState();
			gameState.withCarState(input.playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(carPosition.toDesired()).withRotation(CarOrientation.fromVector(carDirection).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
			gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballPosition.withZ(Constants.BALL_RADIUS).toDesired()).withVelocity(ballVelocity.toDesired()).withAngularVelocity(new Vector3().toDesired())));
			RLBotDll.setGameState(gameState.buildPacket());
			resetCooldown(input.elapsedSeconds);
		}
	}

	public void resetToKickoff(DataPacket input){
		if(input.ball.position.magnitude() < 2000 || getCooldown(input) < 6) return;
		GameState gameState = new GameState();
		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, (Constants.PITCH_LENGTH + Constants.BALL_RADIUS + 10) * Math.signum(input.ball.position.y), Constants.BALL_RADIUS).toDesired())));
		RLBotDll.setGameState(gameState.buildPacket());
		resetCooldown(input.elapsedSeconds);
	}
	
	private boolean isInsideArena(Vector3 vec){
		if(vec == null || vec.confine().distance(vec) > 50) return false;
		return Pitch.segmentIntersect(new Vector3(0, 0, Constants.CEILING / 2), vec) == null;
	}
	
	private boolean isInsideArena(Vector2 vec){
		if(vec == null) return false;
		return isInsideArena(vec.withZ(Constants.BALL_RADIUS));
	}

	public void wallHit(DataPacket input){
		if(Math.abs(input.ball.position.y) < 1000 && getCooldown(input) < 10) return;
		
		CarData car = input.car;
		
		GameState gameState = new GameState();
		
		Vector3 ballPosition = new Vector3(0, 0, Constants.BALL_RADIUS);
		Vector3 ballVelocity = new Vector3(random(2500, 3000), 0, 0);
		Vector3 ballAngVelocity = new Vector3();
		gameState.withBallState(new BallState().withPhysics(new PhysicsState()
				.withLocation(ballPosition.toDesired())
				.withVelocity(ballVelocity.toDesired())
				.withAngularVelocity(ballAngVelocity.toDesired())));
		
		Vector3 carPosition = new Vector3(random(3000, 4000), random(2000, 3500) * -car.sign, Constants.CAR_HEIGHT);
		Vector3 carVelocity = new Vector3(0, 0, 0);
		Vector3 carAngVelocity = new Vector3();
		CarOrientation carOrientation = CarOrientation.fromVector(new Vector2(Constants.PITCH_WIDTH, 0).minus(carPosition.flatten()));
		float boost = 100;
		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(boost).withPhysics(new PhysicsState()
				.withLocation(carPosition.toDesired())
				.withVelocity(carVelocity.toDesired())
				.withRotation(carOrientation.toDesired())
				.withAngularVelocity(carAngVelocity.toDesired())));
		
		RLBotDll.setGameState(gameState.buildPacket());
		resetCooldown(input.elapsedSeconds);
	}

	public void driveDownWall(DataPacket input){
		CarData car = input.car;
		
		if(Math.abs(car.position.z) > 50 && getCooldown(input) < 15) return;
		
		GameState gameState = new GameState();
		
		Vector3 ballPosition = new Vector3(random(-1000, 1000), random(-1000, 1000), Constants.BALL_RADIUS);
		Vector3 ballVelocity = new Vector3(0, 0, 0);
		Vector3 ballAngVelocity = new Vector3();
		gameState.withBallState(new BallState().withPhysics(new PhysicsState()
				.withLocation(ballPosition.toDesired())
				.withVelocity(ballVelocity.toDesired())
				.withAngularVelocity(ballAngVelocity.toDesired())));
		
		double carVelZ = random(-800, 1700);
		Vector3 carPosition = new Vector3(Constants.PITCH_WIDTH - Constants.CAR_HEIGHT, random(-2000, 2000), 700 - carVelZ / 4);
		Vector3 carVelocity = new Vector3(0, 100, carVelZ);
		Vector3 carAngVelocity = new Vector3();
		CarOrientation carOrientation = new CarOrientation(new Vector3(0, 0, 1), new Vector3(0, -1, 0), new Vector3(0, 0, -1));
		float boost = 100;
		gameState.withCarState(wildfire.playerIndex, new CarState().withBoostAmount(boost).withPhysics(new PhysicsState()
				.withLocation(carPosition.toDesired())
				.withVelocity(carVelocity.toDesired())
				.withRotation(carOrientation.toDesired())
				.withAngularVelocity(carAngVelocity.toDesired())));
		
		RLBotDll.setGameState(gameState.buildPacket());
		resetCooldown(input.elapsedSeconds);
	}

}
