package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;

public class Constants {

	public static final double BALLRADIUS = 92.75F;
	public static final double BOOSTACC = (911F + (2F / 3));
	public static final double COASTACC = 525;
	public static final double BRAKEACC = 3500;
	public static final double MAXCARSPEED = 2300;
	public static final double SUPERSONIC = 2200;
	public static final double CEILING = 2044F;
	public static final double GOALHALFWIDTH = 892.755F;
	public static final double GOALHEIGHT = 642.775F;
	public static final double GRAVITY = 650F;
	public static final double PITCHLENGTH = 5120F;
	public static final double PITCHWIDTH = 4096F;
	public static final double CARHEIGHT = 17.049999237060547D;
	
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
