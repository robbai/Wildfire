package wildfire.wildfire.curve;

import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Constants;

/**
 * https://github.com/samuelpmish/RLUtilities/blob/7c645db1c7450ee793510c3acbb8bc61f8825b74/src/simulation/composite_arc.cc
 */
public class CompositeArc extends Curve {
	
	private static double[] signs = new double[] {1, -1};

	private Vector2 p1, p2, t1, t2, n1, n2, o1, o2, q1, q2;
	private double length, r1, r2, phi1, phi2;
	
	private double[] L = new double[5];

	private CompositeArc(double L0, Vector2 p1, Vector2 t1, double r1, double L4, Vector2 p2, Vector2 t2, double r2){
		this.p1 = p1.plus(t1.scaledToMagnitude(L0));
		this.t1 = t1.normalized();
		this.n1 = this.t1.cross();
		this.r1 = r1;
		this.o1 = this.p1.plus(n1.scaledToMagnitude(r1));

		this.p2 = p2.minus(t2.scaledToMagnitude(L4));
		this.t2 = t2.normalized();
		this.n2 = this.t2.cross();
		this.r2 = r2;
		this.o2 = this.p2.plus(n2.scaledToMagnitude(r2));

		Vector2 oDelta = o2.minus(o1);

		double sign = -Math.signum(r1) * Math.signum(r2);
		double R = Math.abs(r1) + sign * Math.abs(r2);
		double o1o2 = oDelta.magnitude();
		
		double beta = 0.97D;
		if((Math.pow(R, 2) / Math.pow(o1o2, 2)) > beta){
			Vector2 pDelta = this.p2.minus(this.p1);
			Vector2 nDelta = n2.scaledToMagnitude(r2).minus(n1.scaledToMagnitude(r1));

			double a = beta * nDelta.dotProduct(nDelta) - Math.pow(R, 2);
			double b = 2D * beta * nDelta.dotProduct(pDelta);
			double c = beta * pDelta.dotProduct(pDelta);

			double alpha = (-b - Math.sqrt(Math.pow(b, 2) - 4D * a * c)) / (2D * a);

			this.r1 *= alpha;
			this.r2 *= alpha;
			R *= alpha;

			o1 = this.p1.plus(n1.scaledToMagnitude(this.r1));
			o2 = this.p2.plus(n2.scaledToMagnitude(this.r2));

			oDelta = o2.minus(o1);
			o1o2 = oDelta.magnitude();
		}

		Vector2 e1 = oDelta.normalized();
		Vector2 e2 = e1.cross().scaled(-Math.signum(this.r1));

		double H = Math.sqrt(Math.pow(o1o2, 2) - Math.pow(R, 2));

		q1 = o1.plus((e1.scaled(R / o1o2).plus(e2.scaled(H / o1o2))).scaled(Math.abs(this.r1)));
		q2 = o2.minus((e1.scaled(R / o1o2).plus(e2.scaled(H / o1o2)).scaled(Math.abs(this.r2) * sign)));

		Vector2 pq1 = q1.minus(this.p1).normalized();
		phi1 = 2D * Math.signum(pq1.dotProduct(this.t1)) * Math.asin(Math.abs(pq1.dotProduct(n1)));
		if(phi1 < 0) phi1 += 2D * Math.PI;

		Vector2 pq2 = q2.minus(this.p2).normalized();
		phi2 = -2D * Math.signum(pq2.dotProduct(this.t2)) * Math.asin(Math.abs(pq2.dotProduct(n2)));
		if(phi2 < 0) phi2 += 2D * Math.PI;

		L[0] = L0;
		L[1] = phi1 * Math.abs(this.r1);
		L[2] = q1.distance(q2);
		L[3] = phi2 * Math.abs(this.r2);
		L[4] = L4;
		length = L[0] + L[1] + L[2] + L[3] + L[4];
	}
	
	public static CompositeArc create(CarData car, Vector2 ball, Vector2 goal, double finalVelocity, double L0, double L4){
		// Sanitise.
		L0 = Math.max(1, Math.abs(L0));
		L4 = Math.max(1, Math.abs(L4));
		
		Vector2 carDirection = car.orientation.noseVector.flatten(), carPosition = car.position.flatten();
		Vector2 goalDirection = goal.minus(ball).normalized();
		double playerTurnRadius = DrivePhysics.getTurnRadius(Math.max(Constants.MAX_THROTTLE_VELOCITY, car.forwardVelocityAbs)), ballTurnRadius = DrivePhysics.getTurnRadius(finalVelocity);
		
		// Find the shortest composite-arc based on its length.
		CompositeArc shortestCompositeArc = null;
		for(double playerTurn : signs){
			for(double ballTurn : signs){
				CompositeArc compositeArc;
				try{
					compositeArc = new CompositeArc(L0, carPosition, carDirection, playerTurn * playerTurnRadius, L4, ball, goalDirection, ballTurn * ballTurnRadius);
				}catch(Exception e){
					compositeArc = null;
					e.printStackTrace();
				}
				
				if(compositeArc != null && (shortestCompositeArc == null || compositeArc.length < shortestCompositeArc.length)){
					shortestCompositeArc = compositeArc;
				}
			}
		}
		
		return shortestCompositeArc;
	}
	
