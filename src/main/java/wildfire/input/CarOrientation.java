package wildfire.input;


import rlbot.flat.Rotator;
import rlbot.gamestate.DesiredRotation;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class CarOrientation {
	
	public double eularPitch;
	public double eularYaw;
	public double eularRoll;

    public Vector3 noseVector;
    public Vector3 roofVector;
    public Vector3 rightVector;

    public CarOrientation(Vector3 noseVector, Vector3 roofVector, double pitch, double yaw, double roll){
    	this.eularPitch = pitch;
    	this.eularYaw = yaw;
    	this.eularRoll = roll;
    	
        this.noseVector = noseVector;
        this.roofVector = roofVector;
        this.rightVector = noseVector.crossProduct(roofVector);
    }

    public static CarOrientation fromFlatbuffer(Rotator rotator){
        return convert(rotator.pitch(), rotator.yaw(), rotator.roll());
    }

    /**
     * All parameters are in radians.
     */
    public static CarOrientation convert(double pitch, double yaw, double roll){
        double noseX = -1 * Math.cos(pitch) * Math.cos(yaw);
        double noseY = Math.cos(pitch) * Math.sin(yaw);
        double noseZ = Math.sin(pitch);

        double roofX = Math.cos(roll) * Math.sin(pitch) * Math.cos(yaw) + Math.sin(roll) * Math.sin(yaw);
        double roofY = Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw);
        double roofZ = Math.cos(roll) * Math.cos(pitch);

        return new CarOrientation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ), pitch, yaw, roll);
    }
    
    public DesiredRotation toDesired(){
    	return new DesiredRotation((float)eularPitch, (float)eularYaw, (float)eularRoll);
    }
    
    public static CarOrientation fromVector(Vector3 nose){
    	nose = nose.normalized();
    	double pitch = Math.asin(nose.z);
		double yaw = -Math.atan2(nose.y, nose.x);
    	
		return convert(pitch, yaw, 0);
    }
    
    public static CarOrientation fromVector(Vector2 nose){
    	return fromVector(nose.withZ(0));
    }
    
}
