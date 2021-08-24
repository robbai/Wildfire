package wildfire.wildfire.grabby;

import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.flat.GameTickPacket;
import wildfire.boost.BoostManager;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.wildfire.grabby.grabber.DodgeGrabber;

public class Grabby implements Bot {

	private final int playerIndex;

	private Grabber grabber;

	public Grabby(int playerIndex){
		this.playerIndex = playerIndex;

		this.grabber = new DodgeGrabber(this);
	}

	private ControlsOutput getOutput(DataPacket input){
		return this.grabber.processInput(input);
	}

	@Override
	public int getIndex(){
		return this.playerIndex;
	}

	@Override
	public ControllerState processInput(GameTickPacket packet){
		if(packet.playersLength() <= playerIndex || packet.ball() == null || !packet.gameInfo().isRoundActive())
			return new ControlsOutput();
		BoostManager.loadGameTickPacket(packet);
		return this.getOutput(new DataPacket(packet, playerIndex));
	}

	public void retire(){
		System.out.println("Retiring Grabby (index=" + playerIndex + ")");
	}

}
