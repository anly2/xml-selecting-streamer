package aanchev.xmlstreamer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

public class Expectations<E> {
	private List<E> expected;
	private int i = 0;


	@SuppressWarnings("unchecked")
	public Expectations(E... expected) {
		this(Arrays.asList(expected));
	}

	public Expectations(List<E> expected) {
		this.expected = expected;
	}


	public void a(E value) {
		E e = expected.get(i);

		assertEquals(e, value);
		i++;
	}

	public void allMet() {
		assertEquals("Not all expectations were met!", expected.size(), i);
	}
}