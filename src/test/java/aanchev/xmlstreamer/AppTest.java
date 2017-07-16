package aanchev.xmlstreamer;

import java.io.File;
import java.util.Scanner;

import junit.framework.TestCase;

public class AppTest extends TestCase {

	public void experimentOnData() {
		File file = new File("D:/Work/Java/nlp-data/elderscrolls_pages_current.xml");
		AsyncXMLStreamer xmlstreamer = new AsyncXMLStreamer(file);

		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);

		int[] i = {0};
		xmlstreamer.on("page$>title~ns{0}", e -> {
			i[0]++;
			String title = e.find("title").getText();
			String text = e.find("text").getText();

			System.out.println(" == " + title + " == ");
			System.out.println(text);

			in.nextLine(); //pause
		});

		System.out.println("Reading: "+file);
		xmlstreamer.drain();
		System.out.println(i[0] + " pages matched");
	}

	public void experimentOnBlockingSource() {
		XMLScanner scanner = new XMLScanner(System.in);

		scanner.next("book+book");
		System.out.println(scanner.next("title").getText());
	}
}
