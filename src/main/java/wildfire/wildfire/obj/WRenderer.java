package wildfire.wildfire.obj;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;
import wildfire.input.car.CarData;
import wildfire.input.car.CarOrientation;
import wildfire.input.car.Hitbox;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.physics.DrivePhysics;
import wildfire.wildfire.pitch.Triangle;
import wildfire.wildfire.utils.Constants;

public class WRenderer extends Renderer {
	
	private Renderer r;
	private boolean twoD, threeD;
	
	// Occlusion.
	private static final double minZ = 9;

	public WRenderer(Wildfire wildfire, boolean twoD, boolean threeD){
		super(wildfire.getIndex());
		this.r = BotLoopRenderer.forBotLoop(wildfire); 
		this.twoD = twoD;
		this.threeD = threeD;
	}
	
	@SuppressWarnings ("deprecation")
	public void drawLine2d(Color color, Point start, Point end){
		if(twoD) r.drawLine2d(color, start, end);
    }
	
	public void drawLine3d(Color color, Vector2 start, Vector2 end){
		if(threeD) r.drawLine3d(color, start.withZ(minZ), end.withZ(minZ));
    }

    public void drawLine3d(Color color, rlbot.vector.Vector3 start, rlbot.vector.Vector3 end){
    	if(threeD) r.drawLine3d(color, start, end);
    }

    @SuppressWarnings ("deprecation")
	public void drawLine2d3d(Color color, Point start, rlbot.vector.Vector3 end){
    	if(twoD && threeD) r.drawLine2d3d(color, start, end);
    }

    public void drawRectangle2d(Color color, Point upperLeft, int width, int height, boolean filled){
    	if(twoD) r.drawRectangle2d(color, upperLeft, width, height, filled);
    }

    public void drawRectangle3d(Color color, rlbot.vector.Vector3 upperLeft, int width, int height, boolean filled){
        if(threeD) r.drawRectangle3d(color, upperLeft, width, height, filled);
    }

    public void drawCenteredRectangle3d(Color color, rlbot.vector.Vector3 position, int width, int height, boolean filled){
        if(threeD) r.drawCenteredRectangle3d(color, position, width, height, filled);
    }

    public void drawString2d(String text, Color color, Point upperLeft, int scaleX, int scaleY){
    	if(twoD) r.drawString2d(text, color, upperLeft, scaleX, scaleY);
    }

    public void drawString3d(String text, Color color, rlbot.vector.Vector3 upperLeft, int scaleX, int scaleY){
    	if(threeD) r.drawString3d(text, color, upperLeft, scaleX, scaleY);
    }
    
    public void drawCrosshair(CarData car, Vector3 point, Color colour, double size){
    	if(!threeD || car.position.minus(point).isZero()) return;
    	drawLine3d(colour, point.withZ(point.z - size / 2), point.withZ(point.z + size / 2));
    	Vector3 orthogonal = car.position.minus(point).scaledToMagnitude(size / 2).rotateHorizontal(Math.PI / 2).withZ(0);
    	drawLine3d(colour, point.plus(orthogonal), point.minus(orthogonal));
    }
    
    public void drawCircle(Color colour, Circle circle){
    	drawCircle(colour, circle.getCentre(), circle.getRadius());
    }
	
	public void drawCircle(Color colour, Vector2 centre, double radius){
		if(!threeD || centre == null) return;
		drawCircle(colour, centre.withZ(minZ), radius);
	}
	
	public void drawCircle(Color colour, Vector3 centre, double radius){
		if(!threeD) return;
		Vector3 last = null;
		double pointCount = 75;
		for(double i = 0; i < pointCount; i += 1){
            double angle = 2 * Math.PI * i / pointCount;
            Vector3 latest = new Vector3(centre.x + radius * Math.cos(angle), centre.y + radius * Math.sin(angle), centre.z);
            if(last != null && !last.isOutOfBounds() && !latest.isOutOfBounds()) drawLine3d(colour, last, latest);
            last = latest;
        }
		
		// Connect the end to the start.
		Vector3 start = new Vector3(centre.x + radius, centre.y, centre.z);
		if(!last.isOutOfBounds() && !start.isOutOfBounds()) drawLine3d(colour, last, start);
	}
	
	/**
	 * Draws the turning radius of the given car.
	 */
	public void drawTurningRadius(Color colour, CarData car){
		if(!threeD) return;
    	double turningRadius = DrivePhysics.getTurnRadius(car.velocity.flatten().magnitude());
    	drawCircle(colour, car.position.plus(car.orientation.right.withZ(0).scaledToMagnitude(turningRadius)).flatten(), turningRadius);
    	drawCircle(colour, car.position.plus(car.orientation.right.withZ(0).scaledToMagnitude(-turningRadius)).flatten(), turningRadius);
	}
	
	public boolean is2D(){
		return twoD;
	}

	public void set2D(boolean twoD){
		this.twoD = twoD;
	}

	public boolean is3D(){
		return threeD;
	}

	public void set3D(boolean threeD){
		this.threeD = threeD;
	}
	
