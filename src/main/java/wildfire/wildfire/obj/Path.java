package wildfire.wildfire.obj;

import java.awt.Color;
import java.util.ArrayList;

import rlbot.flat.BallPrediction;
import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;

public class Path {
	
	/*
	 * Constants
	 */
	private final double scale = 0.03, steerMultiplier = -2.5, distanceThreshold = 100, rotationalValue = 0.4193;
	private final Vector2 confineBorder = new Vector2(120, 240);
	private final int maxPathLength = 110;
	
	private ArrayList<Vector2> points;
	private Vector2 destination;
	
	private Vector2 ball;
	private CarData car;
	private boolean bad;
	private double time = 0;

	public Path(CarData car, Vector2 ball, Vector2 destination){
		this.car = car;
		this.ball = ball;
		this.destination = destination;
		this.bad = false;
		this.points = new ArrayList<Vector2>();
		
		this.generatePath();
		if(points.size() == maxPathLength + 2){
			this.bad = true;
		}else{
			this.bad = false;
//			for(Vector2 vector : this.points){
//				if(vector.isOutOfBounds()){
//					this.bad = true;
//					break;
//				}
//			}
		}
	}
	
	public static Path fromBallPrediction(Wildfire wildfire, CarData car, Vector2 destination){
		BallPrediction ballPrediction = wildfire.ballPrediction;
		
		double initialVelocity = car.velocity.flatten().magnitude();
		
		for(int i = wildfire.impactPoint.getFrame(); i < ballPrediction.slicesLength(); i++){
			Vector3 location = Vector3.fromFlatbuffer(ballPrediction.slices(i).physics().location());
			Path p = new Path(car, location.flatten(), destination);
			if(p.isBadPath()) return null;
//			if(p.isBadPath()) continue;
			
			double displacement = (p.getDistance() - Utils.BALLRADIUS);
			double time = (double)i / 60D;
			double acceleration = 2 * (displacement - initialVelocity * time) / Math.pow(time, 2);
			
			if(initialVelocity + acceleration * time < Math.max(initialVelocity, Utils.boostMaxSpeed(initialVelocity, car.boost))){
				return p.setTime(time);
			}
		}
		
		return null;
	}
	
	private void generatePath(){
		points.clear();
		
		Vector2 start = ball;
//		Vector2 finish = ar.position.plus(car.velocity.scaled(1D / 60)).flatten();
		Vector2 finish = car.position.flatten();
		points.add(start);
		
		//Offset, this is so we line up the goal before we reach the ball
		start = start.plus(start.minus(destination).scaledToMagnitude(Math.min(475, start.distance(finish) / 2))).confine(confineBorder);
		points.add(start);
		
		double velocity = Math.max(car.velocity.magnitude(), 1410);
		Vector2 rotation = ball.minus(destination).scaledToMagnitude(velocity * scale);
		
		for(int i = 0; i < maxPathLength; i++){
			double s = Utils.aimFromPoint(start, rotation, finish) * steerMultiplier;
			rotation = rotation.rotate(-Utils.clampSign(s) / (rotationalValue  / scale));
			
			Vector2 end = start.plus(rotation).confine(confineBorder);
//			renderer.drawLine3d(i % 4 < 2 ? (car.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, start.toFramework(), end.toFramework());
			
			if(end.distance(finish) < distanceThreshold) break;
			points.add(end);
			start = end;
		}
	}
	
	public void renderPath(WRenderer renderer){
		for(int i = 0; i < (this.points.size() - 1); i++){
			Vector2 a = this.points.get(i);
			Vector2 b = this.points.get(i + 1);
			renderer.drawLine3d(i % 4 < 2 ? (car.team == 0 ? Color.BLUE : Color.ORANGE) : Color.WHITE, a.toFramework(), b.toFramework());
		}
	}
	
	public boolean isBadPath(){
		return bad;
	}
	
	public int getSize(){
		return this.points.size();
	}
	
	public Vector2 getPly(int targetPly){
		return this.points.get(points.size() >= targetPly ? (points.size() - targetPly) : 0);
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

}
