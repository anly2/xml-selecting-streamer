package aa5.ecs.soton.ac.uk.xmlstreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;

public class BasicXMLStreamer implements Iterable<Element>, Iterator<Element> {
	
	/* Properties */
	
	private XMLEventReader xmlIterator;
	
	
	/* Constructors */
	
	public BasicXMLStreamer(XMLEventReader iterator) {
		this.xmlIterator = iterator;
	}
	
	public BasicXMLStreamer(InputStream inputStream) {
		this(createEventReader(inputStream));
	}

	public BasicXMLStreamer(Reader reader) {
		this(createEventReader(reader));
	}
	
	public BasicXMLStreamer(File file) {
		this(createEventReader(file));
	}

	
	/* Stateless Initializers */
	
	private static XMLEventReader createEventReader(InputStream inputStream) {
		try {
			return XMLInputFactory.newInstance().createXMLEventReader(inputStream);
		}
		catch (XMLStreamException | FactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
	}
	
	private static XMLEventReader createEventReader(Reader reader) {
		try {
			return XMLInputFactory.newInstance().createXMLEventReader(reader);
		}
		catch (XMLStreamException | FactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
	}
	
	private static XMLEventReader createEventReader(File file) {
		try {
			return createEventReader(new FileReader(file));
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/* Accessors */
	
	public XMLEventReader getXMLEventReader() {
		return this.xmlIterator;
	}
	
	
	/* Iterator Contract */

	public Iterator<Element> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return xmlIterator.hasNext();
	}

	@Override
	public Element next() {
		return nextDeclared();
	}

	
	/* Functionality */
	
	public Element nextDeclared() {
		return null; //TODO
	}
	
	public Element nextSibling() {
		return null; //TODO
	}
}
