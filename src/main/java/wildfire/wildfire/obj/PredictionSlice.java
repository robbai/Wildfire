package wildfire.wildfire.obj;

import wildfire.vector.Vector3;

public class PredictionSlice {
	
	private Vector3 position;
	private int frame;
	private double time;

	public PredictionSlice(Vector3 position, int frame){
		this.position = position;
		this.frame = frame;
		this.time = (double)frame / 60;
	}
	
	public PredictionSlice(Vector3 position, double time){
		this.position = position;
		this.time = time;
		this.frame = (int)(time * 60);
	}
	
	public PredictionSlice(rlbot.flat.PredictionSlice predictionSlice, int frame){
		this.position = Vector3.fromFlatbuffer(predictionSlice.physics().location());
		this.frame = frame;
		this.time = (double)frame / 60;
	}
	
	public Vector3 getPosition(){
		return position;
	}

	public int getFrame(){
		return frame;
	}

	public double getTime(){
		return time;
	}

}
