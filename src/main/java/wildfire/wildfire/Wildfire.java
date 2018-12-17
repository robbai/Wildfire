package wildfire.wildfire;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.flat.GameTickPacket;
import wildfire.boost.BoostManager;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.states.BoostState;
import wildfire.wildfire.states.FallbackState;
import wildfire.wildfire.states.InterceptState;
import wildfire.wildfire.states.KickoffState;
import wildfire.wildfire.states.ShadowState;
import wildfire.wildfire.states.ShootState;
import wildfire.wildfire.states.WaitState;

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
//        new TestState(this);
//      	new TestState2(this);        
        fallbackState = new FallbackState(this);
    }

    private ControlsOutput processInput(DataPacket input){
    	//Get a renderer
    	renderer = new WRenderer(this, true, true);
    	
//    	if((!Utils.isBallAirborne(input.ball) || input.car.position.y > 0) && input.car.hasWheelContact){
//    		GameState gameState = new GameState();
//    		gameState.withCarState(playerIndex, new CarState().withBoostAmount(100F).withPhysics(new PhysicsState().withLocation(new Vector3(Utils.random(-3500, 3500), Utils.random(-1000, -4000), 10).toDesired()).withVelocity(new Vector3(Utils.random(-500, 500), Utils.random(-500, 500), 0).toDesired()).withAngularVelocity(new Vector3().toDesired()).withRotation(CarOrientation.convert(0, Utils.randomRotation(), 0).toDesired())));
//    		double xVel = Utils.random(-3000, 3000);
//    		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(new Vector3(-xVel, 0, 100).toDesired()).withVelocity(new Vector3(xVel, 0, Utils.random(1200, 2000)).toDesired())));
//    		RLBotDll.setGameState(gameState.buildPacket());
//    	}
    	
    	//Get the ball prediction
    	try{
    	    ballPrediction = RLBotDll.getBallPrediction();
    	}catch(IOException e){
    		ballPrediction = null;
    	}
    	
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
    
}
