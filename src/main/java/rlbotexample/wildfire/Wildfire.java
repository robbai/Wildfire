package rlbotexample.wildfire;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.flat.GameTickPacket;
import rlbotexample.boost.BoostManager;
import rlbotexample.input.DataPacket;
import rlbotexample.output.ControlsOutput;
import rlbotexample.vector.Vector2;
import rlbotexample.vector.Vector3;
import rlbotexample.wildfire.states.BoostState;
import rlbotexample.wildfire.states.FallbackState;
import rlbotexample.wildfire.states.InterceptState;
import rlbotexample.wildfire.states.KickoffState;
import rlbotexample.wildfire.states.ShadowState;
import rlbotexample.wildfire.states.ShootState;
import rlbotexample.wildfire.states.WaitState;

public class Wildfire implements Bot {

    public final int playerIndex;
    public final int team;
    
    /*Whether this is a test version*/public final boolean test;
    
    public final double goalSafeZone = 770D;
    
	public WRenderer renderer;
	public BallPrediction ballPrediction;
	
	public ArrayList<State> states;
	private State fallbackState;
	private State activeState;
	
	public long lastDodge = 0L;
	public Vector3 impactPoint;
	public Vector2 target;
	
//	private Vector3 carSpawn;
//	private CarOrientation carAngularSpawn;
	
////	private double maxX = 0;
//	private float throttle = 1F;
//	private long timeStarted = Long.MIN_VALUE / 2;
//	private long startupTime = 2000L;
//	private float increment = 0.025F;

    public Wildfire(int playerIndex, int team, boolean test){
        this.playerIndex = playerIndex;
        this.team = team;
        this.test = test;
        
        //Initialise all the states
        states = new ArrayList<State>();
        new KickoffState(this);
        new ShootState(this);
        new WaitState(this);
        new InterceptState(this);
        new BoostState(this);
        new ShadowState(this);
//      new TestState(this);
//      new TestState2(this);        
        fallbackState = new FallbackState(this);
    }

