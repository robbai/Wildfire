package wildfire.wildfire.grabby.grabber;

import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;

import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;
import wildfire.input.DataPacket;
import wildfire.input.car.CarOrientation;
import wildfire.output.ControlsOutput;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.actions.DodgeAction;
import wildfire.wildfire.grabby.Grabber;
import wildfire.wildfire.grabby.Grabby;
import wildfire.wildfire.handling.Handling;
import wildfire.wildfire.obj.Slice;
import wildfire.wildfire.utils.Constants;
import wildfire.wildfire.utils.Utils;

public class DodgeGrabber extends Grabber {

	public static final double[] speeds = new double[] { -2300, -2200, -2100, -2000, -1900, -1800, -1700, -1600, -1500,
			-1400, -1300, -1200, -1100, -1000, -900, -800, -700, -600, -500, -400, -300, -200, -100, 0, 100, 200, 300,
			400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200,
			2300 };
	public static final double[] angles = new double[] { 0.0, 0.17453292519943295, 0.3490658503988659,
			0.5235987755982988, 0.6981317007977318, 0.8726646259971648, 1.0471975511965976, 1.2217304763960306,
			1.3962634015954636, 1.5707963267948966, 1.7453292519943295, 1.9198621771937625, 2.0943951023931953,
			2.2689280275926285, 2.443460952792061, 2.6179938779914944, 2.792526803190927, 2.9670597283903604, Math.PI };

	public static final Vector3 origin = new Vector3(0, 0, Constants.RIPPER_RESTING), noseVector = new Vector3(0, 1, 0),
			rightVector = new Vector3(1, 0, 0), roofVector = new Vector3(0, 0, 1);
	public static final Vector2 noseFlat = noseVector.flatten();

	private int speedIndex, angleIndex;
	private float timeSet;

	private DodgeAction dodge;

	private Object[] currentData;
	private Slice start;

	private boolean saved;
	private long initMillis;
	private boolean incremented;

	public DodgeGrabber(Grabby grabby){
		super(grabby, "dodge.csv", new String[] { "input_angle", "velocity_angle", "displace_angle", "impulse_angle",
				"displace_forward", "displace_side", "start_speed", "end_speed", "time_taken" });
		this.dodge = null;

		this.speedIndex = (speeds.length - 1);
		this.angleIndex = -1;

		this.saved = false;

		this.initMillis = System.currentTimeMillis();
		this.timeSet = 0;
		this.incremented = false;
	}

	@Override
	public ControlsOutput processInput(DataPacket input){
		if(saved)
			return new ControlsOutput().withNone();
		if(System.currentTimeMillis() - this.initMillis < 2000){
			this.setupState(true);
			this.timeSet = input.elapsedSeconds;
			return new ControlsOutput().withNone();
		}

		Renderer renderer = BotLoopRenderer.forBotLoop(this.grabby);
		if(this.speedIndex >= 0 && this.speedIndex < speeds.length && this.angleIndex >= 0
				&& this.angleIndex < angles.length){
			renderer.drawString2d("Speed: " + (int)speeds[this.speedIndex], Color.WHITE, new Point(0, 30), 2, 2);
			renderer.drawString2d("Angle: " + (int)Math.toDegrees(angles[this.angleIndex]), Color.WHITE,
					new Point(0, 60), 2, 2);
			renderer.drawString2d((int)((double)(this.angleIndex * speeds.length + this.speedIndex + 1)
					/ (speeds.length * angles.length) * 100D) + "%", Color.WHITE, new Point(0, 90), 2, 2);
			renderer.drawString2d(
					dodge == null ? "null" : (Utils.round(dodge.timeDifference(input.elapsedSeconds)) + "s"),
					Color.CYAN, new Point(0, 120), 2, 2);
		}

		if(input.car.hasWheelContact && this.dodge == null){
			if(input.elapsedSeconds - this.timeSet < 0.1){
				if(!this.incremented){
					this.speedIndex++;
					if(this.speedIndex == speeds.length){
						this.speedIndex = 0;
						this.angleIndex++;
					}

					if(this.angleIndex >= angles.length){
						if(!this.saved){
							this.write();
							this.saved = true;
						}
						return new ControlsOutput();
					}

					this.incremented = true;
				}

				this.setupState(input.elapsedSeconds - this.timeSet < 0.05);

				this.dodge = null;
			}else{
				this.setupState(false);

				this.currentData = new Object[] { angles[this.angleIndex], null, null, null, null, null,
						input.car.forwardVelocity, null, null };
				this.start = new Slice(input.car.position, input.elapsedSeconds);

				// TODO
//				this.dodge = new DodgeAction(null, angles[this.angleIndex], input);
			}
		}else if(input.car.hasWheelContact && (input.elapsedSeconds - this.start.getTime()) > 0.3){
			this.dodge = null;

			Vector2 displacement = input.car.position.minus(this.start.getPosition()).flatten();
			Vector2 carVelocity = input.car.velocity.flatten();
			Vector2 velocityChange = carVelocity.minus(noseFlat.scaled(speeds[this.speedIndex]));

			this.currentData[1] = noseFlat.angle(carVelocity);
			this.currentData[2] = noseFlat.angle(displacement);
			this.currentData[3] = noseFlat.angle(velocityChange);
			this.currentData[4] = displacement.y;
			this.currentData[5] = Math.abs(displacement.x);
			this.currentData[7] = input.car.forwardVelocity;
			this.currentData[8] = (input.elapsedSeconds - this.start.getTime());

			System.out.println(Arrays.toString(this.currentData));
			this.addData(this.currentData);

			this.timeSet = input.elapsedSeconds;
			this.incremented = false;
		}

		// TODO
//		if(dodge != null) return this.dodge.getOutput(input);
		double throttle = Handling.produceAcceleration(input.car,
				(speeds[this.speedIndex] - input.car.forwardVelocity) * 60);
		return new ControlsOutput().withThrottle(throttle).withBoost(throttle > 1);
	}

	private void setupState(boolean stop){
		GameState gameState = new GameState();

		Vector3 ballPosition = new Vector3(3000, 3000, Constants.BALL_RADIUS);
		Vector3 ballVelocity = new Vector3(0, 0, 0);
		Vector3 ballAngVelocity = new Vector3();
		gameState.withBallState(new BallState().withPhysics(new PhysicsState().withLocation(ballPosition.toDesired())
				.withVelocity(ballVelocity.toDesired()).withAngularVelocity(ballAngVelocity.toDesired())));

		Vector3 carPosition = origin;
		Vector3 carVelocity = new Vector3(0, stop ? 0 : speeds[this.speedIndex], 0);
		Vector3 carAngVelocity = new Vector3();
		CarOrientation carOrientation = new CarOrientation(noseVector, rightVector, roofVector);
		float boost = 0;
		gameState.withCarState(grabby.getIndex(),
				new CarState().withBoostAmount(boost)
						.withPhysics(new PhysicsState().withLocation(carPosition.toDesired())
								.withVelocity(carVelocity.toDesired()).withRotation(carOrientation.toDesired())
								.withAngularVelocity(carAngVelocity.toDesired())));

		RLBotDll.setGameState(gameState.buildPacket());
	}

}
