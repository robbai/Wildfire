package wildfire.wildfire.obj;

import java.awt.Color;
import java.awt.Point;

import rlbot.flat.BallPrediction;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;
import wildfire.input.CarData;
import wildfire.vector.Vector2;
import wildfire.vector.Vector3;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.utils.Physics;

public class WRenderer extends Renderer {
	
	private Renderer r;
	private boolean twoD, threeD;

	public WRenderer(Wildfire wildfire, boolean twoD, boolean threeD){
		super(wildfire.getIndex());
		this.r = BotLoopRenderer.forBotLoop(wildfire); 
		this.twoD = twoD;
		this.threeD = threeD;
	}
	
	public void drawLine2d(java.awt.Color color, Point start, Point end){
		if(twoD) r.drawLine2d(color, start, end);
    }

    public void drawLine3d(java.awt.Color color, rlbot.vector.Vector3 start, rlbot.vector.Vector3 end){
    	if(threeD) r.drawLine3d(color, start, end);
    }

    public void drawLine2d3d(java.awt.Color color, Point start, rlbot.vector.Vector3 end){
    	if(twoD && threeD) r.drawLine2d3d(color, start, end);
    }

    public void drawRectangle2d(java.awt.Color color, Point upperLeft, int width, int height, boolean filled){
    	if(twoD) r.drawRectangle2d(color, upperLeft, width, height, filled);
    }

    public void drawRectangle3d(java.awt.Color color, rlbot.vector.Vector3 upperLeft, int width, int height, boolean filled){
        if(threeD) r.drawRectangle3d(color, upperLeft, width, height, filled);
    }

    public void drawCenteredRectangle3d(java.awt.Color color, rlbot.vector.Vector3 position, int width, int height, boolean filled){
        if(threeD) r.drawCenteredRectangle3d(color, position, width, height, filled);
    }

    public void drawString2d(String text, java.awt.Color color, Point upperLeft, int scaleX, int scaleY){
    	if(twoD) r.drawString2d(text, color, upperLeft, scaleX, scaleY);
    }

    public void drawString3d(String text, java.awt.Color color, rlbot.vector.Vector3 upperLeft, int scaleX, int scaleY){
    	if(threeD) r.drawString3d(text, color, upperLeft, scaleX, scaleY);
    }
    
    public void drawCrosshair(CarData car, Vector3 point, Color colour, double size){
    	if(!threeD || car.position.minus(point).isZero()) return;
    	drawLine3d(colour, point.withZ(point.z - size / 2).toFramework(), point.withZ(point.z + size / 2).toFramework());
    	Vector3 orthogonal = car.position.minus(point).scaledToMagnitude(size / 2).rotateHorizontal(Math.PI / 2).withZ(0);
    	drawLine3d(colour, point.plus(orthogonal).toFramework(), point.minus(orthogonal).toFramework());
    }
	
	public void drawCircle(Color colour, Vector2 centre, double radius){
		if(!threeD) return;
		drawCircle(colour, centre.withZ(20), radius);
	}
	
	public void drawCircle(Color colour, Vector3 centre, double radius){
		if(!threeD) return;
		Vector3 last = null;
		double pointCount = 100;
		for(double i = 0; i < pointCount; i += 1){
            double angle = 2 * Math.PI * i / pointCount;
            Vector3 latest = new Vector3(centre.x + radius * Math.cos(angle), centre.y + radius * Math.sin(angle), centre.z);
            if(last != null && !last.isOutOfBounds() && !latest.isOutOfBounds()) drawLine3d(colour, last.toFramework(), latest.toFramework());
            last = latest;
        }
		
		//Connect the end to the start
		Vector3 start = new Vector3(centre.x + radius, centre.y, centre.z);
		if(!last.isOutOfBounds() && !start.isOutOfBounds()) drawLine3d(colour, last.toFramework(), start.toFramework());
	}
	
	/*
	 * Draw the turning radius
	 */
	public void drawTurningRadius(Color colour, CarData car){
		if(!threeD) return;
    	double turningRadius = Physics.getTurnRadius(car.velocity.flatten().magnitude());
    	drawCircle(colour, car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(turningRadius)).flatten(), turningRadius);
    	drawCircle(colour, car.position.plus(car.orientation.rightVector.withZ(0).scaledToMagnitude(-turningRadius)).flatten(), turningRadius);
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
	
	public void renderPrediction(BallPrediction p, Color c, int s, int e){
		if(p == null || s == e) return;
		for(int i = Math.max(1, s); i < Math.min(p.slicesLength(), e); i++){
			Vector3 a = Vector3.fromFlatbuffer(p.slices(i - 1).physics().location());
			Vector3 b = Vector3.fromFlatbuffer(p.slices(i).physics().location());
			drawLine3d(c, a.toFramework(), b.toFramework());
		}
	}

}
