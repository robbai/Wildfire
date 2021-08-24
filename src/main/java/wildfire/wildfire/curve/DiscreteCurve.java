package wildfire.wildfire.curve;

import java.awt.Color;
import java.util.OptionalDouble;

import wildfire.vector.Vector2;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

/**
 * https://samuelpmish.github.io/notes/RocketLeague/path_analysis/
 */

public class DiscreteCurve {

	public static final int analysePoints = 80, polyRender = 1;
	public static final double curveStep = (1D / 45);
	private static final double[] brakeCurve = formAccCurve(false);

	private final Vector2[] points;
	private final double initialVelocity, distance, boost, time;
	private final OptionalDouble timeRestriction;
	private final boolean valid, unlimitedBoost;
	private final double[] turningRadii, speeds, accelerations;

	public DiscreteCurve(double initialVelocity, double boost, Vector2[] points, OptionalDouble timeRestriction){
		super();
		this.points = points;
		this.valid = !this.invalidCurve();
		this.timeRestriction = timeRestriction;
		this.initialVelocity = Math.abs(initialVelocity);
		this.unlimitedBoost = (boost <= -1);
		this.boost = Utils.clamp(boost, 0, 100);
		this.distance = distance(this.points);
		this.turningRadii = (this.valid ? this.calculateTurningRadii() : null); // For each point.
		Pair<double[][], Double> result = (this.valid ? this.calculateSpeed() : null);
		this.speeds = (this.valid ? result.getOne()[0] : null); // Interpolated.
		this.accelerations = (this.valid ? result.getOne()[1] : null);
		this.time = (this.valid ? result.getTwo() : 0);
	}

	public DiscreteCurve(double initialVelocity, double boost, Vector2[] points){
		this(initialVelocity, boost, points, OptionalDouble.empty());
	}

	public DiscreteCurve(double initialVelocity, double boost, Curve curve){
		this(initialVelocity, boost, curve.discretise(analysePoints), OptionalDouble.empty());
	}

	public DiscreteCurve(double initialVelocity, double boost, Curve curve, double timeRestriction){
		this(initialVelocity, boost, curve.discretise(analysePoints), OptionalDouble.of(timeRestriction));
	}

	private Pair<double[][], Double> calculateSpeed(){
		if(this.turningRadii == null)
			return null;

		// Find the non-acceleration-limited form.
		double[] optimalSpeeds = new double[this.turningRadii.length];
		for(int i = 0; i < optimalSpeeds.length; i++){
			double radius = this.turningRadii[i];
			optimalSpeeds[i] = DrivePhysics.getSpeedFromRadius(radius);
		}

		// Apply acceleration limits.
		double[] speeds = new double[analysePoints], accelerations = new double[analysePoints];
		double s = 0, time = 0, v = this.initialVelocity, boost = this.boost;
		speeds[0] = v;
//		accelerations[0] = 0D;
		while(s < this.distance){
			double index = this.indexS(s);

			// Slide the braking curve.
			boolean brake = false;
			int brakeIncrement = 20;
			for(int brakeVelocity = (int)Math.floor(v); brakeVelocity >= 0; brakeVelocity -= brakeIncrement){
//			for(int brakeVelocity = 0; brakeVelocity <= Math.floor(v); brakeVelocity += brakeIncrement){
				double brakeDistance = (s + brakeCurve[brakeVelocity]);
				if(brakeDistance > this.distance)
					break;

				double brakeIndex = this.indexS(brakeDistance);
				if(Utils.lerp(optimalSpeeds[(int)Math.floor(brakeIndex)], optimalSpeeds[(int)Math.ceil(brakeIndex)],
						brakeIndex - Math.floor(brakeIndex))
//				if(Math.max(optimalSpeeds[(int)Math.floor(brakeIndex)], optimalSpeeds[(int)Math.ceil(brakeIndex)])
						< brakeVelocity){
					brake = true;
					break; // Brake - Kappa.
				}
			}

			// Create the realistic curve.
			double a;
			if(brake){
				// Brake.
				a = DrivePhysics.determineAcceleration(v, -1, false);
			}else{
				// Throttle and/or boost.
				double optimalSpeed = Utils.lerp(optimalSpeeds[(int)Math.floor(index)],
						optimalSpeeds[(int)Math.ceil(index)], index - Math.floor(index));
				boolean useBoost = ((this.unlimitedBoost || boost >= 1)
						&& (optimalSpeed - v) > Constants.BOOST_GROUND_ACCELERATION * Math.pow(curveStep, 2));
				a = DrivePhysics.determineAcceleration(v, 1, useBoost);
				if(useBoost)
					boost -= (100D / 3) * curveStep; // Consume boost.
				if(!useBoost && v + a * curveStep > optimalSpeed)
					a = (optimalSpeed - v);
			}

			v = Utils.clamp(v + a * curveStep, 1, Constants.MAX_CAR_VELOCITY);
			s += v * curveStep;

			int i = (int)Utils.clamp((double)(analysePoints - 1) * s / this.distance, 1, analysePoints - 1);
			speeds[i] = v;
			accelerations[i - 1] = a;

			time += curveStep;
			if(timeRestriction.isPresent() && time > timeRestriction.getAsDouble())
				break;
		}

//		System.out.println(Arrays.toString(accelerations));

		return new Pair<double[][], Double>(new double[][] { speeds, accelerations }, time);
	}

