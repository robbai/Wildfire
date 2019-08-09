package wildfire.wildfire.curve;

import java.awt.Color;
import java.util.ArrayList;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Physics;
import wildfire.wildfire.utils.Utils;

public class Path {
	
	/*
	 * Constants
	 */
	public final static double scale = 0.01, distanceThreshold = 60, lineupUnits = (Constants.BALLRADIUS + 30);
//	public final static Vector2 confineBorder = new Vector2(120, 240);
	public final static int maxPathLength = 350, polyRender = 9;
		
	private Vector2 ball;
	private CarData car;
	private double velocity;

	/*
	 * Results
	 */
	private ArrayList<Vector2> points;
	private Vector2 destination;
	private double time = 0;
	private boolean bad;

	public Path(CarData car, Vector2 ball, Vector2 destination, double velocity){
		this.points = new ArrayList<Vector2>();
		this.destination = destination;
		this.velocity = velocity;
		this.ball = ball;
		this.bad = false;
		this.car = car;
		
		this.generatePath();
	}
	
	public static Path fromBallPrediction(Wildfire wildfire, CarData car, Vector2 destination, double desiredVelocity, boolean force){
		BallPrediction ballPrediction = wildfire.ballPrediction;
		
//		double initialVelocity = car.velocity.flatten().magnitude();
		
		for(int i = wildfire.impactPoint.getFrame(); i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			
			Path p = new Path(car, location.flatten(), destination, desiredVelocity);
			
			if(!force){
				if(p.isBadPath()) return null;
//				if(p.isBadPath()) continue;
			}
			
			double distance = (p.getDistance() - lineupUnits);
			double time = (ballPrediction.slices(i).gameSeconds() - car.elapsedSeconds);
			
			if(Math.abs(desiredVelocity - distance / time) < 60){
				return p.setTime(time);
			}
		}
		
		return null;
	}
	
	private void generatePath(){
		points.clear();
		this.bad = true;
		
		Vector2 start = ball;
		Vector2 finish = car.position.flatten();
		points.add(start);
		
		double turningRadius = Physics.getTurnRadius(velocity);
		
		double stepDistance = (velocity * scale);
		Vector2 step = ball.minus(destination).scaledToMagnitude(stepDistance);
		
		// Offset, this is so we line up the goal before we reach the ball.
		for(int i = 0; i < Math.ceil(lineupUnits / stepDistance); i++){
			start = start.plus(start.minus(destination).scaledToMagnitude(stepDistance));
			points.add(start);
			if(start.distance(finish) < distanceThreshold){
				this.bad = false;
				break;			
			}
		}
		
		if(this.bad){
			for(int i = 0; i < maxPathLength; i++){
				double dirAngle = Handling.aimFromPoint(start, step, finish);
				double maxAngle = (2 * stepDistance) / (2 * turningRadius);
				step = step.rotate(Utils.clamp(dirAngle, -maxAngle, maxAngle));
				
				Vector2 end = start.plus(step);
				points.add(end);
				
				// Path has finished, successfully!
				if(end.distance(finish) < distanceThreshold){
					this.bad = false;
					break;			
				}
				
				start = end;
			}
		}
	}
	
	public void renderPath(WRenderer renderer){
		for(int i = 0; i < (this.points.size() - 1); i += polyRender){
			Vector2 a = this.points.get(i);
			Vector2 b = this.points.get(Math.min(this.points.size() - 1, i + polyRender));
			renderer.drawLine3d((i % 2 == 0 ? (car.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE), a.toFramework(), b.toFramework());
		}
	}
	
	public boolean isBadPath(){
		return bad;
	}
	
	public int getSize(){
		return this.points.size();
	}
	
	public Vector2 getPly(double targetPly){
		targetPly = Math.max(0, Math.min(this.points.size() - 1, points.size() - 1 - targetPly));
		
		//Whole number
		if(targetPly == Math.ceil(targetPly)){
			return this.points.get((int)targetPly);
		}
		
		int lower = (int)Math.floor(targetPly);
		int upper = (int)Math.ceil(targetPly);
		double difference = (Math.ceil(targetPly) - Math.floor(targetPly));
		
		return this.points.get(lower).plus(this.points.get(upper).minus(this.points.get(lower)).scaled(difference));
	}

	public double getDistance(){
		double distance = 0;
		for(int i = 0; i < (this.points.size() - 1); i++){
			Vector2 a = this.points.get(i);
			Vector2 b = this.points.get(i + 1);
			distance += a.distance(b);
		}
		return distance;
	}

	public double getTime(){
		return time;
	}

	public Path setTime(double time){
		this.time = time;
		return this;
	}
	
	public double getVelocity(){
		return velocity;
	}

}
