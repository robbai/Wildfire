package wildfire.wildfire.obj;

import wildfire.vector.Vector3;

public class Impact extends Slice {

	private Vector3 ballPosition;
	
	public Impact(Vector3 position, Vector3 ballPosition, double time){
		super(position, time);
		this.ballPosition = ballPosition;
	}

	public Impact(Vector3 impact, rlbot.flat.PredictionSlice ballPosition, double time){
		this(impact, new Vector3(ballPosition.physics().location()), time);
	}

	public Vector3 getBallPosition(){
		return ballPosition;
	}

	public Impact withTime(double time){
		return new Impact(this.getPosition(), this.getBallPosition(), time);
	}

}
