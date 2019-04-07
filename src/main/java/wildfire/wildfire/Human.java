package wildfire.wildfire;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import wildfire.output.ControlsOutput;

public class Human extends Thread {
	
	private boolean enabled;

	private Wildfire wildfire;
	private ControlsOutput controls;
	
	private ControllerManager controllers;

	public Human(Wildfire wildfire){
		this.enabled = false;
		
		//Handle RLBot
		this.wildfire = wildfire;
		this.controls = new ControlsOutput().withNone();
		
		//Handle the controller
		this.controllers = new ControllerManager();
		controllers.initSDLGamepad();
	}
	
	public void run(){
		while(true){
			ControllerState currState = controllers.getState(0);
			if(!currState.isConnected) continue;
			
			//Use the controls provided
			controls.withJump(currState.a);
			controls.withBoost(currState.b);
			controls.withSlide(currState.x);
			controls.withThrottle(currState.rightTrigger - currState.leftTrigger);
			float pitch = (float)-Math.sin(Math.toRadians(currState.leftStickAngle)) * currState.leftStickMagnitude;
			float yaw = (float)Math.cos(Math.toRadians(currState.leftStickAngle)) * currState.leftStickMagnitude;
			controls.withPitch(pitch);
			controls.withYaw(yaw);
			controls.withSteer(yaw);
			controls.withRoll(currState.x ? yaw : 0);
			
			if(currState.leftStickJustClicked && this.wildfire.isTestVersion()){
				this.setEnabled(!this.isEnabled());
			}
		}
	}
	
	public ControlsOutput getControls(){
		return controls;
	}
	
	public boolean isEnabled(){
		return enabled;
	}

	public Human setEnabled(boolean enabled){
		this.enabled = enabled;
		return this;
	}

}
