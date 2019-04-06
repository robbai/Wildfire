package wildfire.wildfire.states;

import java.awt.Color;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector3;
import wildfire.wildfire.Utils;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.RecoveryAction;
import wildfire.wildfire.obj.PredictionSlice;
import wildfire.wildfire.obj.State;

public class PatienceState extends State {
	
	/**
	 * Wait for a rolling shot
	 */
	
	private final double maxWaitingSeconds = 2.7, coneBorder = -250;
	
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
		if(Utils.isInCone(input.car, wildfire.impactPoint.getPosition(), coneBorder)) return false;
		
		//Check if there's a solid defender who can hit the ball quicker
		double closestOpponentDistance = Utils.closestOpponentDistance(input, input.ball.position);
		if(closestOpponentDistance < Math.max(2300, impactDistance * 1.4) || Utils.teamSign(input.car.team) * input.ball.velocity.y < -850) return false;
		
		int startFrame = wildfire.impactPoint.getFrame();
		for(int i = startFrame; i < Math.min(wildfire.ballPrediction.slicesLength(), startFrame + (maxWaitingSeconds * 60)); i++){
			Vector3 location = Vector3.fromFlatbuffer(wildfire.ballPrediction.slices(i).physics().location());
			
			if(Utils.isInCone(input.car, location, coneBorder) && location.z < 300){
				wildfire.renderer.renderPrediction(wildfire.ballPrediction, Color.GREEN, startFrame, i);
				wildfire.renderer.drawCrosshair(input.car, location, Color.YELLOW, 50);
				point = new PredictionSlice(location, i);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		if(!hasAction() && Utils.isCarAirborne(input.car)){
			currentAction = new RecoveryAction(this, input.elapsedSeconds);
			return currentAction.getOutput(input);
		}
		
		double s = (point.getPosition().distanceFlat(input.car.position) - 110);
		double t = point.getTime();
		double u = input.car.velocity.flatten().magnitudeInDirection(point.getPosition().minus(input.car.position).flatten());
		double v = ((2 * s) / t - u);
		
		double steerRadians = Utils.aim(input.car, point.getPosition().flatten());
	    float steer = (float)-steerRadians * 3F;
	    
		ControlsOutput controls = new ControlsOutput().withSteer(steer).withSlide(Math.abs(steerRadians) > 1.2);
		
		if(v > u){
			if(v < 1300 && Math.abs(steerRadians) < 0.1){
				controls.withThrottle((float)-input.car.forwardMagnitude() / 1000);
			}else if(v > u + 1000 || v > 1410){
				controls.withThrottle(1).withBoost(Math.abs(steer) < 0.55F);
			}else{
				controls.withThrottle(1);
			}
		}else if(v < u - 400){
			controls.withThrottle(-1);
		}else{
			controls.withThrottle(0);
		}
		
		return controls;
	}

}
