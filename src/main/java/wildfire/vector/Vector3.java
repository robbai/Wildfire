package wildfire.vector;

import java.util.Random;

import com.google.flatbuffers.FlatBufferBuilder;

import rlbot.gamestate.DesiredVector3;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Vector3 extends rlbot.vector.Vector3 {

	/*
	 * Unit vectors for each axis.
	 */
	public static final Vector3 X = new Vector3(1, 0, 0);
	public static final Vector3 Y = new Vector3(0, 1, 0);
	public static final Vector3 Z = new Vector3(0, 0, 1);

	/*
	 * Attributes (the three axis).
	 */
	public final double x;
	public final double y;
	public final double z;

	/*
	 * Constructors.
	 */

	public Vector3(double x, double y, double z){
		super((float)x, (float)y, (float)z);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3(){
		this(0, 0, 0);
	}

	public Vector3(Vector3 other){
		this(other.x, other.y, other.z);
	}

	public Vector3(rlbot.flat.Vector3 vec){
		// Invert the X value so that the axes make more sense.
		this(-vec.x(), vec.y(), vec.z());
	}

	@Override
	public int toFlatbuffer(FlatBufferBuilder builder){
		return rlbot.flat.Vector3.createVector3(builder, (float)-x, (float)y, (float)z);
	}

	public DesiredVector3 toDesired(){
		return new DesiredVector3((float)-x, (float)y, (float)z);
	}

	/*
	 * Methods
	 */

	public Vector3 plus(Vector3 other){
		return new Vector3(x + other.x, y + other.y, z + other.z);
	}

	public Vector3 minus(Vector3 other){
		return new Vector3(x - other.x, y - other.y, z - other.z);
	}

	public Vector3 scaled(double scale){
		return new Vector3(x * scale, y * scale, z * scale);
	}
	
	public Vector3 multiply(Vector3 other){
		return new Vector3(x * other.x, y * other.y, z * other.z);
	}

	/**
	 * If magnitude is negative, we will return a vector facing the opposite direction.
	 */
	public Vector3 scaledToMagnitude(double magnitude){
		//		if(isZero()) throw new IllegalStateException("Cannot scale up a vector with length zero!");
		if(isZero()) return new Vector3();
		return scaled(magnitude / magnitude());
	}

	public double distance(Vector3 other){
		return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
	}

	public double distanceFlat(Vector2 other){
		return flatten().distance(other);
	}

	public double distanceFlat(Vector3 other){
		return distanceFlat(other.flatten());
	}

	public double magnitude(){
		return Math.sqrt(magnitudeSquared());
	}

	public double magnitudeSquared(){
		return x * x + y * y + z * z;
	}

	public Vector3 normalized(){
		//		if(isZero()) throw new IllegalStateException("Cannot normalize a vector with length zero!");
		if(isZero()) return new Vector3();
		return scaled(1 / magnitude());
	}

	public double dotProduct(Vector3 other){
		return x * other.x + y * other.y + z * other.z;
	}

	public boolean isZero(){
		return x == 0 && y == 0 && z == 0;
	}

	public Vector2 flatten(){
		return new Vector2(x, y);
	}

	public double angle(Vector3 other){
		double magnitude = magnitudeSquared();
		double otherMagnitude = other.magnitudeSquared();
		double dot = dotProduct(other);
		return Math.acos(dot / Math.sqrt(magnitude * otherMagnitude));
	}

	public Vector3 crossProduct(Vector3 v){
		double tx = y * v.z - z * v.y;
		double ty = z * v.x - x * v.z;
		double tz = x * v.y - y * v.x;
		return new Vector3(tx, ty, tz);
	}

	@Override
	public String toString(){
		boolean little = (magnitude() <= 1);
		return "[x=" + (little ? x : Utils.round(x)) + ", y=" + (little ? y : Utils.round(y)) + ", z=" + (little ? z : Utils.round(z)) + "]";
	}

	public Vector3 confine(){
		return new Vector3(Math.min(Constants.PITCH_WIDTH, Math.max(-Constants.PITCH_WIDTH, x)), Math.min(Constants.PITCH_LENGTH, Math.max(-Constants.PITCH_LENGTH, y)), Math.min(Constants.CEILING, Math.max(0, z)));
	}

	public Vector3 rotateHorizontal(double angle){
		return new Vector3(x * Math.cos(angle) - y * Math.sin(angle), y * Math.cos(angle) + x * Math.sin(angle), z);
	}

	// TODO
	public boolean isOutOfBounds(){
		return Math.abs(x) > Constants.PITCH_WIDTH || Math.abs(y) > Constants.PITCH_LENGTH || z > Constants.CEILING || z < 0;
	}

	public Vector3 capMagnitude(double max){
		double mag = magnitude();
		return (mag > max ? scaledToMagnitude(max) : new Vector3(this));
	}

	public Vector3 lerp(Vector3 other, double t){
		return plus(other.minus(this).scaled(t));
	}

	public static Vector3 random(double mag){
		Random random = new Random();
		return new Vector3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scaledToMagnitude(mag);
	}

	public Vector3 withX(double x){
		return new Vector3(x, y, z);
	}

	public Vector3 withY(double y){
		return new Vector3(x, y, z);
	}

	public Vector3 withZ(double z){
		return new Vector3(x, y, z);
	}

	@Override
	public boolean equals(Object obj){
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		Vector3 other = (Vector3)obj;
		if(Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		if(Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
		if(Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z)) return false;
		return true;
	}

}
