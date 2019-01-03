package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.State;

public class CircleState extends State {
	
	private final boolean fullSpeed = false;
	
	/*
	 * radius / length = tan(angle / 2)
	 * length = radius / tan(angle / 2)
	 */
	
	/*
	 * |tangent| = root(radius^2 + |distance|^2)
	 * tan(angle) = radius / |distance|
	 */

	public CircleState(Wildfire wildfire){
		super("Circle", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
//		return !Utils.isKickoff(input);
//		return false;
		Vector3 backward = new Vector3(0, -Utils.teamSign(input.car), 0);
		return backward.angle(input.car.position.minus(wildfire.impactPoint.getPosition())) > Math.PI * 0.4;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		if(Utils.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}else if(Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this);
			return currentAction.getOutput(input);
		}

		Vector2 goal = Utils.enemyGoal(input.car.team);
//		Vector2 a = input.car.position.plus(input.car.velocity.scaled(1D / 60)).flatten();
		Vector2 a = input.car.position.flatten();
		Vector2 c = wildfire.impactPoint.getPosition().flatten();

		//Tangent from the goal to the circle
		wildfire.renderer.drawLine3d(Color.ORANGE, goal.toFramework(), Utils.traceToWall(goal, c.minus(goal)).toFramework());

		//Circle
		double radius = Utils.getTurnRadius(fullSpeed ? Utils.boostMaxSpeed(input.car.forwardMagnitude(), input.car.boost) : Math.max(1410, input.car.forwardMagnitude()));
		boolean left = goal.minus(c).correctionAngle(goal.minus(a)) < 0;
		Vector2 circle = c.plus(c.minus(goal).rotate(Math.PI / 2).scaledToMagnitude(left ? -radius : radius));
		wildfire.renderer.drawCircle(Color.YELLOW, circle, radius);

		//Useful info
		double adjacent = a.distance(circle);
		boolean inCircle = (adjacent < radius);
		wildfire.renderer.drawString2d(inCircle ? "Inside Circle" : "Outside Circle", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Radius: " + (int)radius + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		wildfire.renderer.drawString2d("Adjacent: " + (int)adjacent + "uu", Color.WHITE, new Point(0, 60), 2, 2);
		
		double steer;
		if(Math.abs(adjacent - radius) < 200){
			wildfire.renderer.drawCircle(Color.GREEN, circle, radius);
			steer = -Math.signum(Utils.aim(input.car, c));
		}else if(inCircle){
			//Get back on the line of the circle
			Vector2 target = circle.plus(a.plus(input.car.orientation.noseVector.flatten().scaledToMagnitude(adjacent)).minus(circle).scaledToMagnitude(radius));
			wildfire.renderer.drawLine3d(Color.RED, circle.toFramework(), target.toFramework());
			
			steer = Utils.aim(input.car, target);
			steer *= -0.5;
		}else{
			Vector2[] tangents = calculateTangents(circle, radius, a);
			renderTangents(circle, radius, a, tangents);
			Vector2 tangent = (Math.abs(Utils.aim(input.car, tangents[0])) < Math.abs(Utils.aim(input.car, tangents[1])) ? tangents[0] : tangents[1]);
			
			steer = Utils.aim(input.car, tangent);
			steer *= -2;
		}
		
		//Controller
		return new ControlsOutput().withThrottle(1).withSteer((float)steer).withBoost(fullSpeed).withSlide(false);
	}
	
	private void renderTangents(Vector2 circle, double r, Vector2 point, Vector2[] tangents){
		for(Vector2 tangent : tangents){
			//Car to tangents
			wildfire.renderer.drawLine3d(Color.RED, point.toFramework(), tangent.toFramework());
			
			//Radii to tangents
			wildfire.renderer.drawLine3d(Color.RED, circle.toFramework(), tangent.toFramework());
		}
		
		//Car to circle
		wildfire.renderer.drawLine3d(Color.RED, point.toFramework(), circle.toFramework());
	}
	
	private Vector2 calculateTangent(Vector2 circle, double r, Vector2 point, boolean plus){
		double xp = point.x, yp = point.y;
		double a = circle.x, b = circle.y;
		
		double x = (r * r * (xp - a) + (plus ? 1 : -1) * r * (yp - b) * Math.sqrt((xp - a) * (xp - a) + (yp - b) * (yp - b) - r * r)) / ((xp - a) * (xp - a) + (yp - b) * (yp - b)) + a;
		double y = (r * r * (yp - b) + (plus ? -1 : 1) * r * (xp - a) * Math.sqrt((xp - a) * (xp - a) + (yp - b) * (yp - b) - r * r)) / ((xp - a) * (xp - a) + (yp - b) * (yp - b)) + b;
		return new Vector2(x, y);
	}
	
	private Vector2[] calculateTangents(Vector2 circle, double r, Vector2 point){
		return new Vector2[] {calculateTangent(circle, r, point, true), calculateTangent(circle, r, point, false)};
	}

}
