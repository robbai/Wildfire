package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class Constants {

	public static final double BALL_RADIUS = 92.75;
	public static final double BOOST_GROUND_ACCELERATION = (911 + (2 / 3));
	public static final double BOOST_AIR_ACCELERATION = 1000;
	public static final double COAST_ACCELERATION = 525;
	public static final double BRAKE_ACCELERATION = 3500;
	public static final double MAX_CAR_VELOCITY = 2300;
	public static final double SUPERSONIC_VELOCITY = 2200;
	public static final double CEILING = 2044;
	public static final double GOAL_WIDTH = 892.755; // Half.
	public static final double GOAL_HEIGHT = 642.775;
	public static final double GRAVITY = 650;
	public static final double PITCH_LENGTH = 5120; // Half.
	public static final double PITCH_WIDTH = 4096; // Half.
	public static final double CAR_HEIGHT = 17.049999237060547;
	public static final double MAX_THROTTLE_VELOCITY = 1410;
	public static final double BOOST_RATE = 33.3;
	public static final double JUMP_HOLD_ACCELERATION = 1400;
	public static final double JUMP_VELOCITY = 300;
	public static final Vector3 RIPPER = new Vector3(83.28, 127.93, 31.30); // https://discordapp.com/channels/348658686962696195/348661571297214465/617774276879056919
	public static final Vector3 RIPPER_OFFSET = new Vector3(9, 0, 15.75);
	
	public static Vector2 homeGoal(int team){
		return new Vector2(0, Utils.teamSign(team) * -PITCH_LENGTH);
	}
	
	public static Vector2 enemyGoal(int team){
		return new Vector2(0, Utils.teamSign(team) * PITCH_LENGTH);
	}
	
	public static Vector2 enemyGoal(CarData car){
		return new Vector2(0, Utils.teamSign(car) * PITCH_LENGTH);
	}

}
