package wildfire.input;

import wildfire.input.car.CarOrientation;
import wildfire.vector.Vector3;
import wildfire.wildfire.utils.Utils;

public class Rotator {

	public final double pitch, yaw, roll;

	public Rotator(double pitch, double yaw, double roll){
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
	}

	public Rotator(CarOrientation orientation, Vector3 angularVelocityAxis){
		this.pitch = angularVelocityAxis.dotProduct(orientation.right);
	    this.yaw = angularVelocityAxis.dotProduct(orientation.up); // * -1
	    this.roll = angularVelocityAxis.dotProduct(orientation.forward);
	}

	@Override
	public String toString(){
		return "[pitch=" + Utils.round(pitch) + ", yaw=" + Utils.round(yaw) + ", roll=" + Utils.round(roll) + "]";
	}

}