	private double[] calculateTurningRadii(){
		if(this.invalidCurve())
			return null;

		double[] k = new double[this.points.length];
		for(int i = 1; i < k.length - 1; i++){
			Vector2 A = this.points[i - 1], B = this.points[i], C = this.points[i + 1];

			if(B.minus(A).normalised().dotProduct(C.minus(B).normalised()) > 0.99999){
				k[i] = DrivePhysics.getTurnRadius(Constants.MAX_CAR_VELOCITY);
				continue;
			}

			double a = A.distance(B), b = B.distance(C), c = C.distance(A);

			double p = (a + b + c) / 2D;
			double areaSquared = (p * (p - a) * (p - b) * (p - c));

//			if(areaSquared < 0){
//				k[i] = DrivePhysics.getTurnRadius(Constants.MAX_CAR_VELOCITY);
//				continue;
//			}

			double area = Math.sqrt(areaSquared);
			double radius = (a * b * c) / (4D * area);
			k[i] = radius;

//			if(DrivePhysics.getSpeedFromRadius(radius) < 1) System.out.println(i + ": " + (int)radius + " (" + area + ")");
		}
		k[0] = k[1];
		k[k.length - 1] = k[k.length - 2];

		return k;
	}

	public double getDistance(){
		return this.distance;
	}

	public Vector2[] getPoints(){
		return this.points;
	}

	private static double distance(Vector2[] points){
		if(invalid(points))
			return 0;

		double distance = 0;
		for(int i = 1; i < points.length; i++){
			Vector2 a = points[i - 1], b = points[i];
			distance += a.distance(b);
		}
		return distance;
	}

	public Vector2 T(double t){
		if(t < 0 || t > 1)
			return null;
		return S(t * this.distance);
	}

	public Vector2 S(double s){
		if(this.invalid())
			return null;
//		if(s > this.distance || s < 0) return null;
		s = Utils.clamp(s, 0, this.distance);

		Vector2 a = null, b = null;
		double segment = 0, totalS = 0;
		for(int i = 1; i < points.length; i++){
			a = points[i - 1];
			b = points[i];

			segment = a.distance(b);
			totalS += segment;
			if(totalS > s)
				break;
		}

		return b.plus(a.minus(b).scaledToMagnitude(totalS - s));
	}

	public double indexS(double s){
		if(this.invalid())
			return -1;
		if(s > this.distance || s < 0)
			return -1;

		Vector2 a = null, b = null;
		double segment = 0, totalS = 0;
		for(int i = 1; i < points.length; i++){
			a = points[i - 1];
			b = points[i];

			segment = a.distance(b);
			totalS += segment;
			if(totalS > s)
				return i - (totalS - s) / segment;
		}

		return -1;
	}

