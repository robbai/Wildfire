package wildfire.wildfire.physics;

import java.util.OptionalDouble;

import wildfire.input.CarData;
import wildfire.vector.Vector3;
import wildfire.wildfire.utils.Constants;

public class JumpPhysics {
	
	public static final double tick = (1D / 60), maxPressTime = 0.2, maxJumpHeight = getMaxHeight(maxPressTime), maxPeakTime = getPeakTime(maxPressTime);
	
//	public static void main(String[] args){
//		double t = (new Random()).nextDouble();
//		for(int z = 0; z < 250; z++){
//			System.out.println("To reach " + z + "uu in " + t + "s, hold for " + getPressForTimeToZ(z, t) + "s");
//		}
//	}
	
	public static OptionalDouble getTimeToZ(double z, double press){
		double velocity = Constants.JUMP_VELOCITY, acceleration = (Constants.JUMP_HOLD_ACCELERATION - Constants.GRAVITY);
		
		double displacement = (velocity * press + 0.5 * acceleration * Math.pow(press, 2));
		if(displacement > z){
			double time = ((Math.sqrt(2 * acceleration * z + Math.pow(velocity, 2)) - velocity) / acceleration);
			if(time < 0) return OptionalDouble.empty();
			return OptionalDouble.of(time);
		}
		
		velocity += (acceleration * press);
		acceleration = -Constants.GRAVITY;
		
		double square = (2 * acceleration * (z - displacement) + Math.pow(velocity, 2));
		if(square < 0) return OptionalDouble.empty();
		double time = press + 
				((Math.sqrt(square) - velocity) / acceleration);
		return OptionalDouble.of(time);
	}
	
//	private static double clampPressTime(double pressTime){
//		return Utils.clamp(pressTime, tick, maxPressTime);
//	}
	
	public static double getMaxHeight(double press){
		double velocity = Constants.JUMP_VELOCITY, acceleration = (Constants.JUMP_HOLD_ACCELERATION - Constants.GRAVITY);
		
		double displacement = (velocity * press + 0.5 * acceleration * Math.pow(press, 2));
		
		velocity += (acceleration * press);
		acceleration = -Constants.GRAVITY;
		
		displacement += -(Math.pow(velocity, 2) / (2 * acceleration));
		return displacement;
	}

	public static OptionalDouble getPressForPeak(double z){
		double g = -Constants.GRAVITY;
		double a = (Constants.JUMP_HOLD_ACCELERATION + g);
		double u = Constants.JUMP_VELOCITY;
		
		double pressTime = ((Math.sqrt(g * (g - a) * (2 * a * z + Math.pow(u, 2))) - a * u + g * u) / (a * (a - g)));
		
		if(pressTime < 0 || pressTime > maxPressTime) return OptionalDouble.empty();
		return OptionalDouble.of(pressTime);
	}
	
	public static OptionalDouble getPressForTimeToZ(double z, double time){
		if(z < 0) return OptionalDouble.empty();
			
		final double epsilon = 0.001;
		
		double low = tick, high = maxPressTime;
		
		OptionalDouble highTime = getTimeToZ(z, high);
		if(!highTime.isPresent()) return OptionalDouble.empty();
		if(Math.abs(highTime.getAsDouble() - time) < epsilon) return OptionalDouble.of(low);
		
//		OptionalDouble lowTime = getTimeToZ(z, low);
//		if(lowTime.isPresent() && Math.abs(lowTime.getAsDouble() - time) < epsilon) return OptionalDouble.of(low);

		while(low + epsilon < high){
			double mid = ((low + high) / 2);
			
			OptionalDouble midTime = getTimeToZ(z, mid);
			
			if(!midTime.isPresent() || midTime.getAsDouble() > time){
				low = mid;
			}else if(midTime.getAsDouble() < time){
				high = mid;
			}
		}
		
		return OptionalDouble.of((low + high) / 2);
	}

	public static double getPeakTimeZ(double z){
		OptionalDouble press = getPressForPeak(z);
		if(!press.isPresent()) press = OptionalDouble.of(z > 0 ? maxPressTime : 0);
		return getPeakTime(press.getAsDouble());
	}
	
	public static double getFastestTimeZ(double z){
		return getPeakTime(maxPressTime);
	}
	
	public static double getPeakTime(double press){
		return press + (Constants.JUMP_VELOCITY + (Constants.JUMP_HOLD_ACCELERATION - Constants.GRAVITY) * press) / Constants.GRAVITY;
	}

	public static Vector3 simCar(CarData car, double press, double time){
		final double step = (1D / 120);
		
		final Vector3 gravity = new Vector3(0, 0, -Constants.GRAVITY * step);
		final Vector3 jumpAcc = car.orientation.roofVector.scaled(Constants.JUMP_HOLD_ACCELERATION * step);
		
		Vector3 position = car.position;
		Vector3 velocity = car.velocity.plus(car.orientation.roofVector.scaled(Constants.JUMP_VELOCITY));
		
		double t = 0;
		while(t < time){
			if(t <= press) velocity = velocity.plus(jumpAcc);
			velocity = velocity.plus(gravity);
			
//			if(velocity.z < 0) break;
			
			position = position.plus(velocity.scaled(step));
			
			t += step;
		}
		
		return position;
	}

}
