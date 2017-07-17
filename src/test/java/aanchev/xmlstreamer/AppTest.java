package aanchev.xmlstreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class AppTest extends TestCase {

	// ~ 4.3 sec (no sub-find)
	// ~ 5.1 sec (both sub-finds, no prints)
	// ~ 6.1 sec (both sub-finds, only title print)
	// ~ 12  sec (both sub-finds, both prints)
	public void testOnData() {
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


	// ~ 3.1 sec (raw xmlIterator)
	public void checkSpeedOnRawRead() {
		File file = new File("D:/Work/Java/nlp-data/elderscrolls_pages_current.xml");

		XMLEventReader xmlIterator;
		try {
			 xmlIterator = XMLInputFactory.newInstance().createXMLEventReader(new FileReader(file));
		}
		catch (XMLStreamException | FactoryConfigurationError | FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			while (xmlIterator.hasNext())
				xmlIterator.nextEvent();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	// ~ 3.6 sec (XML wrappers)
	public void checkSpeedOnDryRead() {
		File file = new File("D:/Work/Java/nlp-data/elderscrolls_pages_current.xml");
		AsyncXMLStreamer xmlstreamer = new AsyncXMLStreamer(file);
		xmlstreamer.drain();
	}
}
