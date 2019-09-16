package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Utils;

public class PatienceState extends State {
	
	/**
	 * Wait for a rolling shot
	 */
	
	private final double maxWaitingSeconds = 2.6, delaySeconds = 0.14, coneBorder = -130;
	
	private Slice point;

	public PatienceState(Wildfire wildfire){
		super("Patience", wildfire);
	}
	
	@Override
	public boolean ready(InfoPacket input){
		point = null;
		
		// General check to see if its a good idea
		if(input.info.impact.getPosition().y * Utils.teamSign(input.car) < 1000 || input.car.position.z > 200) return false;
		double impactDistance = input.car.position.distanceFlat(input.info.impact.getPosition());
		if(impactDistance > 5000) return false;
		
		// Check if we can already shoot
		if(Behaviour.isInCone(input.car, input.info.impact.getPosition(), coneBorder)) return false;
		
		// Check if there's a solid defender who can hit the ball quicker
		double closestOpponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		if(closestOpponentDistance < Math.max(2300, impactDistance * 1.4) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850) return false;
		
		int startFrame = input.info.impact.getFrame();
		for(int i = (startFrame + (int)(delaySeconds * 60)); i < Math.min(wildfire.ballPrediction.slicesLength(), startFrame + (maxWaitingSeconds * 60)); i++){
			Vector3 location = Vector3.fromFlatbuffer(wildfire.ballPrediction.slices(i).physics().location());
			
			if(Behaviour.isInCone(input.car, location, coneBorder) && location.z < 300){
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, startFrame);
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, startFrame, i);
				wildfire.renderer.drawCrosshair(input.car, location, Color.YELLOW, 50);
				this.point = new Slice(location, i);
				break;
			}
		}
		
		// Make sure that the shot is actually hard in the first place.
		// Don't look like a fool and wait for a straight shot instead of a quick curved shot.
		if(this.point != null){
			Vector2 pointDisplace = this.point.getPosition().minus(input.car.position).flatten();
			if(pointDisplace.magnitude() > 1500 || input.car.velocity.magnitude() > 1100){
				Vector2 impactDisplace = input.info.impact.getPosition().minus(input.car.position).flatten();
				double angle = pointDisplace.angle(impactDisplace);
				if(angle < 0.08) return false;
			}
		}
		
		return this.point != null;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		if(Behaviour.isCarAirborne(car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		// Motion equations.
		double s = (point.getPosition().distanceFlat(car.position) - 110);
		double t = point.getTime();
		double u = car.velocityDir(point.getPosition().minus(car.position).flatten());
		double v = ((2 * s) / t - u);
		double a = (v - u) / t;
		
		// Render.
		wildfire.renderer.drawString2d("Final Velocity: " + (int)v + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Acceleration: " + (int)a + "uu/s^2", Color.WHITE, new Point(0, 40), 2, 2);
		
		// Controls.
	    ControlsOutput controls;
	    double pointRadians = Handling.aim(car, point.getPosition().flatten());
		if(Math.abs(pointRadians) > 0.3 || v > DrivePhysics.maxVelocity(u, car.boost) - 250){
			double throttle = Handling.produceAcceleration(car, a);
			controls = Handling.forwardDrive(car, point.getPosition());
			controls = controls.withThrottle(throttle).withBoost(throttle > 1 && Math.abs(pointRadians) < 0.2);
		}else{
			controls = Handling.stayStill(car);
		}
		return controls;
	}

}
