package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.obj.State;

public class CircleState extends State {
	
//	private double circleSpeed = 0;

	public CircleState(Wildfire wildfire){
		super("Circle", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		return !Utils.isKickoff(input);
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		//Drive down the wall
		if(Utils.isOnWall(input.car)){
			wildfire.renderer.drawString2d("Wall", Color.WHITE, new Point(0, 20), 2, 2);
			return Utils.driveDownWall(input);
		}
				
		double currentSpeed = input.car.velocity.magnitude();
		double turningRadius = Utils.getTurnRadius(currentSpeed);
		
		Vector2 goal = Utils.enemyGoal(wildfire.team);
		Vector2 ball = wildfire.impactPoint.getPosition().flatten();
		Vector2 intersect = intersect(ball, ball.plus(ball.minus(goal)), input.car.position.flatten(), input.car.position.flatten().plus(input.car.orientation.noseVector.flatten())).confine();
		
		double steer = Utils.aim(input.car, intersect);
		boolean left = steer > 0;
		
		Vector2 circleCentre = input.car.position.flatten().plus(input.car.orientation.rightVector.flatten().scaledToMagnitude(turningRadius * (left ? -1 : 1)));
		double distance = Utils.distanceFromLine(circleCentre, goal, intersect);
		
		//3D Rendering
//		wildfire.renderer.drawTurningRadius(Color.ORANGE, input.car);
		wildfire.renderer.drawLine3d(Color.YELLOW, input.car.position.flatten().toFramework(), intersect.toFramework());
		wildfire.renderer.drawLine3d(Color.YELLOW, goal.toFramework(), intersect.toFramework());
//		wildfire.renderer.drawCircle(Color.RED, intersect, 100);
		
		//2D Rendering
		wildfire.renderer.drawString2d("Turning Radius: " + (int)turningRadius, Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Distance: " + (int)distance + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		
		double throttle;
//		if(distance < turningRadius){
//			steer = left ? -1 : 1;
//			wildfire.renderer.drawString2d((left ? "Left" : "Right") + " (Circle)", Color.WHITE, new Point(0, 60), 2, 2);
//			throttle = currentSpeed < circleSpeed ? 1 : (currentSpeed > circleSpeed + 500 ? -1 : 0);
//		}else{
			steer *= -2;
			wildfire.renderer.drawString2d(left ? "Left" : "Right", Color.WHITE, new Point(0, 60), 2, 2);
//			circleSpeed = input.car.velocity.magnitude();
			throttle = 1;
//		}
		return new ControlsOutput().withSteer((float)steer).withBoost(Math.abs(steer) != 1).withSlide(false).withThrottle((float)throttle);
	}
	
	private Vector2 intersect(Vector2 lineOneA, Vector2 lineOneB, Vector2 lineTwoA, Vector2 lineTwoB){
		double oneM = (lineOneA.y - lineOneB.y) / (lineOneA.x - lineOneB.x);
		double oneC = -(oneM * lineOneA.x - lineOneA.y);
		
		double twoM = (lineTwoA.y - lineTwoB.y) / (lineTwoA.x - lineTwoB.x);
		double twoC = -(twoM * lineTwoA.x - lineTwoA.y);
		
		// oneM * x + oneC = twoM * x + twoC
		// (oneM * x - twoM * x) + oneC = twoC
		// (oneM * x - twoM * x) = twoC - oneC
		// x = (twoC - oneC) / (oneM - twoM)
		
		double x = (twoC - oneC) / (oneM - twoM);
		double y = oneM * x + oneC;
		return new Vector2(x, y);
	}

}
