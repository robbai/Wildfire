package wildfire.wildfire.input;

import rlbot.flat.GameTickPacket;
import wildfire.input.DataPacket;

public class InfoPacket extends DataPacket {

	public Info info;

	public InfoPacket(GameTickPacket request, int playerIndex, Info info){
		super(request, playerIndex);
		this.info = info;
	}

	public InfoPacket(GameTickPacket request, int playerIndex){
		this(request, playerIndex, null);
	}

}
