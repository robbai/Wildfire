package wildfire.wildfire.pitch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ReadableObj;
import wildfire.Main;
import wildfire.vector.Vector3;
import wildfire.wildfire.obj.Pair;
import wildfire.wildfire.utils.Constants;

public class Pitch {
	
	/**
	 * An array of all the pitch triangles
	 */
	private static Triangle[] triangles;

	@SuppressWarnings("unused")
	public static void main(String[] args){
		initTriangles();

		Random r = new Random();
		
		long startTime = System.currentTimeMillis();
		int iterations = 10000;
		for(int i = 0; i < iterations; i++){
			Vector3 start = new Vector3(0, 0, Constants.CEILING / 2);
			Vector3 end = new Vector3(r.nextDouble() - 0.5, 
					r.nextDouble() - 0.5, 
					r.nextDouble() - 0.5).scaledToMagnitude(5000).plus(start);
			
//			System.out.println("Start: " + start.toString());
//			System.out.println("End: " + end.toString());
			
			Pair<Triangle, Vector3> intersect = segmentIntersect(start, end);
//			if(intersect != null){
//				System.out.println("Intersect: " + intersect.getTwo().toString());
//			}else{
//				System.out.println("No intersect");
//			}
//			System.out.println();
		}
		double ps = (double)iterations / (double)(System.currentTimeMillis() - startTime) * 1000D;
		System.out.println(ps + " calls per second");
		System.out.println(1D / ps * 1000 + "ms per call");
	}

	public static boolean initTriangles(){
		InputStream inputStream;
		try {
			URL url = Main.class.getClassLoader().getResource("pitch.obj");
			inputStream = url.openStream();
			ReadableObj obj = ObjReader.read(inputStream);
			
			// Create a sorted list.
			ArrayList<Triangle> list = new ArrayList<Triangle>(obj.getNumFaces());
			for(int index = 0; index < obj.getNumFaces(); index++){
				list.add(index, new Triangle(obj, obj.getFace(index)));
			}
			Collections.sort(list, Collections.reverseOrder()); // Sort by area.
			
			// Create a nice array.
			triangles = new Triangle[obj.getNumFaces()];
			for(int index = 0; index < list.size(); index++){
				triangles[index] = list.get(index);
			}
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Vertices use a different coordinate system to vectors
	 */
	public static Vector3 toVector(FloatTuple vertex){
		return new Vector3(vertex.getY(), vertex.getX(), vertex.getZ());
	}
	
	public static Triangle rayIntersect(Vector3 start, Vector3 direction){
		for(int index = 0; index < triangles.length; index++){
			Triangle triangle = triangles[index];
			if(triangle.rayIntersects(start, direction)) return triangle;
		}
		return null;
	}
	
	public static Pair<Triangle, Vector3> segmentIntersect(Vector3 start, Vector3 end){
//		long startTime = System.nanoTime();
		for(int index = 0; index < triangles.length; index++){
			Triangle triangle = triangles[index];
			Vector3 intersect = triangle.segmentIntersects(start, end);
			if(intersect != null){
//				System.out.println((double)(System.nanoTime() - startTime) / 1000000D + "ms");
				return new Pair<Triangle, Vector3>(triangle, intersect);
			}
		}
//		System.out.println((double)(System.nanoTime() - startTime) / 1000000D + "ms (None)");
		return null;
	}
	
	public static Triangle[] getTriangles(){
		return triangles;
	}

}
