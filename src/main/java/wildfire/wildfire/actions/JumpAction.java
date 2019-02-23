package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.Utils;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;

public class JumpAction extends Action {
	
	private final double tick = (1D / 60);
	
	// h = (10500 * T * T) / 13 + (8400 * T) / 13 + 900 / 13
	// T = 0.1 * sqrt(13 / 105) * sqrt(h + 60) - 0.4
	
	private double height, jumpTime;

	public JumpAction(State state, float elapsedSeconds, double height){
		super("Jump", state, elapsedSeconds);
		this.height = height;
		this.jumpTime = Math.max(0.1D * Math.sqrt(13D / 105D) * Math.sqrt(height + 60D) - 0.4D, 0);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(timeDifference(input.elapsedSeconds) * 1000 > jumpTime * 1000D + tick){
			Utils.transferAction(this, new RecoveryAction(state, input.elapsedSeconds));
		}
		wildfire.renderer.drawString2d("Height: " + (int)height + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Jump: " + (int)jumpTime + "ms", Color.WHITE, new Point(0, 60), 2, 2);
		return new ControlsOutput().withBoost(false).withThrottle(0).withJump(true);
	}

	@Override
	public boolean expire(DataPacket input){
		float timeDifference = timeDifference(input.elapsedSeconds) * 1000;
		return (input.car.hasWheelContact && timeDifference > jumpTime + 100) || timeDifference > 2000;
	}

}