    private ControlsOutput processInput(DataPacket input){
    	//Get a renderer
    	renderer = new WRenderer(this, false, true);
    	
    	//Path drawing
//    	for(CarData c : input.cars){
//    		if(c == null) continue;
//    		Vector3 start = c.position;
//    		double scale = 0.05;
//    		Vector2 rotation = c.velocity.scaled(scale).flatten();
//    		for(int i = 0; i < 80; i++){
//    			Vector2 target = input.ball.position.flatten().plus(input.ball.position.flatten().minus(Utils.enemyGoal(c.team)).scaledToMagnitude(0.5 * input.ball.position.distanceFlat(start))).confine();
//    			
//    			double s = (float)-Utils.aimFromPoint(start.flatten(), rotation, target) * 2F;
//    			rotation = rotation.rotate(-Utils.clamp(s) / (0.4193 / scale));
//    			Vector3 end = start.plus(rotation.withZ(0)).confine();
//    			
//    			renderer.drawLine3d(i % 2 == 0 ? (c.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, start.toFramework(), end.toFramework());
//    			if(end.distanceFlat(input.ball.position) < Utils.BALLRADIUS) break;
//    			start = end;
//    		}
//    	}
    	
//    	//Kickoff Testing
//    	GameState gameState = new GameState();
//    	if(!input.ball.velocity.flatten().isZero() && carSpawn != null && carAngularSpawn != null){
//    		RLBotDll.sendQuickChat(playerIndex, false, (byte)new Random().nextInt(20));
//    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(0, 0, Utils.BALLRADIUS).toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired())));
//    		gameState.withCarState(this.playerIndex, new CarState().withBoostAmount(34F).withPhysics(new PhysicsState().withLocation(carSpawn.toDesired()).withVelocity(new Vector3().toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(carAngularSpawn.toDesired())));
//    	}else if(input.car.velocity.isZero()){
//    		carSpawn = input.car.position;
//    		carAngularSpawn = input.car.orientation;
//    	}
//    	RLBotDll.setGameState(gameState.buildPacket());
    	
//    	//Recovery testing
//    	GameState gameState = new GameState();
//    	if(input.car.hasWheelContact && input.car.position.z < 200){
//    		gameState.withCarState(this.playerIndex, new CarState().withPhysics(new PhysicsState().withLocation(new DesiredVector3(0F, 0F, 1000F)).withRotation(new DesiredRotation(rot(), rot(), rot())).withVelocity(new DesiredVector3(rot() * 100, rot() * 100, 200F)).withAngularVelocity(new DesiredVector3(rot() * 10, rot() * 10, rot() * 10))));
//    	}
//    	RLBotDll.setGameState(gameState.buildPacket());
    	
//    	//Freeze ball in air
//    	GameState gameState = new GameState();
//    	if(input.ball.position.flatten().isZero()) gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new DesiredVector3(0F, 0F, 1200F)).withVelocity(new DesiredVector3(0F, 0F, 0F))));
////    	gameState.withCarState(1, new CarState().withPhysics(new PhysicsState().withLocation(new DesiredVector3((float)-input.car.position.x * 2, (float)input.car.position.y * 2, (float)input.car.position.z + 200)).withVelocity(new DesiredVector3(0F, 0F, 0F))));
////    	gameState.withCarState(1, new CarState().withPhysics(new PhysicsState().withRotation(new DesiredRotation(0F, (float)Utils.aim(input.cars[1], input.car.position.flatten()), 0F))));
//    	RLBotDll.setGameState(gameState.buildPacket());
    	
//    	GameState gameState = new GameState();
//    	if(timeStarted > System.currentTimeMillis()) timeStarted = System.currentTimeMillis();
//    	long timeDifference = (System.currentTimeMillis() - timeStarted);
//    	double speed = input.car.velocity.flatten().magnitude();
//    	gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new DesiredVector3(0F, 5000F, 1500F)).withVelocity(new DesiredVector3(0F, 0F, 0F))));
//    	if(timeDifference < startupTime / 2){
////    		maxX = 0;
//    		gameState.withCarState(this.playerIndex, new CarState().withPhysics(new PhysicsState().withLocation(new DesiredVector3(0F, -5700F, 60F)).withRotation(new DesiredRotation(0F, (float)Math.PI / 2F, 0F)).withVelocity(new DesiredVector3(0F, 0F, 0F)).withAngularVelocity(new DesiredVector3(0F, 0F, 0F))));
//    	}else if(speed >= 1409 || input.car.position.y > 5200){
//    		if(speed / (double)((timeDifference - startupTime) / 1000D) >= 1) System.out.println((throttle >= 1 ? "(" + throttle + "F) " : "(" + (throttle - 1) + "F, Boost) ")+ (speed / ((double)(timeDifference - startupTime) / 1000D)) + "uu/s^2");
//    		timeStarted = System.currentTimeMillis();
//    		throttle = (throttle + increment) % (2F + increment);
//    		throttle = Math.round(throttle * 1000F) / 1000F;
//    		throttle = Math.max(Math.min(1F, throttle), increment);
//    	}
//		RLBotDll.setGameState(gameState.buildPacket());
//		renderer.drawString2d("Throttle = " + throttle + "F", Color.WHITE, new Point(0, 0), 2, 2);
//    	renderer.drawString2d("Speed = " + (int)speed + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
//    	renderer.drawString2d("Time = " + ((double)(timeDifference - startupTime) / 1000D) + "s", Color.WHITE, new Point(0, 40), 2, 2);
////    	maxX = Math.max(maxX, input.car.position.x);
////    	renderer.drawString2d("Radius = " + (maxX / 2D) + "uu", Color.WHITE, new Point(0, 20), 2, 2);
//		return new ControlsOutput().withThrottle(timeDifference > startupTime ? (throttle > 1 ? throttle - 1 : throttle) : 0F).withBoost(throttle > 1).withSteer(0F);   
    	
//    	//Yeet the ball
//    	GameState gameState = new GameState();
//    	if(((input.ball.position.z < Utils.BALLRADIUS + 5 && Math.abs(input.ball.velocity.z) < 70) || input.ball.velocity.isZero()) && input.car.hasWheelContact){
//    		Random r = new Random();
//    		float mag = 20000F;
//    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withVelocity(new DesiredVector3().withZ(mag * 0.1F).withX(-0.5F * mag + mag * r.nextFloat()).withY(-0.5F * mag + mag * r.nextFloat()))));
//    	}
//    	RLBotDll.setGameState(gameState.buildPacket());
    	
//    	long timeDifference = (System.currentTimeMillis() % 5000);
//    	if(timeDifference < 50){
//    		Random r = new Random();
//    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new DesiredVector3(-3000F + 6000F * r.nextFloat(), -4000F + 8000F * r.nextFloat(), 500F))));
//        	System.out.println(System.currentTimeMillis());
//    	}else{
//    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withVelocity(new DesiredVector3(0F, 0F, 0F))));
//    	}
//    	RLBotDll.setGameState(gameState.buildPacket());
    	
    	//Get the ball prediction
    	try{
    	    ballPrediction = RLBotDll.getBallPrediction();
    	}catch(IOException e){
    		ballPrediction = null;
    	}
    	
//    	//Draw the turning radius
//    	double turningRadius = Utils.getTurnRadius(input.car.velocity.flatten().magnitude());
//    	Utils.drawCircle(renderer, Color.PINK, input.car.position.plus(input.car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten(), turningRadius);
//    	Utils.drawCircle(renderer, Color.PINK, input.car.position.plus(input.car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten(), turningRadius);
    	
    	//Impact point
    	try{
    		impactPoint = Utils.getEarliestImpactPoint(input, ballPrediction);
    	}catch(Exception e){
    		e.printStackTrace();
    		impactPoint = input.ball.position;
    	}
    	
    	//Target
    	target = Utils.getTarget(input.car, input.ball);
    	Utils.drawCrosshair(renderer, input.car, target.withZ(Utils.BALLRADIUS), Color.WHITE, 125);
//    	renderer.drawString2d(target.toString(), Color.WHITE, new Point(200, 0), 2, 2);
    	
    	//Choose whether to continue with the active state
    	boolean expired = false;
    	if(activeState != null){
    		//Expire the state's action
    		if(activeState.hasAction() && activeState.currentAction.expire(input)){
    			activeState.currentAction = null;
			}
    		
    		//Expire the state
    		if(!activeState.hasAction() && activeState.expire(input)){
    			activeState = null;
    			expired = true;
    		}
    	}
    	
    	//Get a new state if one isn't active
    	if(activeState == null && !fallbackState.hasAction()){
		    for(State state : states){
		    	 if(state.ready(input)){
		    		activeState = state;
		    		System.out.println(playerIndex + " [" + (10 + System.currentTimeMillis() % 90) + "]: " + activeState.getName() + (activeState.hasAction() ? " (" + activeState.currentAction.getName() + ")" : ""));
		    		break;
		    	}
		    }
		    if(activeState == null && expired) System.out.println(playerIndex + " [" + (10 + System.currentTimeMillis() % 90) + "]: " + fallbackState.getName() + (fallbackState.hasAction() ? " (" + fallbackState.currentAction.getName() + ")" : ""));
    	}
    	
    	//Return the output from the state
    	if(activeState == null || fallbackState.hasAction()) return fallback(input);    	
    	try{
    		renderer.drawString2d(activeState.getName(), Color.GREEN, new Point(0, 0), 2, 2);
    		
    		//Return the action's output since it would override the state
    		if(activeState.hasAction()){
    			ControlsOutput actionOutput = activeState.currentAction.getOutput(input);
	    		renderer.drawString2d(activeState.currentAction.getName(), Color.GRAY, new Point(0, 20), 2, 2);
	    		return actionOutput;
    		}
    		
    		//Return the state's regular output
    		return activeState.getOutput(input);
    	}catch(Exception e){
    		//Errors occurred when trying to run the current state!
    		e.printStackTrace();
    		if(activeState.hasAction()) activeState.currentAction = null;
    		activeState = null;
    		return fallback(input);
    	}
    }

