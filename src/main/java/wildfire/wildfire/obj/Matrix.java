package wildfire.wildfire.obj;

import java.util.Arrays;

import wildfire.input.CarOrientation;
import wildfire.vector.Vector3;

/**
 * https://introcs.cs.princeton.edu/java/95linear/Matrix.java.html
 */

public class Matrix {

	private final int M; // Rows.
	private final int N; // Columns.
	private double[][] data; // M-by-N.

	// M-by-N empty matrix.
	public Matrix(int M, int N){
		this.M = M;
		this.N = N;
		data = new double[M][N];
	}

	// Matrix from 2D array.
	public Matrix(double[][] data){
		this.M = data.length;
		this.N = data[0].length;
		
		this.data = new double[M][N];
		for(int i = 0; i < M; i++){
			for(int j = 0; j < N; j++){
				this.data[i][j] = data[i][j];
			}
		}
	}

	// Copy constructor.
	public Matrix(Matrix A){
		this(A.data); 
	}

	public Matrix(CarOrientation car){
		this.M = 3;
		this.N = 3;
		
		this.data = new double[M][N];
		
		Vector3 forward = car.noseVector.normalized(), 
				left = car.rightVector.scaledToMagnitude(-1),
//				left = car.rightVector.scaledToMagnitude(1),
				up = car.roofVector.normalized();
		this.data[0] = new double[] {forward.x, left.x, up.x};
		this.data[1] = new double[] {forward.y, left.y, up.y};
		this.data[2] = new double[] {forward.z, left.z, up.z};
	}

	// N-by-N identity matrix.
	public static Matrix identity(int N){
		Matrix I = new Matrix(N, N);
		for(int i = 0; i < N; i++) I.data[i][i] = 1;
		return I;
	}

	// Transpose of the invoking matrix.
	public Matrix transpose(){
		Matrix A = new Matrix(N, M);
		for(int i = 0; i < M; i++){
			for(int j = 0; j < N; j++){
				A.data[j][i] = this.data[i][j];
			}
		}
		return A;
	}

	public Matrix plus(Matrix B){
		if(B.M != this.M || B.N != this.N) throw new RuntimeException("Illegal matrix dimensions.");
		Matrix C = new Matrix(M, N);
		for(int i = 0; i < M; i++){
			for(int j = 0; j < N; j++){
				C.data[i][j] = this.data[i][j] + B.data[i][j];
			}
		}
		return C;
	}

	public Matrix minus(Matrix B){
		if(B.M != this.M || B.N != this.N) throw new RuntimeException("Illegal matrix dimensions.");
		Matrix C = new Matrix(M, N);
		for(int i = 0; i < M; i++){
			for(int j = 0; j < N; j++){
				C.data[i][j] = this.data[i][j] - B.data[i][j];
			}
		}
		return C;
	}

	@Override
	public boolean equals(Object obj){
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		
		Matrix other = (Matrix)obj;
		if(M != other.M || N != other.N) return false;
		return Arrays.deepEquals(data, other.data);
	}

	public Matrix multiply(Matrix B){
		if(this.N != B.M) throw new RuntimeException("Illegal matrix dimensions.");
		Matrix C = new Matrix(this.M, B.N);
		for(int i = 0; i < C.M; i++){
			for(int j = 0; j < C.N; j++){
				for(int k = 0; k < this.N; k++) C.data[i][j] += (this.data[i][k] * B.data[k][j]);
			}
		}
		return C;
	}

	@Override
	public String toString(){
		String str = "";
		for(int i = 0; i < M; i++){
			str += Arrays.toString(this.data[i]);
			if(i + 1 < M) str += "\n";
		}
		return str;
	}
	
	public Vector3 dot(Vector3 v){
		if(this.M != 3 || this.N != 3) throw new RuntimeException("Illegal matrix dimensions.");
        return new Vector3(
        		this.data[0][0] * v.x + this.data[0][1] * v.y + this.data[0][2] * v.z,
        		this.data[1][0] * v.x + this.data[1][1] * v.y + this.data[1][2] * v.z,
        		this.data[2][0] * v.x + this.data[2][1] * v.y + this.data[2][2] * v.z
        		);
    }
	
}
