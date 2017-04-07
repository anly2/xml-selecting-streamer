package aanchev.xmlstreamer;

import java.io.StringReader;

import org.junit.Test;

import aanchev.xmlstreamer.AsyncXMLStreamer;

public class AsyncXMLStreamerTest {

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
		
		System.out.println("Testing");
		streamer.on("book title", e -> System.out.println(e.getTag() + "|"+e.getText()+"|"));
		streamer.on("book>author", e -> System.out.println(e.getText()));
		streamer.drain();
	}

	@Test
	public void test2() {
		String xml = String.join("\n",
			"<root>",
			"<book>","</book>",
			"<book>","</book>",
			"<book>A","</book>",
			"",
			"<section>",
			"	<book><ref>B1</ref></book>",
			"</section>",
			"</root>"
		);
		AsyncXMLStreamer streamer = new AsyncXMLStreamer(new StringReader(xml));
		
		System.out.println("Testing");
		streamer.on("book~book", e -> System.out.println("-["+e.getText()+"]-"));
		streamer.drain();
	}

}
