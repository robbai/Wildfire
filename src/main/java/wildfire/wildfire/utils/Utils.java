package wildfire.wildfire.utils;

import wildfire.input.CarData;
import wildfire.input.CarOrientation;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Action;
import wildfire.wildfire.obj.Pair;

public class Utils {
	
	/*
	 * Flip the aim to face backwards
	 */
	public static double invertAim(double aim){
		return aim != 0 ? aim + -Math.signum(aim) * Math.PI : Math.PI;
	}
	
	public static int teamSign(int team){
		return (team == 0 ? 1 : -1);
	}
	public static int teamSign(CarData car){
		return teamSign(car.team);
	}

	public static float round(float value){
		return Math.round(value * 100F) / 100F;
	}
	
	public static double round(double value){
		return Math.round(value * 100D) / 100D;
	}
	
	public static void transferAction(Action one, Action two){
		one.state.currentAction = two;
	}
	
	public static boolean isPointWithinRange(Vector2 point, Vector2 target, double minRange, double maxRange){
		double range = point.distance(target);
		return range < maxRange && range > minRange;
	}

	public static double wrapAngle(double d){
		if(d < -Math.PI) d += 2 * Math.PI;
		if(d > Math.PI) d -= 2 * Math.PI;
		return d;
	}
	
	public static double clampSign(double value){
		return clamp(value, -1, 1);
	}

	public static double clamp(double value, double min, double max){
		return Math.max(Math.min(min, max), Math.min(Math.max(min, max), value));
	}
	
	public static double distanceToWall(Vector3 position){
		return Math.min(Constants.PITCHWIDTH - Math.abs(position.x), Math.abs(position.y) > Constants.PITCHLENGTH ? 0 : Constants.PITCHLENGTH - Math.abs(position.y));
	}
	
	public static Vector2 traceToX(Vector2 start, Vector2 direction, double x){
		//The direction is pointing away from the X
		if(Math.signum(direction.x) != Math.signum(x - start.x)) return null;

		//Scale up the direction so the X meets the requirement
		double xChange = x - start.x;
		direction = direction.normalized();
		direction = direction.scaled(Math.abs(xChange / direction.x));
		
		return start.plus(direction);
	}
	
	public static Vector2 traceToY(Vector2 start, Vector2 direction, double y){
		//The direction is pointing away from the X
		if(Math.signum(direction.y) != Math.signum(y - start.y)) return null;

		//Scale up the direction so the X meets the requirement
		double yChange = y - start.y;
		direction = direction.normalized();
		direction = direction.scaled(Math.abs(yChange / direction.y));
		
		return start.plus(direction);
	}
	
	public static Vector2 traceToWall(Vector2 start, Vector2 direction){
		Vector2 trace;
		
		if(!direction.isZero()){
			trace = traceToX(start, direction, Math.signum(direction.x) * Constants.PITCHWIDTH);
			if(trace != null && Math.abs(trace.y) <= Constants.PITCHLENGTH) return trace;
			
			trace = traceToY(start, direction, Math.signum(direction.y) * Constants.PITCHLENGTH);
			if(trace != null && Math.abs(trace.x) <= Constants.PITCHWIDTH) return trace;
		}
		
		//Direction is zero or the start is outside of the map
		return start.confine();
	}
	
	/*
	 *  https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
	 */
	public static double distanceFromLine(Vector2 point, Vector2 lineA, Vector2 lineB){
		return Math.abs(point.x * (lineB.y - lineA.y) - point.y * (lineB.x - lineA.x) + lineB.x * lineA.y - lineB.y * lineA.x) / Math.sqrt(Math.pow(lineB.y - lineA.y, 2) + Math.pow(lineB.x - lineA.x, 2));
	}
	
	public static Vector3 toLocalFromRelative(CarData car, Vector3 relative){
		CarOrientation carOrientation = car.orientation;
		
		double localRight = carOrientation.rightVector.x * relative.x + carOrientation.rightVector.y * relative.y + carOrientation.rightVector.z * relative.z;
		double localNose = carOrientation.noseVector.x * relative.x + carOrientation.noseVector.y * relative.y + carOrientation.noseVector.z * relative.z;
		double localRoof = carOrientation.roofVector.x * relative.x + carOrientation.roofVector.y * relative.y + carOrientation.roofVector.z * relative.z;
		
		return new Vector3(localRight, localNose, localRoof);
	}
	
	public static Vector3 toLocal(CarData car, Vector3 vec){
		return toLocalFromRelative(car, vec.minus(car.position));
	}
	
	public static double lerp(double a, double b, double f){
		return a + f * (b - a);
	}
	
	/**
	 * https://math.stackexchange.com/a/3128850
	 */
	public static Pair<Double, Double> closestPointToLineSegment(Vector2 P, Pair<Vector2, Vector2> lineSegment){
		Vector2 A = lineSegment.getOne(), B = lineSegment.getTwo();
		
	    Vector2 v = B.minus(A);
	    Vector2 u = A.minus(P);
	    
	    double vu = v.dotProduct(u);
	    double vv = v.dotProduct(v);
	    
	    double t = -vu / vv;
	    		
	    if(t >= 0 && t <= 1) return new Pair<Double, Double>(P.distance(A.lerp(B, t)), t);
	    
	    double distA = P.distance(A), distB = P.distance(B);
	    return new Pair<Double, Double>(Math.min(distA, distB), Utils.clamp(t, 0, 1));
	}
	
	public static double pointLineSegmentT(Vector3 P, Pair<Vector3, Vector3> lineSegment){
		return closestPointToLineSegment(P.flatten(), new Pair<Vector2, Vector2>(lineSegment.getOne().flatten(), lineSegment.getTwo().flatten())).getTwo();
	}

	public static Vector3 toGlobal(CarData car, Vector3 localTarget){
		Vector3 carPosition = car.position;
		CarOrientation carOrientation = car.orientation;
		return carPosition.plus(carOrientation.rightVector.scaledToMagnitude(localTarget.x)).plus(carOrientation.noseVector.scaledToMagnitude(localTarget.y)).plus(carOrientation.roofVector.scaledToMagnitude(localTarget.z));
	}

}
