package wildfire.wildfire.obj;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

public class PID {
	
	/*
	 * Rendering
	 */
	private boolean render;
	private WRenderer renderer;
	private ArrayList<Double> data;
	private Color colour;
	
	private float lastTime;
	public double errorSum, lastError;
	private double kp, ki, kd;
	
	public PID(WRenderer renderer, Color colour, double kp, double ki, double kd){
		this.render = true;
		this.renderer = renderer;
		this.colour = colour;
		this.data = new ArrayList<Double>();
		
		this.errorSum = 0;
		this.lastTime = -1;
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
	}
	
	public PID(double kp, double ki, double kd){
		this.render = false;
		
		this.errorSum = 0;
		this.lastTime = -1;
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
	}
	
	public PID(PID other){
		this.render = false;
		
		this.errorSum = 0;
		this.lastTime = -1;
		this.kp = other.kp;
		this.ki = other.ki;
		this.kd = other.kd;
	}

	public double getOutput(float time, double start, double target){
		//How long since we last calculated
		if(lastTime == -1) lastTime = time;
		double timeDifference = (double)(time - lastTime);

		//Calculate the error
		double error = (target - start);
		if(render){
			data.add(error);
			while(data.size() > 10) data.remove(0);
		}
		errorSum += (error * timeDifference);
		double errorDifference = (error - lastError) / timeDifference;

		//Calculate the PID output
		double output = (kp * error + ki * errorSum + kd * errorDifference);
//		if(render && errorSum != error * timeDifference) data.add(output);

		//Rendering
		if(render && data.size() > 1){
			double width = 400;
			double height = 50;
			double startY = 150;
			renderer.drawLine2d(Color.GREEN, new Point(0, (int)startY), new Point((int)width, (int)startY));
			double highestValue = 0;
			for(double value : data) highestValue = Math.max(highestValue, Math.abs(value));
			for(int i = 1; i < data.size(); i++){
				double e = data.get(i);
				double lastValue = data.get(i - 1);

				double y = startY - e / highestValue * height;
				double lastY = startY - lastValue / highestValue * height;
				renderer.drawLine2d(colour, new Point((int)((double)(i - 1) / (double)data.size() * width), (int)lastY), new Point((int)((double)i / (double)data.size() * width), (int)y));
			}
		}

		//Save the values for the next calculation
		lastError = error;
		lastTime = time;

		return output;
	}
	
	public PID withRender(boolean render){
		this.render = (render && this.renderer != null);
		return this;
	}

	public void set(double p, double i, double d){
		this.kp = p;
		this.ki = i;
		this.kd = d;
	}
	
	public PID set(PID pid){
		this.set(pid.kp, pid.ki, pid.kd);
		return this;
	}

	public PID updateRenderer(WRenderer renderer){
		if(this.render) this.renderer = renderer;
		return this;
	}

}
