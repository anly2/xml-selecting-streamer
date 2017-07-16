package aanchev.xmlstreamer;

import java.io.StringReader;

import org.junit.Test;

import aanchev.xmlstreamer.AsyncXMLStreamerTest.Expectations;

public class XMLScannerTest {

	@Test
	public void testScanningIteration() {
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
			"<book id='B3'>",
			"	<title>E</title>",
			"	<author>F</author>",
			"</book>",
			"<book id='B4'>",
			"	<title>G</title>",
			"	<author>H</author>",
			"</book>",
			"</root>"
		);
		XMLScanner scanner = new XMLScanner(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1", "B2", "B3", "B4");

		Element e;
		while ((e = scanner.next("book")) != null)
			expect.a(e.getAttribute("id").toString());

		expect.allMet();
	}

	@Test
	public void testAbruptIteration() {
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
			"<book id='B3'>",
			"	<title>E</title>",
			"	<author>F</author>",
			"</book>",
			"<book id='B4'>",
			"	<title>G</title>",
			"	<author>H</author>",
			"</book>",
			"</root>"
		);
		XMLScanner scanner = new XMLScanner(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>("B1", "B2", "B3");

		for (int i=0; i<3; i++)
			expect.a(scanner.next("book").getAttribute("id").toString());

		expect.allMet();
	}

	@Test
	public void testInterwovenScans() {

		String xml = String.join("\n",
			"<root>",
			"<book id='B1'>",
			"	<title>A</title>",
			"	<author>B</author>",
			"</book>",
			"<book id='B2' audiobook=\"true\">",
			"	<title>C</title>",
			"	<author>D</author>",
			"</book>",
			"<book id='B3'>",
			"	<title>E</title>",
			"	<author>F</author>",
			"</book>",
			"<book id='B4' audiobook=\"true\">",
			"	<title>G</title>",
			"	<author>H</author>",
			"</book>",
			"<book id='B5'>",
			"	<title>I</title>",
			"	<author>J</author>",
			"</book>",
			"<book id='B6' audiobook=\"true\">",
			"	<title>K</title>",
			"	<author>L</author>",
			"</book>",
			"<book id='B7'>",
			"	<title>M</title>",
			"	<author>N</author>",
			"</book>",
			"<book id='B8' audiobook=\"true\">",
			"	<title>O</title>",
			"	<author>P</author>",
			"</book>",
			"<book id='B9'>",
			"	<title>Q</title>",
			"	<author>R</author>",
			"</book>",
			"</root>"
		);
		XMLScanner scanner = new XMLScanner(new StringReader(xml));

		final Expectations<String> expect = new Expectations<>(
				"B1", "B2", "B3",  //selector: "book"
				"B4", "B6",		   //selector: "book[audiobook]"
				"B7", "B8", "B9"); //selector: "book"

		for (int i=0; i<3; i++)
			expect.a(scanner.next("book").getAttribute("id").toString());

		for (int i=0; i<2; i++)
			expect.a(scanner.next("book[audiobook]").getAttribute("id").toString());

		for (int i=0; i<3; i++)
			expect.a(scanner.next("book").getAttribute("id").toString());

		expect.allMet();
	}
}
