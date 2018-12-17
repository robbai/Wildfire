package wildfire.input;


import rlbot.flat.PlayerInfo;
import rlbot.gamestate.DesiredRotation;
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

    public static CarOrientation fromFlatbuffer(PlayerInfo playerInfo){
        return convert(playerInfo.physics().rotation().pitch(), playerInfo.physics().rotation().yaw(), playerInfo.physics().rotation().roll());
    }
    
    public static CarOrientation fromFlatbufferAngularVelocity(PlayerInfo playerInfo){
        return convert(playerInfo.physics().angularVelocity().y(), playerInfo.physics().angularVelocity().z(), playerInfo.physics().angularVelocity().x());
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
//    	return new DesiredRotation((float)noseVector.x, (float)noseVector.y, (float)noseVector.z);
    	return new DesiredRotation((float)eularPitch, (float)eularYaw, (float)eularRoll);
    }
    
}
