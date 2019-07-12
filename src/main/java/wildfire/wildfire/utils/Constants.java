package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;

public class Constants {

	public static final float BALLRADIUS = 92.75F;
	public static final float BOOSTACC = (911F + (2F / 3));
	public static final float COASTACC = 525;
	public static final float BRAKEACC = 3500;
	public static final float MAXCARSPEED = 2300;
	public static final float SUPERSONIC = 2200;
	public static final float CEILING = 2044F;
	public static final float GOALHALFWIDTH = 892.755F;
	public static final float GOALHEIGHT = 642.775F;
	public static final float GRAVITY = 650F;
	public static final float PITCHLENGTH = 5120F;
	public static final float PITCHWIDTH = 4096F;
	
	public static Vector2 homeGoal(int team){
		return new Vector2(0, Utils.teamSign(team) * -PITCHLENGTH);
	}
	
	public static Vector2 enemyGoal(int team){
		return new Vector2(0, Utils.teamSign(team) * PITCHLENGTH);
	}
	
	public static Vector2 enemyGoal(CarData car){
		return new Vector2(0, Utils.teamSign(car) * PITCHLENGTH);
	}

}
