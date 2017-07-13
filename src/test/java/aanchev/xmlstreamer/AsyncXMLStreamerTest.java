package aanchev.xmlstreamer;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AsyncXMLStreamerTest {

	public static class Expectations<E> {
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



	@Test
	public void test1() {
		String xml = String.join("\n",
			"<root>",
			"<title>Index</title>",
			"<author>Librarian</author>",
			"",
			"<fodder>Lorem</fodder>",
			"",
			"<book>",
			"	<title>B1</title>",
			"	<author>A1</author>",
			"</book>",
			"<book>",
			"	<title>B2</title>",
			"	<author>A2</author>",
			"</book>",
			"",
			"<section>",
			"	<title>Rented</title>",
			"	<book><ref>B1</ref></book>",
			"</section>",
			"",
			"<section>",
			"	<book><ref><title>B2</title></ref></book>",
			"	<author>Misplaced</author>",
			"</section>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("title|B1", "A1", "title|B2", "A2", "title|B2");
		streamer.on("book title", e -> expect.a(e.getTag() + "|"+e.getText()));
		streamer.on("book>author", e -> expect.a(e.getText()));
		streamer.drain();
	}

	@Test
	public void test2() {
		String xml = String.join("\n",
			"<root>",
			"<book>A</book>",
			"<book>1</book>",
			"<book>2</book>",
			"",
			"<section>",
			"	<book><ref>B</ref></book>",
			"</section>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("1", "2");
		streamer.on("book~book", e -> expect.a(e.getText()));
		streamer.drain();
	}


	@Test
	public void test3() {
		String xml = String.join("\n",
			"<root>",
			"<book>A</book>",
			"<book>1</book>",
			"<book>2</book>",
			"",
			"<section>",
			"	<book>B</book><br/><book>C</book>",
			"</section>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("1", "2");
		streamer.on("book+book", e -> expect.a(e.getText()));
		streamer.drain();
	}

	@Test
	public void test4() {
		String xml = String.join("\n",
			"<root>",
			"<book>A</book>",
			"<book>B</book>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<Integer> expect = new Expectations<>(1, 2);
		streamer.on("book+book:before", e -> expect.a(1));
		streamer.on("book+book:after", e -> expect.a(2));
		streamer.drain();
	}

	@Test
	public void test5() {
		String xml = String.join("\n",
			"<root>",
			"<book>A</book>",
			"<book>B</book>",
			"<p>1</p>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("1");
		streamer.on("book+:not(book)", e -> expect.a(e.getText()));
		streamer.drain();
	}
}
