package aanchev.xmlstreamer;

import java.io.StringReader;

import org.junit.Test;

public class ContentSelectorsTest {

	@Test
	public void testTrimMatchingContentSelector() {
		String xml = String.join("\n",
			"<root>",
			"<book id='B1'>An Adventure into the Woods</book>",
			"<book id='B2'>",
			"	An Adventure into the Woods",
			"</book>",
			"<book id='B3'>Not An Adventure into the Woods</book>",
			"</root>"
		);
		SelectingReactiveXMLStreamer streamer = new SelectingReactiveXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1", "B2");
		streamer.on("book{An Adventure into the Woods}", e -> expect.a(e.getAttribute("id").toString()));
		streamer.drain();

		expect.allMet();
	}

	@Test
	public void testExactMatchingContentSelector() {
		String xml = String.join("\n",
			"<root>",
			"<book id='B1'>An Adventure into the Woods</book>",
			"<book id='B2'>",
			"	An Adventure into the Woods",
			"</book>",
			"<book id='B3'>Not An Adventure into the Woods</book>",
			"</root>"
		);
		SelectingReactiveXMLStreamer streamer = new SelectingReactiveXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1");
		streamer.on("book|An Adventure into the Woods|", e -> expect.a(e.getAttribute("id").toString()));
		streamer.drain();

		expect.allMet();
	}

	@Test
	public void testRegexMatchingContentSelector() {
		String xml = String.join("\n",
			"<root>",
			"<book id='B1'>An Adventure into the Woods</book>",
			"<book id='B2'>An Adventure</book>",
			"<book id='B3'>Not An Adventure into the Woods</book>",
			"</root>"
		);
		SelectingReactiveXMLStreamer streamer = new SelectingReactiveXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1", "B3");
		streamer.on("book/Adventure.*?Woods/", e -> expect.a(e.getAttribute("id").toString()));
		streamer.drain();

		expect.allMet();
	}
}
