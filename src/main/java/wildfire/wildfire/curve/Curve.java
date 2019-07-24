package wildfire.wildfire.curve;

import wildfire.vector.Vector2;
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
	
}
