package wildfire.wildfire;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.GameTickPacket;
import rlbot.flat.QuickChatSelection;
import wildfire.WildfireJava;
import wildfire.boost.BoostManager;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.obj.Human;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.obj.StateSettingManager;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.states.*;
import wildfire.wildfire.utils.Behaviour;

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
	private Human human;
	
	//State info
	public ArrayList<State> states;
	private State fallbackState;
	private State activeState;
	private State lastPrintedState = null;
	
	//Measured in elapsed seconds
	private float lastDodge = 0;

	//Measured in milliseconds
	private final long quickChatCooldown = 40000L;
	private long lastQuickChat = 0L;
	
	public boolean unlimitedBoost;	

    public Wildfire(int playerIndex, int team, boolean test){
        this.playerIndex = playerIndex;
        this.team = team;
        this.test = test;
        this.stateSetting = new StateSettingManager(this);
        this.unlimitedBoost = false;
        
        //Human thread
        this.human = new Human(this).setEnabled(false);
        this.human.start();
        
        //Initialise all the states
        states = new ArrayList<State>();
        if(!isTestVersion()) new IdleState(this);
        new KickoffState(this);
        new WallHitState(this);
        new PatienceState(this);
        new BoostState(this);
        new WaitState(this, false);
        new MixerState(this);
        new StalkState(this);
        new ShootState(this);
        new ClearState(this);
        new ReturnState(this);
        new PathState(this, false);
        new DemoState(this);
        new ShadowState(this);
        fallbackState = new FallbackState(this);
        
        //Test states
//        new TestState(this);
//        fallbackState = new TestState2(this);
//        fallbackState = new PathState(this, true);
//        fallbackState = new DemoState(this);
//        fallbackState = new IdleState(this);
//        fallbackState = new ReturnState(this);
//        fallbackState = new ShadowState(this);
        
        WildfireJava.bots.add(this);
    }

    private ControlsOutput processInput(DataPacket input){
    	//Get a renderer
    	renderer = new WRenderer(this, !Behaviour.hasTeammate(input) && isTestVersion(), isTestVersion());
    	
//    	stateSetting.path(input, true, true);
//    	stateSetting.shoot(input, false);
    	
    	//Get the ball prediction
    	try{
    	    ballPrediction = RLBotDll.getBallPrediction();
    	}catch(RLBotInterfaceException e){
    		e.printStackTrace();
    		ballPrediction = null;
    	}
    	
    	//GG
    	if(input.gameInfo.isMatchEnded()){
    		try{
				if(lastQuickChat != -1){
					if(Behaviour.isTeammateCloser(input)){
						RLBotDll.sendQuickChat(this.playerIndex, false, (new Random()).nextBoolean() ? QuickChatSelection.PostGame_ThatWasFun : QuickChatSelection.PostGame_WhatAGame);
					}else{
						RLBotDll.sendQuickChat(this.playerIndex, false, QuickChatSelection.PostGame_Gg);
					}
				}
				lastQuickChat = -1;
			}catch(Exception e){
				e.printStackTrace();
			}
    	}else if(lastQuickChat == -1){
    		lastQuickChat = 0;
    	}
    	
    	//Impact point
    	try{
    		impactPoint = Behaviour.getEarliestImpactPoint(input, ballPrediction);
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
    	String printPrefix = "[" + (int)(input.elapsedSeconds) + "] " + playerIndex + ": ";
    	if(activeState == null && !fallbackState.hasAction()){
		    for(State state : states){
		    	 if(state.getClass() != fallbackState.getClass() && state.ready(input)){
		    		activeState = state;
		    		if(lastPrintedState == null || activeState.getName() != lastPrintedState.getName()){
		    			System.out.println(printPrefix + activeState.getName() + (activeState.hasAction() ? " (" + activeState.currentAction.getName() + ")" : ""));
		    			lastPrintedState = activeState;
		    		}
		    		break;
		    	}
		    }
		    if(activeState == null && (lastPrintedState == null || fallbackState.getName() != lastPrintedState.getName())){
		    	System.out.println(printPrefix + fallbackState.getName() + (fallbackState.hasAction() ? " (" + fallbackState.currentAction.getName() + ")" : ""));
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

    /**
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
        if(packet.playersLength() <= playerIndex || packet.ball() == null) return new ControlsOutput().withNone();
        
        try{
        	BoostManager.loadGameTickPacket(packet);
        }catch(Exception e){
        	System.err.println("Those dang contributors broke the boost pads :(");
        }
        
        DataPacket dataPacket = new DataPacket(packet, playerIndex);
        
        ControlsOutput wildfireControls = processInput(dataPacket);
        if(!this.human.isEnabled()){
        	return wildfireControls;
        }else{
        	renderer.drawString2d("Human", Color.BLUE, new Point(0, 200), 8, 8);
        	ControlsOutput humanControls = this.human.getControls();
        	return humanControls;
        }
    }

    public void retire(){
        System.out.println("Retiring Wildfire [" + playerIndex + "]");
        WildfireJava.bots.remove(this);
    }

    @Override
    public int getIndex(){
        return this.playerIndex;
    }

	/*
	 * Intended so that the bot doesn't spam... too much
	 */
	public boolean sendQuickChat(boolean teamOnly, byte... quickChatSelection){
		long currentTime = System.currentTimeMillis();
		if(currentTime > lastQuickChat + quickChatCooldown && lastQuickChat != -1){
			Random random = new Random();
			
			try{
				RLBotDll.sendQuickChat(this.playerIndex, teamOnly, quickChatSelection[random.nextInt(quickChatSelection.length)]);
			}catch(Exception e){
				System.err.println("Error when trying to send quick-chat [" + quickChatSelection.toString() + "]");
			}
			
			lastQuickChat = currentTime;
			return true;
		}
		return false;
	}
	
	public boolean sendQuickChat(byte... quickChatSelection){
		return this.sendQuickChat(false, quickChatSelection);
	}
	
	public float lastDodgeTime(float elapsedSeconds){
		if(elapsedSeconds < this.lastDodge) resetDodgeTime(elapsedSeconds);
		return elapsedSeconds - this.lastDodge;
	}

	public void resetDodgeTime(float elapsedSeconds){
		this.lastDodge = elapsedSeconds;
	}

}
