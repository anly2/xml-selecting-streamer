package aanchev.xmlstreamer;

import java.io.File;
import java.util.Scanner;

import junit.framework.TestCase;

public class AppTest extends TestCase {

	public void testXMLStream() {
		File file = new File("D:/Work/Java/nlp-data/elderscrolls_pages_current.xml");
		AsyncXMLStreamer xmlstreamer = new AsyncXMLStreamer(file);

		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);

		xmlstreamer.on("page>title", e -> {
//		xmlstreamer.on("page>title$~ns{0}", e -> {
			System.out.println(e.getTag() + "{" + e.getText() + "}");
			in.nextLine();
		});

		System.out.println("Reading: "+file);
		xmlstreamer.drain();
	}
}
