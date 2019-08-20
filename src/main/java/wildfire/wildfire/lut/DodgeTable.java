package wildfire.wildfire.lut;

import org.apache.commons.csv.CSVRecord;

import wildfire.wildfire.obj.Table;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class DodgeTable extends Table {
	
	/*
	 * Headers: "input_angle", "velocity_angle", "displace_angle", "impulse_angle", "displace_forward", "displace_side", "start_speed", "end_speed", "time_taken"
	 * Input angles are measured in radians, taken in steps of 10 degrees, ranging from 0 to Pi
	 * Start speeds are forward velocities, taken in steps of 10uu/s, ranging from approximately ±2300
	 */
	
	private double minVelocity, maxVelocity;
	
	private static int differentSpeeds = 47, differentAngles = 19;

	public DodgeTable(){
		super("dodge.csv");
		
		/*
		 *  Find speed bounds.
		 *  They're just short of ±2300 D: 
		 */
		this.minVelocity = Constants.MAXCARSPEED;
		this.maxVelocity = -Constants.MAXCARSPEED;
		for(CSVRecord record : this.records){
			double startSpeed = Double.parseDouble(record.get("start_speed"));
			if(Math.abs(Constants.MAXCARSPEED - Math.abs(startSpeed)) < 90) continue;
			this.minVelocity = Math.min(this.minVelocity, startSpeed);
			this.maxVelocity = Math.max(this.maxVelocity, startSpeed);
		}
		System.out.println(this.minVelocity);
		System.out.println(this.maxVelocity);
	}
	
	public double getInputForAngle(double desiredAngle, double forwardVelocity){
		// Handle the parameters
		double angleSign = Math.signum(desiredAngle);
		desiredAngle = Utils.clamp(Math.abs(desiredAngle), 0, Math.PI);
		forwardVelocity = Utils.clamp(forwardVelocity, this.minVelocity, this.maxVelocity);
		boolean backwards = (forwardVelocity < 0), backflip = (Math.cos(desiredAngle) < 0);
		
		// Handle the angle(s)
		double[] displaceAngles = new double[differentAngles],
				inputAngles = new double[differentAngles];
		for(int i = (backflip ? differentAngles - 1 : 0); (backflip ? i > -1 : i < differentAngles); i += (backflip ? -1 : 1)){
			inputAngles[i] = Double.parseDouble(this.records.get(i * differentSpeeds).get("input_angle"));
			for(int j = (backwards ? 0 : differentSpeeds - 2); (backwards ? j < (differentSpeeds - 1) : j > -1); j += (backwards ? 1 : -1)){
//			for(int j = (differentSpeeds - 2); j > -1; j--){
				double lowerSpeed = Double.parseDouble(this.records.get(i * differentSpeeds + j).get("start_speed"));
				double upperSpeed = Double.parseDouble(this.records.get(i * differentSpeeds + Math.min(j + 1, differentSpeeds - 1)).get("start_speed"));
				double speedLerp = ((forwardVelocity - lowerSpeed) / (upperSpeed - lowerSpeed));
				
				// Check if the speed fits the boundaries.
				if(speedLerp <= 0 || speedLerp > 1) continue;
				
				// Fill in the input angle.
				double lowerAngle = Double.parseDouble(this.records.get(i * differentSpeeds + j).get("impulse_angle"));
				double upperAngle = Double.parseDouble(this.records.get(i * differentSpeeds + Math.min(j + 1, differentSpeeds - 1)).get("impulse_angle"));
				displaceAngles[i] = Utils.lerp(lowerAngle, upperAngle, speedLerp);
				
				break;
			}
		}
		
//		for(int i = 0; i < differentAngles - 1; i ++){
		for(int i = (backflip ? differentAngles - 2 : 0); (backflip ? i > -1 : i < differentAngles - 1); i += (backflip ? -1 : 1)){
			double lowerEffectAngle = displaceAngles[i];
			double upperEffectAngle = displaceAngles[i + 1];
			double angleLerp = ((desiredAngle - lowerEffectAngle) / (upperEffectAngle - lowerEffectAngle));
			
			// Check if the angle fits the boundaries.
			if((angleLerp < 0 && i != 0) || (angleLerp > 1 && i != differentAngles - 2)) continue;
			
			// Return the input angle.
			return Utils.lerp(inputAngles[i], inputAngles[i + 1], angleLerp) * angleSign;
		}
		
		return (Math.abs(displaceAngles[0] - desiredAngle) < Math.abs(displaceAngles[differentAngles - 1] - desiredAngle)
				? inputAngles[0] : inputAngles[differentAngles - 1]) * angleSign;
	}
	
	public double getInputForAngle(double desiredAngle, double forwardVelocity, boolean print){
		double input = getInputForAngle(desiredAngle, forwardVelocity);
		if(print) System.out.println("Dodging at " + (int)Math.toDegrees(input) + "° to produce " + (int)Math.toDegrees(desiredAngle) + "° (" + (int)forwardVelocity + ")");
		return input;
	}

}
