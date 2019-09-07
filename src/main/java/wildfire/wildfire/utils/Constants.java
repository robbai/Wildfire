package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;

public class Constants {
	
	// TODO: Refactor all of these to use underscores

	public static final double BALLRADIUS = 92.75;
	public static final double BOOSTACC = (911 + (2 / 3));
	public static final double COASTACC = 525;
	public static final double BRAKEACC = 3500;
	public static final double MAXCARSPEED = 2300;
	public static final double SUPERSONIC = 2200;
	public static final double CEILING = 2044;
	public static final double GOALHALFWIDTH = 892.755;
	public static final double GOALHEIGHT = 642.775;
	public static final double GRAVITY = 650;
	public static final double PITCHLENGTH = 5120;
	public static final double PITCHWIDTH = 4096;
	public static final double CARHEIGHT = 17.049999237060547;
	public static final double MAXCARTHROTTLE = 1410;
	public static final double BOOSTRATE = 33.3;
	public static final double JUMPACC = 1400;
	public static final double JUMPVEL = 300;
	
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
