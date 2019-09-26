package wildfire.wildfire.obj;

import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import wildfire.Main;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Wildfire;

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
	}
	
	public void run(){
		controllers.initSDLGamepad();
		ControllerIndex currController = controllers.getControllerIndex(0);
		
		while(!this.isInterrupted()){
			controllers.update();
			
			try{
				/*
				 * Use the controls provided
				 */
				
				//Toggle the human
				if(currController.isButtonJustPressed(ControllerButton.LEFTSTICK)){
					this.setEnabled(!this.isEnabled());
				}
				
				if(!this.isEnabled()) continue;
				
				controls.withJump(currController.isButtonPressed(ControllerButton.A));
				controls.withBoost(currController.isButtonPressed(ControllerButton.B));
				controls.withSlide(currController.isButtonPressed(ControllerButton.X));
				controls.withThrottle(currController.getAxisState(ControllerAxis.TRIGGERRIGHT) - currController.getAxisState(ControllerAxis.TRIGGERLEFT));
				
				float pitch = -currController.getAxisState(ControllerAxis.LEFTY);
				float yaw = currController.getAxisState(ControllerAxis.LEFTX);
				
				controls.withPitch(pitch);
				controls.withYaw(yaw);
				controls.withSteer(yaw);
				
				//Air roll left is bound to R1
				if(currController.isButtonPressed(ControllerButton.RIGHTBUMPER)){
					controls.withRoll(-1F);
				}else{
					controls.withRoll(currController.isButtonPressed(ControllerButton.X) ? yaw : 0);
				}
			}catch(ControllerUnpluggedException e){  
				continue;
			}
		}
		controllers.quitSDLGamepad();
	}
	
	public ControlsOutput getControls(){
		return controls;
	}
	
	public boolean isEnabled(){
		return enabled;
	}

	public Human setEnabled(boolean enabled){
		this.enabled = (enabled && Main.getArguments().contains("allow-human") && this.wildfire.isTestVersion());
		return this;
	}

}
