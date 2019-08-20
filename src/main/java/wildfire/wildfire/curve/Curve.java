package wildfire.wildfire.curve;

import java.awt.Color;

import wildfire.vector.Vector2;
import wildfire.wildfire.obj.WRenderer;
import wildfire.wildfire.utils.Utils;

public abstract class Curve {

	public abstract Vector2 T(double t);
	
	public abstract double getLength();
	
	public Vector2[] discretise(int n){
		Vector2[] d = new Vector2[n];
		for(int i = 0; i < n; i++) d[i] = this.T((double)i / (n - 1));
		return d;
	}
	
	public Vector2 S(double s){
		return this.T(Utils.clamp(s, 0, 1) / this.getLength());
	}
	
	public void render(WRenderer renderer, Color colour, int n){
		if(n < 2) return;
		Vector2[] points = this.discretise(n);
		for(int i = 0; i < (points.length - 1); i++){
			Vector2 a = points[i], b = points[Math.min(points.length - 1, i + 1)];
			renderer.drawLine3d(colour, a, b);
		}
	}
	
}
