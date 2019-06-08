package wildfire.wildfire.utils;

public class Pair<T1, T2> {

	private final T1 one;
	private final T2 two;

	public Pair(T1 one, T2 two){
		this.one = one;
		this.two = two;
	}
	
	public T1 getOne(){
		return one;
	}

	public T2 getTwo(){
		return two;
	}

}
