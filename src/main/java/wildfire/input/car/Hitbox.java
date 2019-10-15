package wildfire.input.car;

import wildfire.input.shapes.BoxShape;
import wildfire.input.shapes.SphereShape;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.utils.Utils;

public class Hitbox extends BoxShape {

	/*
	 * https://discordapp.com/channels/348658686962696195/348661571297214465/617774276879056919
	 */

	private static final double[] lengths = new double[] {118.01, 127.93, 128.82, 131.49, 127.02};
	private static final Vector3[] offsets = new Vector3[] {new Vector3(0, 13.88, 20.75), new Vector3(0, 9.00, 15.75), new Vector3(0, 9.01, 12.09), new Vector3(0, 12.50, 11.75), new Vector3(0, 13.88, 20.75)};
	private static final double[] restingHeights = new double[] {17.00, 17.05, 18.65, 18.33, 17.01};

	public final double restingHeight;
	public final Vector3 offset;

	private final CarOrientation orientation;

	public Hitbox(CarData car, rlbot.flat.BoxShape boxShape){
		super(car.position, boxShape);

		this.orientation = car.orientation;

		Pair<Vector3, Double> hitbox = identify(length);
		this.offset = hitbox.getOne();
		this.restingHeight = hitbox.getTwo();
	}

	public Hitbox(Hitbox hitbox, Vector3 position, CarOrientation orientation){
		super(position, hitbox);
		this.orientation = new CarOrientation(orientation);
		this.offset = new Vector3(hitbox.offset);
		this.restingHeight = hitbox.restingHeight;
	}

	public Hitbox(CarData car, Vector3 shape, Vector3 offset, double restingHeight){
		super(car.position, shape);
		this.orientation = new CarOrientation(car.orientation);
		this.offset = new Vector3(offset);
		this.restingHeight = restingHeight;
	}

	/**
	 * We find the hitbox with the closest length to that provided by the framework
	 * in order to identify which hitbox offset belongs to us.
	 */
	private static Pair<Vector3, Double> identify(double inputLength){
		int closest = -1;
		double smallestDifference = 0;

		for(int i = 0; i < lengths.length; i++){
			double difference = Math.abs(inputLength - lengths[i]);
			if(closest == -1 || difference < smallestDifference){
				closest = i;
				smallestDifference = difference;
			}
		}

		return new Pair<Vector3, Double>(offsets[closest], restingHeights[closest]);
	}

	/*
	 * https://samuelpmish.github.io/notes/RocketLeague/car_ball_interaction/#hitboxes-and-collision-detection
	 */
	public boolean intersects(SphereShape sphere){
		Vector3 p = nearestPoint(sphere.position);
		return (p.distance(sphere.position) <= sphere.radius);
	}

	private Vector3 nearestPoint(Vector3 vec){
		// Transform to local coordinates.
		Vector3 local = Utils.toLocal(this.position, this.orientation, vec);
		
		local = local.minus(this.offset);

		// Clamp the local coordinates.
		Vector3 closestLocal = new Vector3(
				Utils.clamp(local.x, -this.width / 2, this.width / 2),
				Utils.clamp(local.y, -this.length / 2, this.length / 2),
				Utils.clamp(local.z, -this.height / 2, this.height / 2)
				);
		
//		local = local.minus(this.offset);

		// Transform back to global coordinates.
		return Utils.toGlobal(this.position, this.orientation, closestLocal);
	}

}
