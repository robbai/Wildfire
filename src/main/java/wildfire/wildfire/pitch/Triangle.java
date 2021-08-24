package wildfire.wildfire.pitch;

import java.util.Arrays;

import wildfire.vector.Vector3;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ReadableObj;

public class Triangle implements Comparable<Triangle> {

	/**
	 * Small value (avoid floating point mess)
	 */
	private static final double EPSILON = 0.0000001;

	private final Vector3[] vectors;

	public Triangle(ReadableObj obj, ObjFace face){
		vectors = new Vector3[3];
		for(int v = 0; v < 3; v++)
			vectors[v] = Pitch.toVector(obj.getVertex(face.getVertexIndex(v)));
	}

	public Vector3 getVector(int index){
		return vectors[index];
	}

	@Override
	public String toString(){
		return "Triangle " + Arrays.toString(vectors);
	}

	/*
	 * https://en.wikipedia.org/wiki/M�ller�Trumbore_intersection_algorithm
	 */
	public boolean rayIntersects(Vector3 rayOrigin, Vector3 rayVector){
		Vector3 edge1 = this.vectors[1].minus(this.vectors[0]);
		Vector3 edge2 = this.vectors[2].minus(this.vectors[0]);
		Vector3 h = rayVector.crossProduct(edge2);
		double a = edge1.dotProduct(h);

		// This ray is parallel to this triangle.
		if(Math.abs(a) < EPSILON)
			return false;

		Vector3 s = rayOrigin.minus(this.vectors[0]);
		double u = s.dotProduct(h) / a;
		if(u < 0 || u > 1)
			return false;

		Vector3 q = s.crossProduct(edge1);
		double v = rayVector.dotProduct(q) / a;
		if(v < 0 || u + v > 1)
			return false;

		// At this stage we can compute t to find out where the intersection point is on
		// the line.
		double t = edge2.dotProduct(q) / a;

		// Ray intersection
		if(t > EPSILON)
			return true;

		// This means that there is a line intersection but not a ray intersection.
		return false;
	}

	/**
	 * Calculate the normal
	 *
	 * @return The normal of the triangle
	 */
	public Vector3 getNormal(){
		Vector3 v = this.vectors[1].minus(this.vectors[0]);
		Vector3 w = this.vectors[2].minus(this.vectors[0]);
		return v.crossProduct(w).normalised();
	}

	/**
	 * Calculate the centre
	 *
	 * @return The centre of the triangle
	 */
	public Vector3 getCentre(){
		return this.vectors[0].plus(this.vectors[1]).plus(this.vectors[2]).scaled(1D / 3);
	}

	/*
	 * http://geomalgorithms.com/a05-_intersect-1.html
	 */
	public Vector3 segmentIntersects(Vector3 segment1, Vector3 segment2){
		Vector3 pn = this.getNormal();
		Vector3 u = segment2.minus(segment1);
		Vector3 w = segment1.minus(this.vectors[0]);

		double D = pn.dotProduct(u);
		double N = -pn.dotProduct(w);

		// Segment is parallel to plane.
		if(D == 0){
			// Segment lies in plane.
			if(N == 0)
				return vectorInside(segment1) ? segment1 : null;

			// No intersection.
			return null;
		}

		// Compute intersect.
		double sI = N / D;
		if(sI < 0 || sI > 1)
			return null; // No intersection.

		// compute segment intersect point
		Vector3 I = segment1.plus(u.scaled(sI));
		return vectorInside(I) ? I : null;
	}

	/*
	 * http://blackpawn.com/texts/pointinpoly/default.html
	 */
	private boolean sameSide(Vector3 point1, Vector3 point2, int sideIndex1, int sideIndex2){
		Vector3 a = this.vectors[sideIndex1];
		Vector3 b = this.vectors[sideIndex2];

		Vector3 cp1 = b.minus(a).crossProduct(point1.minus(a));
		Vector3 cp2 = b.minus(a).crossProduct(point2.minus(a));

		return cp1.dotProduct(cp2) >= 0;
	}

	/*
	 * http://blackpawn.com/texts/pointinpoly/default.html
	 */
	private boolean vectorInside(Vector3 point){
		return (sameSide(point, this.vectors[0], 1, 2) && sameSide(point, this.vectors[1], 0, 2)
				&& sameSide(point, this.vectors[2], 0, 1));
	}

	public double getArea(){
		double a = this.vectors[0].distance(this.vectors[1]);
		double b = this.vectors[1].distance(this.vectors[2]);
		double c = this.vectors[2].distance(this.vectors[0]);

		double s = (a + b + c) / 2;
		return Math.sqrt(s * (s - a) * (s - b) * (s - c));
	}

	@Override
	public int compareTo(Triangle other){
		return Double.compare(this.getArea(), other.getArea());
	}

}
