package wildfire.input.car;

import rlbot.gamestate.DesiredRotation;
import wildfire.input.Rotator;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class CarOrientation extends Rotator {

    public final Vector3 forward, up, right;

    public CarOrientation(Vector3 noseVector, Vector3 roofVector, double pitch, double yaw, double roll){
    	super(pitch, yaw, roll);
        this.forward = noseVector;
        this.up = roofVector;
        this.right = noseVector.crossProduct(roofVector);
    }
    
    /**
     * https://discordapp.com/channels/348658686962696195/535605770436345857/588542425568641055
     */
    public CarOrientation(Vector3 noseVector, Vector3 rightVector, Vector3 roofVector){
    	super(
    			Math.atan2(noseVector.z, Math.sqrt(rightVector.z * rightVector.z + roofVector.z * roofVector.z)), // Pitch.
    			Math.atan2(noseVector.y, noseVector.x), // Yaw.
    			Math.atan2(rightVector.z, roofVector.z) // Roll.
    			);

        this.forward = noseVector;
        this.up = roofVector;
        this.right = rightVector;
    }

    public CarOrientation(rlbot.flat.Rotator rotator){
        this(rotator.pitch(), rotator.yaw(), rotator.roll());
    }

    /**
     * All parameters are in radians.
     */
    public CarOrientation(double pitch, double yaw, double roll){
    	this(
    			new Vector3(-1 * Math.cos(pitch) * Math.cos(yaw), Math.cos(pitch) * Math.sin(yaw), Math.sin(pitch)), 
    			new Vector3(Math.cos(roll) * Math.sin(pitch) * Math.cos(yaw) + Math.sin(roll) * Math.sin(yaw), Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw), Math.cos(roll) * Math.cos(pitch)), 
    			pitch, yaw, roll
    			);
    }
    
    public CarOrientation(CarOrientation orientation){
		this(orientation.forward, orientation.right, orientation.up);
	}

	public DesiredRotation toDesired(){
    	return new DesiredRotation((float)pitch, (float)yaw, (float)roll);
    }
    
    // TODO
    public static CarOrientation fromVector(Vector3 nose){
    	nose = nose.normalised();
    	double pitch = Math.asin(nose.z);
		double yaw = Math.atan2(nose.y, nose.x) + Math.PI / 2;    	
		return new CarOrientation(pitch, yaw, 0);
    }
    
    public static CarOrientation fromVector(Vector2 nose){
    	return fromVector(nose.withZ(0));
    }

	public CarOrientation step(double dt, Rotator angularVelocity){
		return new CarOrientation(this.pitch + angularVelocity.pitch * dt, this.yaw + angularVelocity.yaw * dt, this.roll + angularVelocity.roll * dt);
	}
    
}
