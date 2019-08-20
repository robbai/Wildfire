package wildfire.wildfire.grabby.grabber;

import java.util.Random;

import wildfire.wildfire.lut.DodgeTable;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class Test {

	public static void main(String[] args){
		DodgeTable table = new DodgeTable();
		
		Random r = new Random();
		for(int i = 0; i < 10; i++){
			double desiredAngle = ((r.nextDouble() - 0.5) * 2 * Math.PI);
			double forwardVelocity = ((r.nextDouble() - 0.5) * 2 * Constants.MAXCARSPEED);
			
//			double desiredAngle = 0.872665;
//			double forwardVelocity = 2281;
			
			System.out.println("So if I'm travelling at " + Utils.round(forwardVelocity) + 
					"uu/s, and I want to dodge at an angle of " + Utils.round(Math.toDegrees(desiredAngle)) + 
					", you're telling me I gotta input an angle of " + Utils.round(Math.toDegrees(table.getInputForAngle(desiredAngle, forwardVelocity))) + "?!");
		}
	}

}
