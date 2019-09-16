package wildfire.wildfire.obj;

public class PID {

	public final double kp, ki, kd;
	
	private double lastTime, errorSum, lastError;
	
	public PID(double kp, double ki, double kd){
		this.errorSum = 0;
		this.lastTime = -1;
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
	}
	
	public PID(PID other){
		this(other.kp, other.ki, other.kd);
	}

	public double getOutput(double time, double start, double target){
		// How long since we last calculated.
		if(lastTime == -1) lastTime = time;
		double timeDifference = (double)(time - lastTime);

		// Calculate the error.
		double error = (target - start);
		errorSum += (error * timeDifference);
		double errorDifference = (error - lastError) / timeDifference;

		// Calculate the PID output.
		double output = (kp * error + ki * errorSum + kd * errorDifference);

		// Save the values for the next calculation.
		lastError = error;
		lastTime = time;

		return output;
	}

}
