package wildfire.wildfire.states;

import java.awt.Color;
import java.awt.Point;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Physics;
import wildfire.wildfire.utils.Utils;

public class PatienceState extends State {
	
	/**
	 * Wait for a rolling shot
	 */
	
	private final double maxWaitingSeconds = 2.6, delaySeconds = 0.1, coneBorder = -130;
	
	private PredictionSlice point;

	public PatienceState(Wildfire wildfire){
		super("Patience", wildfire);
	}
	
	@Override
	public boolean ready(DataPacket input){
		point = null;
		
		//General check to see if its a good idea
		if(wildfire.impactPoint.getPosition().y * Utils.teamSign(input.car) < 1000 || input.car.position.z > 200) return false;
		double impactDistance = input.car.position.distanceFlat(wildfire.impactPoint.getPosition());
		if(impactDistance > 5000) return false;
		
		//Check if we can already shoot
		if(Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition(), coneBorder)) return false;
		
		//Check if there's a solid defender who can hit the ball quicker
		double closestOpponentDistance = Behaviour.closestOpponentDistance(input, input.ball.position);
		if(closestOpponentDistance < Math.max(2300, impactDistance * 1.4) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850) return false;
		
		int startFrame = wildfire.impactPoint.getFrame();
		for(int i = (startFrame + (int)(delaySeconds * 60)); i < Math.min(wildfire.ballPrediction.slicesLength(), startFrame + (maxWaitingSeconds * 60)); i++){
			Vector3 location = Vector3.fromFlatbuffer(wildfire.ballPrediction.slices(i).physics().location());
			
			if(Behaviour.isInCone(input.car, location, coneBorder) && location.z < 300){
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.YELLOW, 0, startFrame);
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, startFrame, i);
				wildfire.renderer.drawCrosshair(input.car, location, Color.YELLOW, 50);
				this.point = new PredictionSlice(location, i);
				break;
			}
		}
		
		// Make sure that the shot is actually hard in the first place.
		// Don't look like a fool and wait for a straight shot instead of a quick curved shot.
		if(this.point != null){
			Vector2 pointDisplace = this.point.getPosition().minus(input.car.position).flatten();
			if(pointDisplace.magnitude() > 1000 || input.car.velocity.magnitude() > 900){
				Vector2 impactDisplace = wildfire.impactPoint.getPosition().minus(input.car.position).flatten();
				double angle = pointDisplace.angle(impactDisplace);
				System.out.println((int)Math.toDegrees(angle) + " degrees");
				if(angle < 0.1) return false;
			}
		}
		
		return this.point != null;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && Behaviour.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		//Motion equations
		double s = (point.getPosition().distanceFlat(input.car.position) - 110);
		double t = point.getTime();
		double u = input.car.velocity.flatten().magnitudeInDirection(point.getPosition().minus(input.car.position).flatten());
		double v = ((2 * s) / t - u);
		double a = (v - u) / t;
		
		//Render
		wildfire.renderer.drawString2d("Final Velocity: " + (int)v + "uu/s", Color.WHITE, new Point(0, 20), 2, 2);
		wildfire.renderer.drawString2d("Acceleration: " + (int)a + "uu/s^2", Color.WHITE, new Point(0, 40), 2, 2);
		
		double steerRadians = Handling.aim(input.car, point.getPosition().flatten());
	    float steer = (float)steerRadians * -3F;
	    
		ControlsOutput controls = new ControlsOutput().withSteer(steer).withSlide(Math.abs(steerRadians) > 1.2 && Math.abs(u) > 500);
		if(Math.abs(steerRadians) > 0.3 || v > Physics.boostMaxSpeed(u, input.car.boost) - 250){
			controls.withThrottle((float)(a / 1200)).withBoost(a > 1100 && Math.abs(steerRadians) < 0.2);
		}else{
			controls.withThrottle(0).withBoost(false);
		}
		
		return controls;
	}

}
