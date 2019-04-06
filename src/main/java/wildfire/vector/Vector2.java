package wildfire.vector;

import rlbot.gamestate.DesiredVector3;
import wildfire.wildfire.Utils;

public class Vector2 {

    public final double x;
    public final double y;
    
    public Vector2(){
        this.x = 0;
        this.y = 0;
    }

    public Vector2(double x, double y){
        this.x = x;
        this.y = y;
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

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    public Vector2 scaledToMagnitude(double magnitude){
        if(isZero()) throw new IllegalStateException("Cannot scale up a vector with length zero!");
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

    public Vector2 normalized(){
        if(isZero()) throw new IllegalStateException("Cannot normalize a vector with length zero!");
        return this.scaled(1 / magnitude());
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
    public double angle(Vector2 b){
    	return Math.abs(this.correctionAngle(b));
    }
    
    public rlbot.vector.Vector3 toFramework(){
        // Invert the X value again so that RLBot sees the format it expects.
        return new rlbot.vector.Vector3((float)-x, (float)y, 0);
    }
    
    public Vector2 rotate(double angle){
    	return new Vector2(x * Math.cos(angle) - y * Math.sin(angle), y * Math.cos(angle) + x * Math.sin(angle));
    }

	public Vector3 withZ(double z){
		return new Vector3(x, y, z);
	}
	
	public Vector2 confine(double borderX, double borderY){
		return new Vector2(Math.min(Utils.PITCHWIDTH - borderX, Math.max(-Utils.PITCHWIDTH + borderX, x)), Math.min(Utils.PITCHLENGTH - borderY, Math.max(-Utils.PITCHLENGTH + borderY, y)));
	}
	
	public Vector2 confine(){
		return this.confine(0);
	}
	
	public Vector2 confine(double border){
		return this.confine(border, border);
	}
	
	public Vector2 confine(Vector2 border){
		return this.confine(border.x, border.y);
	}
	
	public Vector2 withX(double x){
		return new Vector2(x, this.y);
	}
	
	public Vector2 withY(double y){
		return new Vector2(this.x, y);
	}
	
	@Override
	public String toString(){
		return "[x=" + Math.round(x * 100) / 100F + ", y=" + Math.round(y * 100) / 100F + "]";
	}
	
	public DesiredVector3 toDesired(){
		return new DesiredVector3().withX((float)-x).withY((float)y).withZ(0F);
	}
	
	public boolean isOutOfBounds(){
		 return Math.abs(x) > Utils.PITCHWIDTH || Math.abs(y) > Utils.PITCHLENGTH;
	}
	
	public double magnitudeInDirection(Vector2 direction){
    	double component = Math.cos(direction.correctionAngle(this));
    	return this.magnitude() * component;
    }
    
}
