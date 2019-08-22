package wildfire.wildfire.obj;

import wildfire.vector.Vector3;

public class Slice {
	
	private Vector3 position;
	private double time;
	private int frame;

	public Slice(Vector3 position, int frame){
		this.position = position;
		this.frame = frame;
		this.time = (double)frame / 60;
	}
	
	public Slice(Vector3 position, double time){
		this.position = position;
		this.time = time;
		this.frame = (int)(time * 60);
	}
	
	public Slice(rlbot.flat.PredictionSlice predictionSlice, double secondsElapsed){
		this.position = Vector3.fromFlatbuffer(predictionSlice.physics().location());
		this.time = (predictionSlice.gameSeconds() - secondsElapsed);
		this.frame = (int)(this.time * 60);
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