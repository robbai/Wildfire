package wildfire.wildfire.curve;

import java.awt.Color;
import java.awt.Point;
import java.util.OptionalDouble;

import wildfire.vector.Vector2;
import wildfire.wildfire.obj.Circle;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.utils.Utils;

/**
 * http://www.ryanjuckett.com/programming/biarc-interpolation/ I thank
 * whatisaphone for linking this in his bot's README
 */

public class Biarc extends Curve {

	private static final double epsilon = 0.0000001;

	private final Vector2 start, startDir, end, endDir, connection;
	private final double dOne, arcOneLength, arcTwoLength;
	private final OptionalDouble dTwo;
	private final Circle circleOne, circleTwo;

	public Biarc(Vector2 start, Vector2 startDir, Vector2 end, Vector2 endDir){
		this.start = start;
		this.startDir = startDir.normalised();
		this.end = end;
		this.endDir = endDir.normalised();

		this.dOne = this.chooseD();
		this.dTwo = this.solveD(this.dOne);

		this.connection = this.findConnection();

		Pair<Circle, Circle> circles = this.findCircles();
		this.circleOne = circles.getOne();
		this.circleTwo = circles.getTwo();

		this.arcOneLength = this.circleOne.getSectorCircumference(
				this.start.minus(this.circleOne.getCentre()).angle(this.connection.minus(this.circleOne.getCentre())));
		this.arcTwoLength = this.circleTwo.getSectorCircumference(
				this.end.minus(this.circleTwo.getCentre()).angle(this.connection.minus(this.circleTwo.getCentre())));
	}

	private double chooseD(){
		Vector2 v = this.end.minus(this.start);
		Vector2 t = this.startDir.plus(this.endDir);

		double denominator = 2 * (1 - this.startDir.dotProduct(this.endDir));
		if(Math.abs(denominator) > epsilon){
			double square = Math.pow(v.dotProduct(t), 2) + denominator * v.dotProduct(v);
			return (-v.dotProduct(t) + Math.sqrt(square)) / denominator;
		}

		// Note: I didn't actually deal with case 3, since it's so rare to occur in
		// these conditions
		return v.dotProduct(v) / (4 * v.dotProduct(this.endDir));
	}

	private OptionalDouble solveD(double otherD){
		Vector2 v = this.end.minus(this.start);

		double denominator = v.dotProduct(this.endDir) - otherD * (this.startDir.dotProduct(this.endDir) - 1);
		if(Math.abs(denominator) > epsilon){
			return OptionalDouble.of((0.5 * v.dotProduct(v) - otherD * v.dotProduct(this.startDir)) / denominator);
		}

		return OptionalDouble.empty();
	}

	private Pair<Circle, Circle> findCircles(){
		Vector2 nOne = this.startDir.cross();
		double cOneRadius = this.connection.minus(this.start).dotProduct(this.connection.minus(this.start))
				/ (nOne.scaled(2).dotProduct(this.connection.minus(this.start)));
		Vector2 cOneCentre = this.start.plus(nOne.scaled(cOneRadius));

		Vector2 nTwo = this.endDir.cross();
		double cTwoRadius = this.connection.minus(this.end).dotProduct(this.connection.minus(this.end))
				/ (nTwo.scaled(2).dotProduct(this.connection.minus(this.end)));
		Vector2 cTwoCentre = this.end.plus(nTwo.scaled(cTwoRadius));

		Circle cOne = new Circle(cOneCentre, cOneRadius);
		Circle cTwo = new Circle(cTwoCentre, cTwoRadius);

		return new Pair<Circle, Circle>(cOne, cTwo);
	}

	private Vector2 findConnection(){
		if(this.dTwo.isPresent()){
			double dTwoVal = this.dTwo.getAsDouble();
			return (this.start.plus(this.startDir.scaled(dOne))).scaled(dTwoVal / (this.dOne + dTwoVal))
					.plus((this.end.minus(this.endDir.scaled(dTwoVal))).scaled(this.dOne / (this.dOne + dTwoVal)));
		}

		Vector2 v = this.end.minus(this.start);
		return this.start.plus(this.startDir.scaled(dOne)).plus(this.endDir
				.scaled(v.dotProduct(this.endDir) - this.startDir.scaled(this.dOne).dotProduct(this.endDir)));
	}

	@Override
	public Vector2 T(double t){
		t = Utils.clamp(t, 0, 1);

		double arcDivide = (this.arcOneLength / this.getLength());

		if(t < arcDivide){
			double sector = (t / arcDivide);
			Vector2 toStart = this.start.minus(this.circleOne.getCentre());
			Vector2 toConnection = this.connection.minus(this.circleOne.getCentre());
			return this.circleOne.getCentre().plus(toStart.rotate(toStart.correctionAngle(toConnection) * sector));
		}else{
			double sector = ((t - arcDivide) / (1 - arcDivide));
			Vector2 toEnd = this.end.minus(this.circleTwo.getCentre());
			Vector2 toConnection = this.connection.minus(this.circleTwo.getCentre());
			return this.circleTwo.getCentre().plus(toConnection.rotate(toConnection.correctionAngle(toEnd) * sector));
		}
	}

	@Override
	public double getLength(){
		return this.arcOneLength + this.arcTwoLength;
	}

	public void render(WRenderer renderer, boolean verbose){
		renderer.drawLine3d(Color.WHITE, this.start, this.start.plus(this.startDir.scaled(this.dOne)));
		if(this.dTwo.isPresent()){
			renderer.drawLine3d(Color.WHITE, this.end, this.end.plus(this.endDir.scaled(-this.dTwo.getAsDouble())));
			renderer.drawLine3d(Color.WHITE, this.start.plus(this.startDir.scaled(this.dOne)),
					this.end.plus(this.endDir.scaled(-this.dTwo.getAsDouble())));
		}
		renderer.drawCircle(Color.RED, this.circleOne);
		renderer.drawLine3d(Color.RED, this.circleOne.getCentre(), this.start);
		renderer.drawLine3d(Color.RED, this.circleOne.getCentre(), this.connection);
		renderer.drawCircle(Color.BLUE, this.circleTwo);
		renderer.drawLine3d(Color.BLUE, this.circleTwo.getCentre(), this.end);
		renderer.drawLine3d(Color.BLUE, this.circleTwo.getCentre(), this.connection);
		renderer.drawCircle(Color.YELLOW, this.connection, 25);

		if(verbose){
			renderer.drawString2d(
					"Radii: " + (int)this.circleOne.getRadius() + "uu, " + (int)this.circleTwo.getRadius() + "uu",
					Color.WHITE, new Point(0, 20), 2, 2);
			renderer.drawString2d("Length: " + (int)this.getLength() + "uu", Color.WHITE, new Point(0, 40), 2, 2);
		}
	}

	public void render(WRenderer renderer){
		render(renderer, false);
	}

	public Pair<Double, Double> getRadii(){
		return new Pair<Double, Double>(this.circleOne.getRadius(), this.circleTwo.getRadius());
	}

}
