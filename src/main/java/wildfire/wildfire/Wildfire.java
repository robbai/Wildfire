package wildfire.wildfire;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.flat.GameTickPacket;
import rlbot.flat.QuickChatSelection;
import wildfire.Main;
import wildfire.boost.BoostManager;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.input.Info;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Human;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.obj.StateSettingManager;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.states.*;
import wildfire.wildfire.utils.Behaviour;

public class Wildfire implements Bot {

    
	public final int playerIndex;
    public final int team;
    
    /**
     * Flag for whether the agent contains the word "test" in its in-game name
     */
    private final boolean test;
    public boolean isTestVersion(){
		return test;
	}
    
    /**
     * Flag for running the "garbage collector" during replays
     */
    private static final boolean runGc = true;
    private static boolean ranGc;	
    
    private static final boolean printMs = false;
    
	public StateSettingManager stateSetting;
	
	private Human human;
	
	// State info.
	public ArrayList<State> states;
	private State fallbackState;
	private State activeState;
	private String lastPrintedInfo = "";

	// Measured in milliseconds.
	private final long quickChatCooldown = 40000L;
	private long lastQuickChat = 0L;
	
	public boolean unlimitedBoost;
	
	public Info info;
	public WRenderer renderer;
	public BallPrediction ballPrediction;

    public Wildfire(int playerIndex, int team, boolean test){
        this.playerIndex = playerIndex;
        this.team = team;
        this.test = test;
        this.stateSetting = new StateSettingManager(this);
        this.unlimitedBoost = false;
        this.info = new Info(this);
        
        // Human thread.
//        this.human = new Human(this).setEnabled(false);
//        this.human.start();
        
        /*
         * Initialise all the states.
         */
        states = new ArrayList<State>();
        new IdleState(this);
        new KickoffState(this);
        new BoostTestState(this);
        new WallHitState(this);
        new PatientShootState(this);
        new PathState(this, false);
//        new PatienceState(this);
        new BoostState(this);
        new WaitState(this, false);
        new StalkState(this);
        new MixerState(this);
//        new ShootState(this);
        new ClearState(this);
        new ReturnState(this);
//        new PathState(this, false);
//        new DemoState(this);
        new ShadowState(this);
//        new ShadowTestState(this);
        fallbackState = new FallbackState(this);
        
        /*
         * Test states
         */
//        fallbackState = new TestState(this);
//        fallbackState = new TestState2(this);
//        fallbackState = new PathState(this, true);
//        fallbackState = new DemoState(this);
//        fallbackState = new IdleState(this);
//        fallbackState = new ReturnState(this);
//        fallbackState = new ShadowState(this);
        
        Main.bots.add(this);
    }

