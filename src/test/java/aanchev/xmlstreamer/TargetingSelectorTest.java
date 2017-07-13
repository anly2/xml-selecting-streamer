package aanchev.xmlstreamer;

import java.io.StringReader;

import org.junit.Test;

import aanchev.xmlstreamer.AsyncXMLStreamerTest.Expectations;

public class TargetingSelectorTest {

	/*
	@Test
	public void testTargetingCompile() {
		junit.framework.Assert.assertEquals("Reconstructed selector string is different than expected.",
				"$book b",
				aanchev.xmlstreamer.selectors.Selector.compilerFor(null)
					.compile("$book b"));

		junit.framework.Assert.assertEquals("Reconstructed selector string is different than expected.",
				"book$ b",
				aanchev.xmlstreamer.selectors.Selector.compilerFor(null)
					.compile("book$ b"));
	}
	//*/

	@Test
	public void testTargetingSelector() {
		String xml = String.join("\n",
			"<root>",
			"<book id='B1'>",
			"	<title>A</title>",
			"	<author>B</author>",
			"</book>",
			"<book id='B2'>",
			"	<title>C</title>",
			"	<author>D</author>",
			"</book>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1", "B2");
		streamer.on("$book>title+author", e -> expect.a(e.getAttribute("id").toString()));
		streamer.drain();

		//expect.allMet();
	}

	@Test
	public void testTargetingDirection() {
		String xml = String.join("\n",
			"<root>",
			"<div id='1'>",
			"	<div id='2'>",
			"		<div id='3'>",
			"			<b>A</b>",
			"		</div>",
			"	</div>",
			"</div>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("1", "3");
		streamer.on("$div b", e -> expect.a(e.getAttribute("id").toString()));
		streamer.on("div$ b", e -> expect.a(e.getAttribute("id").toString()));
		streamer.drain();

		//expect.allMet();
	}
}
