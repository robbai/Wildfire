package wildfire.wildfire.obj;

import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class Impact extends Slice {

	private Vector3 ballPosition;

	public Impact(Vector3 position, Vector3 ballPosition, double time){
		super(correctOffset(position), time);
		this.ballPosition = ballPosition;
	}

	public Impact(Vector3 impact, rlbot.flat.PredictionSlice ballPosition, double time){
		this(impact, new Vector3(ballPosition.physics().location()), time);
	}

	public Vector3 getBallPosition(){
		return ballPosition;
	}

	public Impact withTime(double time){
		return new Impact(this.position, this.ballPosition, time);
	}

	private Impact withGoal(Vector2 goal){
		Vector3 currentOffset = this.position.minus(this.ballPosition);
		double originalFlatMagnitude = currentOffset.flatten().magnitude();
		Vector3 position = this.ballPosition.plus(this.ballPosition.flatten().minus(goal).scaledToMagnitude(originalFlatMagnitude).withZ(currentOffset.z));
		return new Impact(position, this.ballPosition, this.time);
	}

	public Impact withGoal(Vector3 goal){
		return withGoal(goal.flatten());
	}

	public double offsetMagnitude(){
		return this.position.distance(this.ballPosition);
	}

	@Override
	public String toString(){
		return "Impact [ballPosition=" + ballPosition + ", position=" + position + ", time=" + time + ", frame=" + frame + "]";
	}

	/**
	 * The offset is corrected so the ball 
	 */
	private static Vector3 correctOffset(Vector3 position){
		return position; // TODO
	}

}
