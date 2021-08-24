package wildfire.wildfire.obj;

import wildfire.vector.Vector2;

public class Circle {

	private final Vector2 centre;
	private final double radius;

	public Circle(Vector2 centre, double radius){
		super();
		this.centre = centre;
		this.radius = Math.abs(radius);
	}

	public Vector2 getCentre(){
		return centre;
	}

	public double getRadius(){
		return radius;
	}

	/*
	 * http://www.ambrsoft.com/TrigoCalc/Circles2/CirclePoint/CirclePointDistance.
	 * htm
	 */
	public Pair<Vector2, Vector2> calculateTangents(Vector2 p){
		double radiusSq = Math.pow(this.radius, 2);
		double divisor = (Math.pow(p.x - centre.x, 2) + Math.pow(p.y - centre.y, 2));
		double root = Math.sqrt(Math.pow(p.x - centre.x, 2) + Math.pow(p.y - centre.y, 2) - radiusSq);

		// Calculate the tangents.
		Pair<Vector2, Vector2> tangents = new Pair<Vector2, Vector2>(
				new Vector2((radiusSq * (p.x - centre.x) + radius * (p.y - centre.y) * root) / divisor + centre.x,
						(radiusSq * (p.y - centre.y) - radius * (p.x - centre.x) * root) / divisor + centre.y),
				new Vector2((radiusSq * (p.x - centre.x) - radius * (p.y - centre.y) * root) / divisor + centre.x,
						(radiusSq * (p.y - centre.y) + radius * (p.x - centre.x) * root) / divisor + centre.y));

		return tangents;
	}

	public double getCircumference(){
		return this.radius * 2 * Math.PI;
	}

	public double getSectorCircumference(double radians){
		return this.getCircumference() * radians / (2 * Math.PI);
	}

}
