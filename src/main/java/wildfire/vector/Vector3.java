package wildfire.vector;

import java.util.Random;

import rlbot.gamestate.DesiredVector3;
import wildfire.wildfire.utils.Constants;

public class Vector3 {

    public final double x;
    public final double y;
    public final double z;

    public Vector3(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(){
        this(0, 0, 0);
    }
    
    public Vector3(Vector3 other){
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public static Vector3 fromFlatbuffer(rlbot.flat.Vector3 vec){
        // Invert the X value so that the axes make more sense.
        return new Vector3(-vec.x(), vec.y(), vec.z());
    }

    public rlbot.vector.Vector3 toFramework(){
        // Invert the X value again so that RLBot sees the format it expects.
        return new rlbot.vector.Vector3((float)-x, (float)y, (float)z);
    }

    public Vector3 plus(Vector3 other){
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 minus(Vector3 other){
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scaled(double scale){
        return new Vector3(x * scale, y * scale, z * scale);
    }

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    public Vector3 scaledToMagnitude(double magnitude){
        if(isZero()) throw new IllegalStateException("Cannot scale up a vector with length zero!");
        double scaleRequired = magnitude / magnitude();
        return scaled(scaleRequired);
    }

    public double distance(Vector3 other){
        double xDiff = x - other.x;
        double yDiff = y - other.y;
        double zDiff = z - other.z;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
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
//        if(isZero()) throw new IllegalStateException("Cannot normalize a vector with length zero!");
    	if(isZero()) return new Vector3();
        return this.scaled(1 / magnitude());
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

    public double angle(Vector3 v){
        double mag2 = magnitudeSquared();
        double vmag2 = v.magnitudeSquared();
        double dot = dotProduct(v);
        return Math.acos(dot / Math.sqrt(mag2 * vmag2));
    }

    public Vector3 crossProduct(Vector3 v){
        double tx = y * v.z - z * v.y;
        double ty = z * v.x - x * v.z;
        double tz = x * v.y - y * v.x;
        return new Vector3(tx, ty, tz);
    }

	@Override
	public String toString(){
		return "[x=" + Math.round(x * 100) / 100F + ", y=" + Math.round(y * 100) / 100F + ", z=" + Math.round(z * 100) / 100F + "]";
	}
	
	public Vector3 confine(){
		return new Vector3(Math.min(Constants.PITCH_WIDTH, Math.max(-Constants.PITCH_WIDTH, x)), Math.min(Constants.PITCH_LENGTH, Math.max(-Constants.PITCH_LENGTH, y)), Math.min(Constants.CEILING, Math.max(0, z)));
	}
	
	public Vector3 rotateHorizontal(double angle){
    	return new Vector3(x * Math.cos(angle) - y * Math.sin(angle), y * Math.cos(angle) + x * Math.sin(angle), z);
    }
	
	public DesiredVector3 toDesired(){
		return new DesiredVector3().withX((float)-x).withY((float)y).withZ((float)z);
	}

	public boolean isOutOfBounds(){
		 return Math.abs(x) > Constants.PITCH_WIDTH || Math.abs(y) > Constants.PITCH_LENGTH || z > Constants.CEILING || z < 0;
	}
	
	public Vector3 capMagnitude(double max){
		double mag = this.magnitude();
		return (mag > max ? this.scaledToMagnitude(max) : new Vector3(this));
	}

	public Vector3 lerp(Vector3 other, double t){
		return this.plus(other.minus(this).scaled(t));
	}

	public static Vector3 random(double mag){
		Random random = new Random();
		return new Vector3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scaledToMagnitude(mag);
	}
	
	public Vector3 withX(double x){
		return new Vector3(x, this.y, this.z);
	}
	
	public Vector3 withY(double y){
		return new Vector3(this.x, y, this.z);
	}
	
	public Vector3 withZ(double z){
		return new Vector3(this.x, this.y, z);
	}
    
}
