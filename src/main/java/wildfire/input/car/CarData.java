package wildfire.input.car;


import wildfire.input.Rotator;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class CarData {

	public final Vector3 position, velocity, angularVelocityAxis;
	public final CarOrientation orientation;
	public final Rotator angularVelocity;
	public final Hitbox hitbox;
	public final double boost, forwardVelocity, forwardVelocityAbs, sign;
	public final float elapsedSeconds;
	public final boolean hasWheelContact, isSupersonic, isDemolished, hasDoubleJumped, onFlatGround;
	public final int team, index;
	public final String name;

	public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds, int index){
		this.position = new Vector3(playerInfo.physics().location());
		this.velocity = new Vector3(playerInfo.physics().velocity());
		this.angularVelocityAxis = new Vector3(playerInfo.physics().angularVelocity());

		this.orientation = new CarOrientation(playerInfo.physics().rotation());

		this.angularVelocity = new Rotator(this.orientation, this.angularVelocityAxis);

		this.hitbox = new Hitbox(this, playerInfo.hitbox());

		this.boost = playerInfo.boost();

		this.elapsedSeconds = elapsedSeconds;

		this.hasWheelContact = playerInfo.hasWheelContact();
		this.isSupersonic = playerInfo.isSupersonic();
		this.isDemolished = playerInfo.isDemolished();
		this.hasDoubleJumped = playerInfo.doubleJumped();
		this.onFlatGround = (this.hasWheelContact && this.orientation.up.z > 0.8);

		this.team = playerInfo.team();
		this.index = index;

		this.name = playerInfo.name();

		this.forwardVelocity = this.velocity.dotProduct(this.orientation.forward);
		this.forwardVelocityAbs = Math.abs(this.forwardVelocity);
		this.sign = (this.team == 0 ? 1 : -1);
	}

	public double velocityDir(Vector2 direction){
		return this.velocity.dotProduct(direction.normalized().withZ(0));
	}

	public double velocityDir(Vector3 direction){
		return this.velocity.dotProduct(direction.normalized());
	}

}
