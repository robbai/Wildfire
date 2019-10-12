package wildfire.input.ball;

import rlbot.flat.BallInfo;
import wildfire.input.shapes.SphereShape;
import wildfire.vector.Vector3;
import wildfire.wildfire.utils.Constants;

public class BallData extends SphereShape {

	public final Vector3 velocity, angularVelocityAxis;
	public final boolean hasBeenTouched;
	public final BallTouch touch;

	public BallData(BallInfo ball){
		super(new Vector3(ball.physics().location()), Constants.BALL_RADIUS);
		this.velocity = new Vector3(ball.physics().velocity());
		this.angularVelocityAxis = new Vector3(ball.physics().angularVelocity());
		this.hasBeenTouched = ball.latestTouch() != null;
		this.touch = (this.hasBeenTouched ? new BallTouch(ball.latestTouch()) : null);
	}

}
