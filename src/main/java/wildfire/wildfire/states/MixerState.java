package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.CarData;
import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Handling;
import wildfire.wildfire.utils.Utils;

public class MixerState extends State {

	/*
	 * This state puts hits it towards the opponent's corner
	 * This can be better than going for a shot
	 */
	
	private double maxGoalArea = 3000;

	public MixerState(Wildfire wildfire){
		super("Mixer", wildfire);
	}

	@Override
	public boolean ready(DataPacket input){
		Vector3 impactLocation = wildfire.impactPoint.getPosition();
		double teamSign = Utils.teamSign(input.car);
		Vector2 teamSignVec = new Vector2(0, teamSign);
		
		// The ball must be on the wing.
		if(Math.abs(impactLocation.x) < Constants.PITCHWIDTH - 800) return false;
		if(teamSign * impactLocation.y < -2500) return false;
		if(teamSign * impactLocation.y > Constants.PITCHLENGTH - 1200) return false;
		
		// We must be solidly behind the ball.
		if(wildfire.impactPoint.getTime() > 2.5 || Behaviour.isTeammateCloser(input)) return false;
		if(Behaviour.isCarAirborne(input.car) || Behaviour.isBallAirborne(input.ball)) return false;
		double yAngle = teamSignVec.angle(impactLocation.minus(input.car.position).flatten());
		if(yAngle > Math.toRadians(50)) return false;
		
		// We must not have a (good) shot.
		if(Behaviour.isInCone(input.car, wildfire.impactPoint.getPosition()) 
				&& Math.abs(wildfire.impactPoint.getPosition().y) < 2800) return false;
		
		// There must be a goalkeeper.
		CarData goalkeeper = Behaviour.getGoalkeeper(input.cars, 1 - input.car.team, maxGoalArea);
		return goalkeeper != null;
	}

	@Override
	public ControlsOutput getOutput(DataPacket input){
		Vector3 impactLocation = wildfire.impactPoint.getPosition();
		double impactDistance = impactLocation.distance(input.car.position);
		double teamSign = Utils.teamSign(input.car);
		Vector2 corner = new Vector2(Math.signum(impactLocation.x) * (Constants.PITCHWIDTH - 100),
				teamSign * (Constants.PITCHLENGTH - 700));
		double aimImpact = Handling.aim(input.car, impactLocation.flatten());
		
		if(!hasAction()){
			if((impactDistance < 300 || (impactDistance > 3500 && !input.car.isSupersonic)) && Math.abs(aimImpact) < 0.25){
				if(input.car.forwardMagnitude() > 1600) wildfire.sendQuickChat(QuickChatSelection.Information_Centering);
				currentAction = new DodgeAction(this, aimImpact, input);
			}
			if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
			currentAction = null;
		}
		
		double offsetMagnitude = 100;
		Vector2 offset = impactLocation.flatten().minus(corner).scaledToMagnitude(offsetMagnitude);
		Vector3 destination = impactLocation.plus(offset.withZ(0));
		
		//Render
		wildfire.renderer.drawCrosshair(input.car, destination, Color.CYAN, 60);
		wildfire.renderer.drawCircle(Color.CYAN, corner, 700);
		wildfire.renderer.drawCircle(Color.BLUE, Constants.homeGoal(1 - input.car.team), maxGoalArea);
		
		return Handling.drivePoint(input, destination.flatten(), true);
	}

}