	public void renderPrediction(BallPrediction ballPrediction, Color colour, int start, int end){
		if(ballPrediction == null || start == end) return;
		for(int i = Math.max(1, start); i < Math.min(ballPrediction.slicesLength(), end); i++){
			Vector3 a = new Vector3(ballPrediction.slices(i - 1).physics().location());
			Vector3 b = new Vector3(ballPrediction.slices(i).physics().location());
			drawLine3d(colour, a, b);
		}
	}
	
	public void drawTriangle(Color color, Triangle t){
    	if(!threeD) return;
    	
    	Vector3[] vectors = new Vector3[] {t.getVector(0).withZ(Math.max(minZ, t.getVector(0).z)),
    			t.getVector(1).withZ(Math.max(minZ, t.getVector(1).z)),
    			t.getVector(2).withZ(Math.max(minZ, t.getVector(2).z))};
    	
    	r.drawLine3d(color, vectors[0], vectors[1]);
    	r.drawLine3d(color, vectors[0], vectors[2]);
    	r.drawLine3d(color, vectors[1], vectors[2]);
    }

	public void drawPolyline3d(Color colour, Vector2[] points){
		if(!threeD || points.length < 2);
		for(int i = 0; i < points.length - 1; i++){
			r.drawLine3d(colour, points[i].withZ(minZ), points[i + 1].withZ(minZ));
		}
	}
	
	public void drawPolyline3d(Color colour, Vector3[] points){
		if(!threeD || points.length < 2);
		for(int i = 0; i < points.length - 1; i++){
			r.drawLine3d(colour, points[i], points[i + 1]);
		}
	}
	
	public void drawUprightSquare(Vector3 centre, Color colour, Vector3 forward, Vector3 up, double size){
		if(!threeD) return;
		
		Vector3 right = forward.crossProduct(up);
		forward = forward.scaledToMagnitude(size / 2);
		up = up.scaledToMagnitude(size / 2);
		right = right.scaledToMagnitude(size / 2);
		
		r.drawLine3d(colour, centre.plus(right).plus(up), centre.plus(right).minus(up));
		r.drawLine3d(colour, centre.minus(right).plus(up), centre.minus(right).minus(up));
		r.drawLine3d(colour, centre.plus(right).plus(up), centre.minus(right).plus(up));
		r.drawLine3d(colour, centre.plus(right).minus(up), centre.minus(right).minus(up));
	}
	
	public void drawUprightSquare(Vector3 centre, Color colour, Vector3 forward, double size){
		drawUprightSquare(centre, colour, forward, Vector3.Z, size);
	}
	
	public void drawHitbox(Vector3 position, CarOrientation orientation, Hitbox hitbox, Color colour){
		if(!threeD) return;
		
		Vector3 centre = position.plus(orientation.right.scaled(hitbox.offset.x)).plus(orientation.forward.scaled(hitbox.offset.y)).plus(orientation.up.scaled(hitbox.offset.z));
		
		for(double x = -1; x <= 1; x += 2){
			for(double y = -1; y <= 1; y += 2){
				for(double z = -1; z <= 1; z += 2){
					if(x != 1) r.drawLine3d(colour, centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * z)), centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * -x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * z)));
					if(y != 1) r.drawLine3d(colour, centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * z)), centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * -y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * z)));
					if(z != 1) r.drawLine3d(colour, centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * z)), centre.plus(orientation.right.scaled(hitbox.shape.x / 2 * x)).plus(orientation.forward.scaled(hitbox.shape.y / 2 * y)).plus(orientation.up.scaled(hitbox.shape.z / 2 * -z)));
				}
			}
		}
	}
		
	public void drawHitbox(CarData car, Color colour){
		if(!threeD) return;
		
		final double step = (1D / 60);
		
		CarOrientation orientation = car.orientation.step(step, car.angularVelocity);
		Vector3 position = car.position.plus(car.velocity.scaled(step));

		drawHitbox(position, orientation, car.hitbox, colour);
	}

	public void drawFlatSquare(Color colour, Vector3 centre, Vector3 surface1, Vector3 normal, double sideLength){
		if(!threeD) return;
		
		if(centre.z < minZ) centre = centre.withZ(minZ);
		surface1 = surface1.normalised();
		normal = normal.normalised();
		
		// https://math.stackexchange.com/a/32602
		Vector3 surface2 = normal.multiply(surface1).scaled(1D / normal.magnitudeSquared()).normalised();
		
		sideLength /= 2;
		surface1 = surface1.scaled(sideLength);
		surface2 = surface2.scaled(sideLength);
		
		r.drawLine3d(colour, centre.plus(surface1).plus(surface2), centre.plus(surface1).minus(surface2));
		r.drawLine3d(colour, centre.plus(surface1).minus(surface2), centre.minus(surface1).minus(surface2));
		r.drawLine3d(colour, centre.minus(surface1).minus(surface2), centre.minus(surface1).plus(surface2));
		r.drawLine3d(colour, centre.minus(surface1).plus(surface2), centre.plus(surface1).plus(surface2));
	}

}
