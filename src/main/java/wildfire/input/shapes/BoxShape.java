package wildfire.input.shapes;

import wildfire.input.car.Hitbox;
import wildfire.vector.Vector3;

public class BoxShape {
	
	public final Vector3 position;
	
	public final double length, width, height;
	
	/**
	 * Width, length, height
	 */
	public final Vector3 shape;

	public BoxShape(Vector3 position, double length, double width, double height){
		this.position = position;
		
		this.length = length;
		this.width = width;
		this.height = height;
		
		this.shape = new Vector3(width, length, height);
	}

	public BoxShape(Vector3 position){
		this(position, 0, 0, 0);
	}

	public BoxShape(Vector3 position, rlbot.flat.BoxShape boxShape){
		this(position, boxShape.length(), boxShape.width(), boxShape.height());
	}

	public BoxShape(Vector3 position, Hitbox hitbox){
		this(position, hitbox.length, hitbox.width, hitbox.height);
	}

}
