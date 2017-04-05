package aanchev.utils;

import java.util.function.Function;

public class LambdaUtils {
	
	public static <V> V let(V value, Function<V, V> use) {
		if (value == null)
			return null;
		
		return use.apply(value);
	}
	
	
	@SuppressWarnings("unchecked")
	public static <V> Result<V> use(V value, Function<V, V> use) {
		if (value == null)
			return Result.EMPTY;
		
		return new Result<>(use.apply(value));
	}
	
	public static class Result<V> {
		public final V value;

		public Result(V value) {
			this.value = value;
		}
		
		@SuppressWarnings("unchecked")
		public <E> Result<E> then(Function<V,E> use) {
			if (value == null)
				return EMPTY;
			
			return new Result<>(use.apply(value));
		}
		
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static final Result EMPTY = new Result(null);
	}
}
