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
		
		System.out.println("Testing 1");
		streamer.on("book title", e -> System.out.println(e.getTag() + "|"+e.getText()+"|"));
		streamer.on("book>author", e -> System.out.println(e.getText()));
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
		
		System.out.println("Testing 2");
		streamer.on("book~book", e -> System.out.println("-"+e.getText()+"-"));
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
		
		System.out.println("Testing 3");
		streamer.on("book+book", e -> System.out.println("-"+e.getText()+"-"));
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
		
		System.out.println("Testing 4");
		streamer.on("book+book:before", e -> System.out.println("-"+1+"-"));
		streamer.on("book+book:after", e -> System.out.println("-"+2+"-"));
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
		
		System.out.println("Testing 5");
		streamer.on("book+:not(book)", e -> System.out.println("-"+e.getText()+"-"));
		streamer.drain();
	}
}
