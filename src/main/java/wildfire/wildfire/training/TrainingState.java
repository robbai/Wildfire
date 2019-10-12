package wildfire.wildfire.training;

import rlbot.flat.BallInfo;
import rlbot.flat.GameTickPacket;
import rlbot.flat.PlayerInfo;
import rlbot.flat.Rotator;
import rlbot.flat.Vector3;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.DesiredRotation;
import rlbot.gamestate.DesiredVector3;
import rlbot.gamestate.GameState;
import rlbot.gamestate.GameStatePacket;
import rlbot.gamestate.PhysicsState;

public class TrainingState {

	/*
	 * Ball.
	 */
	public final Vector3 ballLocation, ballVelocity, ballAngularVelocity;
	public final Rotator ballRotation;

	/*
	 * Cars.
	 */
	public final Vector3 playerLocation, playerVelocity, playerAngularVelocity, opponentLocation, opponentVelocity, opponentAngularVelocity;
	public final Rotator playerRotation, opponentRotation;
	public final boolean playerJumped, playerDoubleJumped, opponentJumped, opponentDoubleJumped;
	public final float playerBoost, opponentBoost;

	//	/*
	//	 * Boost pads.
	//	 */
	//	public final int boostPadCount;
	//	public final boolean[] boostPadsActive;
	//	public final float[] boostPadsTimer;

	public TrainingState(GameTickPacket packet){
		/*
		 * Ball.
		 */
		BallInfo ball = packet.ball();
		this.ballLocation = ball.physics().location();
		this.ballVelocity = ball.physics().velocity();
		this.ballAngularVelocity = ball.physics().angularVelocity();
		this.ballRotation = ball.physics().rotation();

		/*
		 * Player.
		 */
		PlayerInfo player = packet.players(0);
		this.playerLocation = player.physics().location();
		this.playerVelocity = player.physics().velocity();
		this.playerAngularVelocity = player.physics().angularVelocity();
		this.playerRotation = player.physics().rotation();
		this.playerJumped = player.jumped();
		this.playerDoubleJumped = player.doubleJumped();
		this.playerBoost = player.boost();

		/*
		 * Opponent.
		 */
		PlayerInfo opponent = packet.players(1);
		this.opponentLocation = opponent.physics().location();
		this.opponentVelocity = opponent.physics().velocity();
		this.opponentAngularVelocity = opponent.physics().angularVelocity();
		this.opponentRotation = opponent.physics().rotation();
		this.opponentJumped = opponent.jumped();
		this.opponentDoubleJumped = opponent.doubleJumped();
		this.opponentBoost = opponent.boost();

		//		/*
		//		 * Boost pads.
		//		 */
		//		this.boostPadCount = packet.boostPadStatesLength();
		//		this.boostPadsActive = new boolean[this.boostPadCount];
		//		this.boostPadsTimer = new float[this.boostPadCount];
		//		for(int i = 0; i < this.boostPadCount; i++){
		//			this.boostPadsActive[i] = packet.boostPadStates(i).isActive();
		//			this.boostPadsTimer[i] = packet.boostPadStates(i).timer();
		//		}
	}

	public GameState toGameState(){
		GameState gameState = new GameState();

		/*
		 * Ball.
		 */
		PhysicsState ballPhysics = new PhysicsState();
		ballPhysics.withLocation(new DesiredVector3(this.ballLocation));
		ballPhysics.withVelocity(new DesiredVector3(this.ballVelocity));
		ballPhysics.withAngularVelocity(new DesiredVector3(this.ballAngularVelocity));
		ballPhysics.withRotation(new DesiredRotation(this.ballRotation));
		gameState.withBallState(new BallState(ballPhysics));

		/*
		 * Player.
		 */
		PhysicsState playerPhysics = new PhysicsState();
		playerPhysics.withLocation(new DesiredVector3(this.playerLocation));
		playerPhysics.withVelocity(new DesiredVector3(this.playerVelocity));
		playerPhysics.withAngularVelocity(new DesiredVector3(this.playerAngularVelocity));
		playerPhysics.withRotation(new DesiredRotation(this.playerRotation));
		CarState playerState = new CarState().withPhysics(playerPhysics);
		playerState.withJumped(this.playerJumped);
		playerState.withDoubleJumped(this.playerDoubleJumped);
		playerState.withBoostAmount(this.playerBoost);
		gameState.withCarState(0, playerState);

		/*
		 * Player.
		 */
		PhysicsState opponentPhysics = new PhysicsState();
		opponentPhysics.withLocation(new DesiredVector3(this.opponentLocation));
		opponentPhysics.withVelocity(new DesiredVector3(this.opponentVelocity));
		opponentPhysics.withAngularVelocity(new DesiredVector3(this.opponentAngularVelocity));
		opponentPhysics.withRotation(new DesiredRotation(this.opponentRotation));
		CarState opponentState = new CarState().withPhysics(opponentPhysics);
		opponentState.withJumped(this.opponentJumped);
		opponentState.withDoubleJumped(this.opponentDoubleJumped);
		opponentState.withBoostAmount(this.opponentBoost);
		gameState.withCarState(1, opponentState);

		return gameState;
	}
	
	public GameStatePacket toGameStatePacket(){
		return this.toGameState().buildPacket();
	}

}