    private ControlsOutput processInput(InfoPacket input){
//    	stateSetting.path(input, true);
    	
    	// GG.
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
    	
    	// Choose whether to continue with the active state.
    	if(activeState != null){
    		// Expire the state's action.
    		if(activeState.hasAction() && activeState.currentAction.expire(input)){
    			activeState.currentAction = null;
			}
    		
    		// Expire the state's mechanic.
    		if(activeState.runningMechanic() && activeState.currentMechanic.expire(input)){
    			activeState.currentMechanic = null;
			}
    		
    		// Expire the state.
    		if(!activeState.hasAction() && activeState.expire(input)){
    			activeState.currentMechanic = null;
    			activeState = null;
    		}
    	}
    	
    	// Get a new state if one isn't active.
    	if(activeState == null && !fallbackState.hasAction()){
		    for(State state : states){
		    	 if(!state.getClass().equals(fallbackState.getClass()) && state.ready(input)){
		    		activeState = state;
		    		break;
		    	}
		    }
    	}
    	
    	// Print info.
    	State effectiveState = (activeState != null ? activeState : this.fallbackState);
    	String printPrefix = "[" + ((int)input.elapsedSeconds % 1000) + "] " + playerIndex + ": ";
    	String printInfo = (effectiveState.getName() + (effectiveState.hasAction() ? " (" + effectiveState.currentAction.getName() + ")" : (effectiveState.runningMechanic() ? " (" + effectiveState.currentMechanic.getName() + ")" : "")));
		if(lastPrintedInfo.isEmpty() || !printInfo.equals(lastPrintedInfo)){
			System.out.println(printPrefix + printInfo);
			lastPrintedInfo = printInfo;
		}
    	
    	// Return the output from the state.
    	if(activeState == null || fallbackState.hasAction()) return fallback(input);    	
    	try{
    		info.renderer.drawString2d(activeState.getName(), Color.GREEN, new Point(0, 0), 2, 2);
    		
    		// Return the mechanics's output since it would override the state
    		if(activeState.runningMechanic() && !activeState.hasAction()){
    			ControlsOutput mechanicOutput = activeState.currentMechanic.getOutput(input);
    			if(mechanicOutput != null){
    				info.renderer.drawString2d(activeState.currentMechanic.getName(), Color.GRAY, new Point(0, 20), 2, 2);
		    		return mechanicOutput;
    			}
    		}
    		
    		// Return the action's output since it would override the state
    		if(activeState.hasAction()){
    			ControlsOutput actionOutput = activeState.currentAction.getOutput(input);
    			info.renderer.drawString2d(activeState.currentAction.getName(), Color.GRAY, new Point(0, 20), 2, 2);
	    		return actionOutput;
    		}
    		
    		// Return the state's regular output.
    		return activeState.getOutput(input);
    	}catch(Exception e){
    		// Errors occurred when trying to run the current state!.
    		e.printStackTrace();
    		
    		activeState.currentAction = null;
    		activeState.currentMechanic = null;
    		activeState = null;
    		
    		return fallback(input);
    	}
    }

    /**
     * Fallback state saves us all!
     */
	private ControlsOutput fallback(InfoPacket input){
		info.renderer.drawString2d(fallbackState.getName(), Color.RED, new Point(0, 0), 2, 2);
		if(fallbackState.hasAction() && fallbackState.currentAction.expire(input)) fallbackState.currentAction = null;
		if(fallbackState.hasAction()){
			ControlsOutput actionOutput = fallbackState.currentAction.getOutput(input);
			info.renderer.drawString2d(fallbackState.currentAction.getName(), Color.DARK_GRAY, new Point(0, 20), 2, 2);
    		return actionOutput;
		}
		return fallbackState.getOutput(input);
	}

	@SuppressWarnings ("static-access")
	@Override
    public ControllerState processInput(GameTickPacket packet){
        if(packet.playersLength() <= playerIndex || packet.ball() == null) return new ControlsOutput().withNone();
        
        try{
        	BoostManager.loadGameTickPacket(packet);
        }catch(Exception e){
        	System.err.println("Those dang contributors broke the boost pads :(");
        }
        
        InfoPacket infoPacket = new InfoPacket(packet, playerIndex);
        this.info.update(infoPacket);
        infoPacket.info = this.info;
        
        this.renderer = this.info.renderer;
        this.ballPrediction = this.info.ballPrediction;
        
        ControlsOutput wildfireControls;
        if(this.printMs){
        	long time = System.nanoTime();
        	wildfireControls = processInput(infoPacket);
        	System.out.println((System.nanoTime() - time) / Math.pow(10, 6) + "ms");
        }else{
        	wildfireControls = processInput(infoPacket);
        }
        
        if(this.runGc){
	        if(!packet.gameInfo().isRoundActive()){
	        	if(!this.ranGc){
	        		System.gc();
	        		this.ranGc = true;
	        	}
	        }else{
	        	this.ranGc = false;
	        }
        }
        
        if(this.human == null || !this.human.isEnabled()){
        	return wildfireControls;
        }else{
        	info.renderer.drawString2d("Human", Color.BLUE, new Point(0, 200), 8, 8);
        	ControlsOutput humanControls = this.human.getControls();
        	return humanControls;
        }
    }

    public void retire(){
        System.out.println("Retiring Wildfire [" + playerIndex + "]");
        Main.bots.remove(this);
    }

    @Override
    public int getIndex(){
        return this.playerIndex;
    }

	/**
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

}
