package wildfire.vector;

import rlbot.gamestate.DesiredVector3;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Vector2 {

	/*
	 * Unit vectors for each axis.
	 */
	public static final Vector2 X = new Vector2(1, 0);
	public static final Vector2 Y = new Vector2(0, 1);

	/*
	 * Attributes (the two axis).
	 */
	public final double x;
	public final double y;

	/*
	 * Constructors.
	 */

	public Vector2(double x, double y){
		this.x = x;
		this.y = y;
	}

	public Vector2(){
		this(0, 0);
	}

	public Vector2(Vector2 other){
		this(other.x, other.y);
	}

	public Vector2 plus(Vector2 other){
		return new Vector2(x + other.x, y + other.y);
	}

	public Vector2 minus(Vector2 other){
		return new Vector2(x - other.x, y - other.y);
	}

	public Vector2 scaled(double scale){
		return new Vector2(x * scale, y * scale);
	}
	
	public Vector2 multiply(Vector2 other){
		return new Vector2(x * other.x, y * other.y);
	}

	/**
	 * If magnitude is negative, we will return a vector facing the opposite direction.
	 */
	public Vector2 scaledToMagnitude(double magnitude){
		//    	if(isZero()) throw new IllegalStateException("Cannot scale up a vector with length zero!");
		if(isZero()) return new Vector2();
		double scaleRequired = magnitude / magnitude();
		return scaled(scaleRequired);
	}

	public double distance(Vector2 other){
		double xDiff = x - other.x;
		double yDiff = y - other.y;
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

	public double magnitude(){
		return Math.sqrt(magnitudeSquared());
	}

	public double magnitudeSquared(){
		return x * x + y * y;
	}

	public Vector2 normalised(){
		//    	if(isZero()) throw new IllegalStateException("Cannot normalize a vector with length zero!");
		if(isZero()) return new Vector2();
		return scaled(1 / magnitude());
	}

	public double dotProduct(Vector2 other){
		return x * other.x + y * other.y;
	}

	public boolean isZero(){
		return x == 0 && y == 0;
	}

	public double correctionAngle(Vector2 ideal){
		double currentRad = Math.atan2(y, x);
		double idealRad = Math.atan2(ideal.y, ideal.x);

		if(Math.abs(currentRad - idealRad) > Math.PI){
			if(currentRad < 0){
				currentRad += Math.PI * 2;
			}
			if(idealRad < 0){
				idealRad += Math.PI * 2;
			}
		}

		return idealRad - currentRad;
	}

	/**
	 * Will always return a positive value <= Math.PI
	 */
	public static double angle(Vector2 a, Vector2 b){
		return Math.abs(a.correctionAngle(b));
	}
	public double angle(Vector2 other){
		return Math.abs(correctionAngle(other));
	}

	public Vector2 rotate(double angle){
		return new Vector2(x * Math.cos(angle) - y * Math.sin(angle), y * Math.cos(angle) + x * Math.sin(angle));
	}

	public Vector2 confine(double borderX, double borderY){
		return new Vector2(Math.min(Constants.PITCH_WIDTH - borderX, Math.max(-Constants.PITCH_WIDTH + borderX, x)), Math.min(Constants.PITCH_LENGTH - borderY, Math.max(-Constants.PITCH_LENGTH + borderY, y)));
	}

	public Vector2 confine(){
		return confine(0);
	}

	public Vector2 confine(double border){
		return confine(border, border);
	}

	public Vector2 confine(Vector2 border){
		return confine(border.x, border.y);
	}

	public Vector2 withX(double x){
		return new Vector2(x, y);
	}

	public Vector2 withY(double y){
		return new Vector2(x, y);
	}

	public Vector3 withZ(double z){
		return new Vector3(x, y, z);
	}

	@Override
	public String toString(){
		boolean little = (magnitude() <= 1);
		return "[x=" + (little ? x : Utils.round(x)) + ", y=" + (little ? y : Utils.round(y)) + "]";
	}

	public DesiredVector3 toDesired(){
		return new DesiredVector3((float)-x, (float)y, 0F);
	}

	public boolean isOutOfBounds(){
		return Math.abs(x) > Constants.PITCH_WIDTH || Math.abs(y) > Constants.PITCH_LENGTH;
	}

	public double magnitudeInDirection(Vector2 direction){
		double component = Math.cos(direction.correctionAngle(this));
		return magnitude() * component;
	}

	public Vector2 capMagnitude(double max){
		if(max < 0) max = 0;
		double mag = magnitude();
		return (mag > max ? scaledToMagnitude(max) : new Vector2(x, y));
	}

	public Vector2 cross(){
		return new Vector2(-y, x);
	}

	public Vector2 lerp(Vector2 other, double t){
		return plus(other.minus(this).scaled(t));
	}
	
	@Override
	public boolean equals(Object obj){
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		Vector2 other = (Vector2)obj;
		if(Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		if(Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
		return true;
	}
	
	public double component(Vector2 other){
		return normalised().dotProduct(other.normalised());
	}

}