	private boolean invalid(){
		return invalid(this.points);
	}

	private boolean invalidCurve(){
		return this.points.length < 3;
	}

	private static boolean invalid(Vector2[] points){
		return points.length < 2;
	}

	/**
	 * https://samuelpmish.github.io/notes/RocketLeague/path_analysis/#a-better-approximation-for-vs
	 *
	 * @param forwardVelocity Forward velocity
	 * @param boostNotBrake   True for boosting, false for braking
	 * @return Displacement for all 2300 velocities
	 */
	private static double[] formAccCurve(boolean boostNotBrake){
		double v = (boostNotBrake ? 0 : Constants.MAX_CAR_VELOCITY);
		double s = 0;

		double[] curve = new double[(int)Constants.MAX_CAR_VELOCITY + 1];
		while(true){
			double a = DrivePhysics.determineAcceleration(v, boostNotBrake ? 1 : -1, boostNotBrake);
			double step = (1D / Math.abs(a));
			v = Utils.clamp(v + a * step, 0, Constants.MAX_CAR_VELOCITY);
			s += v * step;

			curve[(int)v] = Math.min(s, curve[(int)v] == 0 ? Double.MAX_VALUE : curve[(int)v]);

			if(boostNotBrake ? v >= Constants.MAX_CAR_VELOCITY : v <= 0)
				break;
		}
//		System.out.println(Arrays.toString(curve));

		return curve;
	}

	public double getInitialVelocity(){
		return this.initialVelocity;
	}

	public double getSpeed(double t){
		if(this.invalidCurve())
			return Constants.MAX_CAR_VELOCITY;
		double speedIndex = Utils.clamp(t * (this.speeds.length - 1), 0, this.speeds.length - 1);
		return Utils.lerp(this.speeds[(int)Math.floor(speedIndex)], this.speeds[(int)Math.ceil(speedIndex)],
				speedIndex - Math.floor(speedIndex));
	}

	public double getSpeedS(double s){
		return getSpeed(s / this.getDistance());
	}

	public double getAcceleration(double t){
		if(this.invalidCurve())
			return Constants.MAX_CAR_VELOCITY;
		double accIndex = Utils.clamp(t * (double)(this.speeds.length - 1), 0, this.speeds.length - 1);
		return Utils.lerp(this.accelerations[(int)Math.floor(accIndex)], this.accelerations[(int)Math.ceil(accIndex)],
				accIndex - Math.floor(accIndex));
//		return Math.max(this.accelerations[(int)Math.floor(accIndex)], this.accelerations[(int)Math.ceil(accIndex)]);
//		return this.accelerations[(int)Math.ceil(accIndex)];
	}

	public void render(WRenderer renderer, Color colour){
		if(this.invalid())
			return;
		for(int i = 0; i < (points.length - 1); i += polyRender){
			Vector2 a = points[i], b = points[Math.min(points.length - 1, i + polyRender)];
			renderer.drawLine3d(colour, a, b);
		}
	}

	public double getTime(){
		return this.time;
	}

	public boolean isValid(){
		return valid;
	}

	public double findClosestS(Vector2 car, boolean returnDistance){
		double closestS = 0;
		double closestDistance = -1;

		double s = 0;
		for(int i = 0; i < (this.points.length - 1); i++){
			Pair<Vector2, Vector2> lineSegment = new Pair<Vector2, Vector2>(this.points[i], this.points[i + 1]);
			double segmentLength = lineSegment.getOne().distance(lineSegment.getTwo());

			Pair<Double, Double> result = Utils.closestPointToLineSegment(car, lineSegment);
			double distance = result.getOne(), t = result.getTwo();

			if(closestDistance == -1 || closestDistance > distance){
				closestDistance = distance;
				closestS = s + segmentLength * t;
			}
			s += segmentLength;
		}

		return (returnDistance ? closestDistance : closestS);
	}

	public Vector2 getDestination(){
		return this.points[this.points.length - 1];
	}

}
