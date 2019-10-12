package wildfire.wildfire.states;

import java.awt.Color;

import rlbot.flat.QuickChatSelection;
import wildfire.input.car.CarData;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.input.InfoPacket;
import wildfire.wildfire.obj.State;
import wildfire.wildfire.utils.Behaviour;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class MixerState extends State {

	/*
	 * This state puts hits it towards the opponent's corner,
	 * which can be better than going for a shot if the opponent
	 * is in a good defensive position.
	 */
	
	private double maxGoalArea = 3100;

	public MixerState(Wildfire wildfire){
		super("Mixer", wildfire);
	}

	@Override
	public boolean ready(InfoPacket input){
		Vector3 impactLocation = input.info.impact.getPosition();
		Vector2 teamSignVec = new Vector2(0, input.car.sign);
		Vector2 carPosition = input.car.position.flatten();
		
//		// The ball must be on the wing.
		if(Math.abs(impactLocation.x) < Constants.PITCH_WIDTH - 550 * 2) return false;
		if(input.car.sign * impactLocation.y < -1000) return false;
		if(input.car.sign * impactLocation.y > Constants.PITCH_LENGTH - 900) return false;
		
		// We must be solidly behind the ball.
		if(input.info.impact.getTime() > 2.5 || Behaviour.isTeammateCloser(input)) return false;
		if(Behaviour.isCarAirborne(input.car) || Behaviour.isBallAirborne(input.ball)) return false;
		double yAngle = teamSignVec.angle(input.car.position.minus(impactLocation).flatten());
		if(yAngle > Math.toRadians(60)) return false;
		
		Vector2 backwallTrace = Utils.traceToY(carPosition, input.info.impact.getBallPosition().flatten().minus(carPosition), input.car.sign * Constants.PITCH_LENGTH);
		if(backwallTrace == null || Math.abs(backwallTrace.x) < Constants.PITCH_WIDTH - 450) return false;
		
//		// We must not have a (good) shot.
//		if(Behaviour.isInCone(input.car, input.info.impact.getPosition()) 
//				&& Math.abs(input.info.impact.getPosition().x) < Constants.PITCH_WIDTH - 1100) return false;
		
		// There must be a goalkeeper.
		CarData goalkeeper = Behaviour.getGoalkeeper(input.cars, 1 - input.car.team, maxGoalArea);
		return goalkeeper != null;
	}

	@Override
	public ControlsOutput getOutput(InfoPacket input){
		CarData car = input.car;
		
		Vector3 impactLocation = input.info.impact.getBallPosition();
		double impactDistance = input.info.impactDistance;
		
		Vector2 corner = new Vector2(Math.signum(impactLocation.x) * (Constants.PITCH_WIDTH - Constants.BALL_RADIUS), car.sign * (Constants.PITCH_LENGTH - 700));
		double aimImpact = Handling.aim(car, impactLocation.flatten());
		
		// Dodge.
		if((input.info.impact.getTime() < Behaviour.IMPACT_DODGE_TIME
				|| (impactDistance > Behaviour.dodgeDistance(car) && car.forwardVelocity > 1200 && car.forwardVelocity < 2000))
				&& Math.abs(aimImpact) < 0.25){
			if(car.forwardVelocity > 1600) wildfire.sendQuickChat(QuickChatSelection.Information_Centering);
			currentAction = new DodgeAction(this, aimImpact, input);
		}
		if(currentAction != null && !currentAction.failed) return currentAction.getOutput(input);
		currentAction = null;
		
		double offsetMagnitude = 100;
		Vector2 offset = impactLocation.flatten().minus(corner).scaledToMagnitude(offsetMagnitude);
		Vector3 destination = impactLocation.plus(offset.withZ(0));
		
		//Render
		wildfire.renderer.drawCrosshair(car, destination, Color.CYAN, 60);
		wildfire.renderer.drawCircle(Color.CYAN, corner, 700);
		wildfire.renderer.drawCircle(Color.BLUE, Constants.homeGoal(1 - car.team), maxGoalArea);
		
		return Handling.forwardDrive(car, destination.flatten());
	}

}