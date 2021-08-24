package wildfire.wildfire.actions;

import java.awt.Color;
import java.awt.Point;

import wildfire.output.ControlsOutput;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Utils;

public class JumpAction extends Action {

	// h = (10500 * T * T) / 13 + (8400 * T) / 13 + 900 / 13
	// T = 0.1 * sqrt(13 / 105) * sqrt(h + 60) - 0.4

	private final double tick = (1D / 60);
//	private final double maxSingleJumpHeight = 230.76923076923077D;

	private double height, jumpTime;

	public JumpAction(State state, float elapsedSeconds, double height){
		super("Jump", state, elapsedSeconds);
		this.height = height;
		this.jumpTime = Math.max(0.1D * Math.sqrt(13D / 105D) * Math.sqrt(height + 60D) - 0.4D, 0);
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		float timeDifference = timeDifference(input.elapsedSeconds);

		if(timeDifference > jumpTime + tick && !input.car.hasWheelContact){
			Utils.transferAction(this, new RecoveryAction(state, input.elapsedSeconds));
		}

		wildfire.renderer.drawString2d("Height: " + (int)height + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Jump: " + (int)jumpTime + "ms", Color.WHITE, new Point(0, 60), 2, 2);
		ControlsOutput controller = new ControlsOutput().withNone()
				.withJump(timeDifference < jumpTime + tick && timeDifference > tick);
//		if(!controller.holdJump()) controller.withJump(input.car.velocity.z <= 0 && !input.car.doubleJumped);
		return controller;
	}

	@Override
	public boolean expire(InfoPacket input){
		float timeDifference = timeDifference(input.elapsedSeconds);
		return (input.car.hasWheelContact && timeDifference > jumpTime + tick);
	}

}
