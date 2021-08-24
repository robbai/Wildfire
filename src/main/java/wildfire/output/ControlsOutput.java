package wildfire.output;

import java.util.Random;

import rlbot.ControllerState;

public class ControlsOutput implements ControllerState {

	// 0 is straight, -1 is hard left, 1 is hard right.
	private float steer;

	// -1 for front flip, 1 for back flip
	private float pitch;

	// 0 is straight, -1 is hard left, 1 is hard right.
	private float yaw;

	// 0 is straight, -1 is hard left, 1 is hard right.
	private float roll;

	// 0 is none, -1 is backwards, 1 is forwards
	private float throttle;

	private boolean jumpDepressed;
	private boolean boostDepressed;
	private boolean slideDepressed;
	private boolean useItemDepressed;

	public ControlsOutput(float steer, float pitch, float yaw, float roll, float throttle, boolean jumpDepressed,
			boolean boostDepressed, boolean slideDepressed, boolean useItemDepressed){
		this.steer = steer;
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
		this.throttle = throttle;
		this.jumpDepressed = jumpDepressed;
		this.boostDepressed = boostDepressed;
		this.slideDepressed = slideDepressed;
		this.useItemDepressed = useItemDepressed;
	}

	public ControlsOutput(){
		// Empty.
	}

	public ControlsOutput withSteer(double steer){
		this.steer = clamp(steer);
		return this;
	}

	public ControlsOutput withPitch(double pitch){
		this.pitch = clamp(pitch);
		return this;
	}

	public ControlsOutput withYaw(double yaw){
		this.yaw = clamp(yaw);
		return this;
	}

	public ControlsOutput withRoll(double roll){
		this.roll = clamp(roll);
		return this;
	}

	public ControlsOutput withThrottle(double throttle){
		this.throttle = clamp(throttle);
		return this;
	}

	public ControlsOutput withJump(boolean jumpDepressed){
		this.jumpDepressed = jumpDepressed;
		return this;
	}

	public ControlsOutput withBoost(boolean boostDepressed){
		this.boostDepressed = boostDepressed;
		return this;
	}

	public ControlsOutput withSlide(boolean slideDepressed){
		this.slideDepressed = slideDepressed;
		return this;
	}

	public ControlsOutput withUseItem(boolean useItemDepressed){
		this.useItemDepressed = useItemDepressed;
		return this;
	}

	public ControlsOutput withJump(){
		this.jumpDepressed = true;
		return this;
	}

	public ControlsOutput withBoost(){
		this.boostDepressed = true;
		return this;
	}

	public ControlsOutput withSlide(){
		this.slideDepressed = true;
		return this;
	}

	public ControlsOutput withUseItem(){
		this.useItemDepressed = true;
		return this;
	}

	private float clamp(float value){
		return Math.max(-1, Math.min(1, value));
	}

	private float clamp(double value){
		return clamp((float)value);
	}

	@Override
	public float getSteer(){
		return steer;
	}

	@Override
	public float getThrottle(){
		return throttle;
	}

	@Override
	public float getPitch(){
		return pitch;
	}

	@Override
	public float getYaw(){
		return yaw;
	}

	@Override
	public float getRoll(){
		return roll;
	}

	@Override
	public boolean holdJump(){
		return jumpDepressed;
	}

	@Override
	public boolean holdBoost(){
		return boostDepressed;
	}

	@Override
	public boolean holdHandbrake(){
		return slideDepressed;
	}

	@Override
	public boolean holdUseItem(){
		return useItemDepressed;
	}

	public ControlsOutput withNone(){
		this.withBoost(false).withSlide(false).withJump(false).withSteer(0).withPitch(0).withRoll(0).withYaw(0)
				.withThrottle(0).withUseItem(false);
		return this;
	}

	public ControlsOutput withPitchYawRoll(double pitch, double yaw, double roll){
		this.pitch = clamp(pitch);
		this.yaw = clamp(yaw);
		this.roll = clamp(roll);
		return this;
	}

	public ControlsOutput withPitchYawRoll(double[] angles){
		return this.withPitchYawRoll(angles[0], angles[1], angles[2]);
	}

	private static final Random random = new Random();

	public static ControlsOutput random(){
		return new ControlsOutput(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1,
				random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextBoolean(), random.nextBoolean(),
				random.nextBoolean(), random.nextBoolean());
	}

}