	public static CompositeArc create(CarData car, Vector2 ball, Vector2 goal, double L0, double L4){
		return create(car, ball, goal, DrivePhysics.maxVelocity(car.forwardVelocityAbs, car.boost), L0, L4);
	}
	
	@Override
	public Vector2[] discretise(int n){
		Vector2 r;
		Pair<Vector2, Vector2> Q; // Matrix 2x2

		double ds = (this.length / n);

		int[] segments = new int[5];
		int capacity = 1;
		for(int i = 0; i < 5; i++){
			segments[i] = (int)Math.ceil(this.L[i] / ds);
			capacity += segments[i];
		}

		Vector2[] points = new Vector2[capacity];

		int id = 0;

		Vector2 m1 = p1.minus(t1.scaledToMagnitude(L[0]));
		Vector2 m2 = p2.plus(t2.scaledToMagnitude(L[4]));

		ds = L[0] / segments[0];
		for(int i = 0; i < segments[0]; i++){
			points[id] = m1.lerp(p1, (double)i / segments[0]);
			id++;
		}

		ds = L[1] / segments[1];
		r = p1.minus(o1);
		Q = rotation(Math.signum(r1) * phi1 / segments[1]);
		for(int i = 0; i < segments[1]; i++){
			points[id] = o1.plus(r);
			id++;
			r = dot(Q, r);
		}

		ds = L[2] / segments[2];
		for(int i = 0; i < segments[2]; i++){
			points[id] = q1.lerp(q2, (double)i / segments[2]);
			id++;
		}

		ds = L[3] / segments[3];
		r = q2.minus(o2);
		Q = rotation(Math.signum(r2) * phi2 / segments[3]);
		for(int i = 0; i < segments[3]; i++){
			points[id] = o2.plus(r);
			id++;
			r = dot(Q, r);
		}

		ds = L[4] / segments[4];
		for(int i = 0; i <= segments[4]; i++){
			points[id] = p2.lerp(m2, (double)i / segments[4]);
			id++;
		}

		return points;
	}
	
	/**
	 * Matrix 2x2
	 */
	private static Pair<Vector2, Vector2> rotation(double theta){
		return new Pair<Vector2, Vector2>(
				new Vector2(Math.cos(theta), -Math.sin(theta)),
				new Vector2(Math.sin(theta), Math.cos(theta))
				);
	}
	
	/**
	 * Matrix 2x2 dot product.
	 */
	private static Vector2 dot(Pair<Vector2, Vector2> mat, Vector2 vec){
		return new Vector2(mat.getOne().dotProduct(vec), mat.getTwo().dotProduct(vec));
	}

	// TODO
	@Override
	public Vector2 T(double t){
		return null;
	}

	@Override
	public double getLength(){
		return this.length;
	}

	public double getL(int i){
		return L[i];
	}

	public double getR1(){
		return r1;
	}

	public double getR2(){
		return r2;
	}
	
	public double minTravelTime(CarData car, boolean includeL0, boolean includeL4){
		double velocity = Math.max(car.forwardVelocity, 0), boost = car.boost;

		double firstArcFinalVel = DrivePhysics.getSpeedFromRadius(this.getR1()),
				firstArcAccTime = DrivePhysics.timeToReachVel(velocity, car.boost, firstArcFinalVel);

		double traversed = DrivePhysics.maxDistance(firstArcAccTime, velocity, car.boost);
		velocity = firstArcFinalVel;
		boost -= (Constants.BOOST_RATE * firstArcAccTime);
		double time = firstArcAccTime;

		double lengthL0 = (includeL0 ? this.getL(0) : 0);
		if(traversed < lengthL0 + this.getL(1)){
			time += (lengthL0 + this.getL(1) - traversed) / velocity;
			traversed = (lengthL0 + this.getL(1));
		}

		double secondArcMaxVel = DrivePhysics.getSpeedFromRadius(this.getR2());
		
		double distanceToTravel = (this.getLength() - (includeL0 ? 0 : this.getL(0)) - (includeL4 ? 0 : this.getL(4)));
		double straightawayTime = (includeL4 ? DrivePhysics.minTravelTime(velocity, boost, distanceToTravel - traversed, secondArcMaxVel) : 0);

		return time + straightawayTime;
	}
	
}