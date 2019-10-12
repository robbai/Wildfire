package wildfire.input.ball;

import rlbot.flat.Touch;
import wildfire.vector.Vector3;

public class BallTouch {

	public final Vector3 position;
	public final Vector3 normal;
	public final String playerName;
	public final float gameSeconds;
	public final int playerIndex;
	public final int team;

	public BallTouch(final Touch touch){
		this.position = new Vector3(touch.location());
		this.normal = new Vector3(touch.normal());
		this.playerName = touch.playerName();
		this.gameSeconds = touch.gameSeconds();
		this.playerIndex = touch.playerIndex();
		this.team = touch.team();
	}

}