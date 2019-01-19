package wildfire.wildfire;

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
import rlbot.flat.QuickChatSelection;
import wildfire.boost.BoostManager;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.obj.StateSettingManager;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.states.BoostState;
import wildfire.wildfire.states.ClearState;
import wildfire.wildfire.states.FallbackState;
import wildfire.wildfire.states.KickoffState;
import wildfire.wildfire.states.PathState;
import wildfire.wildfire.states.ReturnState;
import wildfire.wildfire.states.ShadowState;
import wildfire.wildfire.states.ShootState;
import wildfire.wildfire.states.WaitState;
import wildfire.wildfire.states.WallHitState;

public class Wildfire implements Bot {

    public final int playerIndex;
    public final int team;
    
    /*Whether this is a test version*/
    private final boolean test;
    public boolean isTestVersion(){
		return test;
	}
    
	public WRenderer renderer;
	public BallPrediction ballPrediction;
	public PredictionSlice impactPoint;
	
	public StateSettingManager stateSetting;
	
	//State info
	public ArrayList<State> states;
	private State fallbackState;
	private State activeState;
	private State lastPrintedState = null;
	
	//Measured in elapsed seconds
	public float lastDodge = 0;
	
	//Measured in milliseconds
	private final long quickChatCooldown = 60000L;
	private long lastQuickChat = 0L;

    public Wildfire(int playerIndex, int team, boolean test){
        this.playerIndex = playerIndex;
        this.team = team;
        this.test = test;
        this.stateSetting = new StateSettingManager(this);
        
        //Initialise all the states
        states = new ArrayList<State>();
        new KickoffState(this);
        new WallHitState(this);
        new WaitState(this);
        new ShootState(this);
        new ClearState(this);
        new ReturnState(this);
        new BoostState(this);
        new PathState(this);
        new ShadowState(this);
        fallbackState = new FallbackState(this);
        
        //Test states
//        new HookState(this);
//        new TestState(this);
//     	  new TestState2(this);
//        fallbackState = new CircleState(this);
//        fallbackState = new StayStillState(this);
//        fallbackState = new PathState(this);
    }

    private ControlsOutput processInput(DataPacket input){
    	//Get a renderer
    	renderer = new WRenderer(this, isTestVersion(), isTestVersion());
    	
//    	//Bezier
//    	for(CarData car : input.cars){
//    		Vector2 trace = Utils.traceToY(car.position.flatten(), input.ball.position.minus(car.position).flatten(), Utils.PITCHLENGTH * Utils.teamSign(car));
//    		double offset = (trace == null ? 2000 : Math.max(0, Math.abs(trace.x) - Utils.GOALHALFWIDTH) / 4);
//	    	BezierCurve bezier = new BezierCurve(car.position.flatten(), input.ball.position.plus(input.ball.position.minus(Utils.enemyGoal(car.team).withZ(0)).scaledToMagnitude(offset)).flatten().confine(), input.ball.position.flatten());
//	    	bezier.render(BotLoopRenderer.forBotLoop(this), car.team == 0 ? Color.BLUE : Color.ORANGE);
//    	}
    	
//    	stateSetting.recovery(input);
    	
    	//Get the ball prediction
    	try{
    	    ballPrediction = RLBotDll.getBallPrediction();
    	}catch(IOException e){
    		ballPrediction = null;
    	}
    	
    	//GG
    	if(input.gameInfo.isMatchEnded()){
    		try{
				if(lastQuickChat != -1) RLBotDll.sendQuickChat(this.playerIndex, false, QuickChatSelection.PostGame_Gg);
				lastQuickChat = -1;
			}catch(Exception e){
				e.printStackTrace();
			}
    	}else if(lastQuickChat == -1){
    		lastQuickChat = 0;
    	}
    	
    	//Impact point
    	try{
    		impactPoint = Utils.getEarliestImpactPoint(input, ballPrediction);
    	}catch(Exception e){
    		e.printStackTrace();
    		impactPoint = new PredictionSlice(input.ball.position, 360);
    	}
    	
    	//Choose whether to continue with the active state
    	if(activeState != null){
    		//Expire the state's action
    		if(activeState.hasAction() && activeState.currentAction.expire(input)){
    			activeState.currentAction = null;
			}
    		
    		//Expire the state
    		if(!activeState.hasAction() && activeState.expire(input)){
    			activeState = null;
    		}
    	}
    	
    	//Get a new state if one isn't active
    	if(activeState == null && !fallbackState.hasAction()){
		    for(State state : states){
		    	 if(state.ready(input)){
		    		activeState = state;
		    		if(lastPrintedState == null || activeState.getName() != lastPrintedState.getName()){
		    			System.out.println(playerIndex + " [" + (10 + System.currentTimeMillis() % 90) + "]: " + activeState.getName() + (activeState.hasAction() ? " (" + activeState.currentAction.getName() + ")" : ""));
		    			lastPrintedState = activeState;
		    		}
		    		break;
		    	}
		    }
		    if(activeState == null && (lastPrintedState == null || fallbackState.getName() != lastPrintedState.getName())){
		    	System.out.println(playerIndex + " [" + (10 + System.currentTimeMillis() % 90) + "]: " + fallbackState.getName() + (fallbackState.hasAction() ? " (" + fallbackState.currentAction.getName() + ")" : ""));
		    	lastPrintedState = fallbackState;
		    }
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

	/*
	 * Intended so that the bot doesn't spam... too much
	 */
	public boolean sendQuickChat(byte... quickChatSelection){
		long currentTime = System.currentTimeMillis();
		if(currentTime > lastQuickChat + quickChatCooldown && lastQuickChat != -1){
			Random random = new Random();
			
			try{
				RLBotDll.sendQuickChat(this.playerIndex, false, quickChatSelection[random.nextInt(quickChatSelection.length)]);
			}catch(Exception e){
				System.err.println("Error when trying to send quick-chat [" + quickChatSelection.toString() + "]");
			}
			
			lastQuickChat = currentTime;
			return true;
		}
		return false;
	}

}