    /*
     * Fallback state saves us all!
     */
	private ControlsOutput fallback(DataPacket input){
		renderer.drawString2d(fallbackState.getName(), Color.RED, new Point(0, 0), 2, 2);
		if(fallbackState.hasAction() && fallbackState.currentAction.expire(input)) fallbackState.currentAction = null;
		if(fallbackState.hasAction()){
			ControlsOutput actionOutput = fallbackState.currentAction.getOutput(input);
    		renderer.drawString2d(fallbackState.currentAction.getName(), Color.DARK_GRAY, new Point(0, 20), 2, 2);
    		return actionOutput;
		}
		return fallbackState.getOutput(input);
	}

	@Override
    public ControllerState processInput(GameTickPacket packet){
        if(packet.playersLength() <= playerIndex || packet.ball() == null) return new ControlsOutput();
        BoostManager.loadGameTickPacket(packet);
        DataPacket dataPacket = new DataPacket(packet, playerIndex);
        return processInput(dataPacket);
    }

    public void retire(){
        System.out.println("Retiring Wildfire [" + playerIndex + "]");
    }
    
    @Override
    public int getIndex(){
        return this.playerIndex;
    }
    
    @SuppressWarnings("unused")
	private float rot(){
    	Random r = new Random();
    	return (float)(r.nextFloat() * Math.PI * 2 - Math.PI);
    }
    
}
