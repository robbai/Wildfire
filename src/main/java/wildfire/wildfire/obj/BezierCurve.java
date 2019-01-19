package wildfire.wildfire.obj;

import java.awt.Color;
import java.util.ArrayList;

import rlbot.render.Renderer;
import wildfire.vector.Vector2;

public class BezierCurve {
	
	private Vector2[] points;

	public BezierCurve(Vector2... points){
		if(points == null || points.length < 2) return;
		this.points = points;
	}
	
	public Vector2 atTime(double t){
		ArrayList<Vector2> p = new ArrayList<Vector2>();
		
		//Fixed points
		for(Vector2 point : this.points) p.add(point);
		
		//Loop to find the final point
		while(true){
			ArrayList<Vector2> newP = new ArrayList<Vector2>();
			for(int i = 0; i < (p.size() - 1); i++){
				newP.add(t(p.get(i), p.get(i + 1), t));
			}
			if(newP.size() > 1){
				p.clear();
				for(Vector2 point : newP) p.add(point);
			}else{
				return newP.get(0);
			}
		}
	}	
	
	private Vector2 t(Vector2 one, Vector2 two, double t){
		return one.plus(two.minus(one).scaled(t));
	}
	
	public void render(Renderer r, Color colour){
		//Draw fixed lines
//		for(int i = 0; i < (this.points.length - 1); i++){
//			r.drawLine3d(Color.WHITE, this.points[i].toFramework(), this.points[i + 1].toFramework());
//		}
		
		//Draw curve
		Vector2 last = null;
		for(double t = 0; t <= 1; t += 0.01){
			Vector2 next = this.atTime(t);
			if(last != null) r.drawLine3d(colour, last.toFramework(), next.toFramework());
			last = next;
		}
	}

}
