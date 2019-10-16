package wildfire.wildfire.obj;

import wildfire.vector.Vector3;

public class Slice {
	
	// TODO don't use 60Hz
	
	protected Vector3 position;
	protected double time;
	protected int frame;

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
		this.position = new Vector3(predictionSlice.physics().location());
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

	@Override
	public String toString(){
		return "Slice [position=" + position + ", time=" + time + ", frame=" + frame + "]";
	}

}
