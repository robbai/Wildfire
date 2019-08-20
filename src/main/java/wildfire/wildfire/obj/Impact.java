package wildfire.wildfire.obj;

import wildfire.vector.Vector3;

public class Impact extends Slice {

	private Vector3 ballPosition;

	public Impact(Vector3 impact, rlbot.flat.PredictionSlice predictionSlice, double time){
		super(impact, time);
		this.ballPosition = Vector3.fromFlatbuffer(predictionSlice.physics().location());
	}

	public Vector3 getBallPosition(){
		return ballPosition;
	}

}
