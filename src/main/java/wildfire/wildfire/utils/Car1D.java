package wildfire.wildfire.utils;

import wildfire.wildfire.physics.DrivePhysics;

public class Car1D {

	private double distance, velocity, boost, time;

	public Car1D(double distance, double velocity, double boost, double time){
		this.distance = distance;
		this.velocity = velocity;
		this.boost = boost;
		this.time = time;
	}

	public Car1D(Car1D car){
		this(car.getDistance(), car.getVelocity(), car.getBoost(), car.getTime());
	}

	public double getDistance(){
		return distance;
	}

	public double getTime(){
		return time;
	}

	public double getVelocity(){
		return velocity;
	}

	public double getBoost(){
		return boost;
	}

	public void step(double dt, double throttle, boolean boost){
		if(this.boost <= 0){
			boost = false;
		}else{
			this.boost -= Constants.BOOST_RATE * dt;
		}

		double acceleration = DrivePhysics.determineAcceleration(this.velocity, throttle, boost);

		this.velocity += acceleration * dt;
		if(Math.abs(this.velocity) > Constants.MAX_CAR_VELOCITY){
			this.velocity = Math.signum(this.velocity) * Constants.MAX_CAR_VELOCITY;
		}

		this.distance += this.velocity * dt;

		this.time += dt;
	}

	public double distanceFrom(double targetDistance){
		return Math.max(0, targetDistance - this.distance);
	}

}
