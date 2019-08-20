package wildfire.wildfire.physics;

import wildfire.input.CarData;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class JumpPhysics {
	
	private static final double tick = (1D / 60);
	
	public static final double maxPressTime = 0.2, maxJumpVelocity = getJumpVelocity(maxPressTime), maxJumpHeight = getJumpHeight(maxPressTime);
	
//	public static void main(String[] args){
//		for(int h = 0; h < 300; h++){
//			System.out.println(h + "uu: " + Utils.round(getPressTime(h)) + "s");
//		}
//		for(double t = 0; t <= maxPressTime; t += 0.005){
//			System.out.println(Utils.round(t) + "s: " + Utils.round(getJumpVelocity(t)) + "uu/s, " + Utils.round(getPeakTime(t)) + "s");
//		}
//	}

	public static double getJumpVelocity(double pressTime){
		pressTime = clampPressTime(pressTime);
		return Math.sqrt(Math.pow(1400D * pressTime + 300D, 2) - (Constants.GRAVITY * 1400D * Math.pow(pressTime, 2)));
	}
	
	/** This uses hard-coded gravity (-650uu/s^2),
	 *  since WolframAlpha convinced me it's not worth it rearranging to have gravity on the other side
	 */
	public static double getPressTime(double height){
		double t = 0.1D * Math.sqrt(13D / 105D) * Math.sqrt(height + 60D) - 0.4D;
//		double t = 0.03518657752D * Math.sqrt(height + 60D) - 0.4D;
		return clampPressTime(t);
	}
	
	private static double clampPressTime(double time){
		return Utils.clamp(time, tick, maxPressTime);
	}

	public static double getPressTime(CarData car, Vector3 vec){
		return getPressTime(Utils.toLocal(car, vec).z);
	}
	
	public static double getPeakTime(double pressTime){
		double jumpVelocity = getJumpVelocity(clampPressTime(pressTime));
		double timeTaken = -(jumpVelocity / -Constants.GRAVITY);		
		return timeTaken;
	}

	public static double getJumpHeight(double pressTime){
		double jumpVelocity = getJumpVelocity(pressTime);
		return -(Math.pow(jumpVelocity, 2) / (2 * -Constants.GRAVITY));
	}
	
	public static double getPeakTime(CarData car, Vector3 vec){
		return getPeakTime(getPressTime(Utils.toLocal(car, vec).z));
	}

	public static double getPeakTime(CarData car, Slice slice){
		return getPeakTime(car, slice.getPosition());
	}

}
