package aa5.ecs.soton.ac.uk.xmlstreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.Document;
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

	
	/* Implementation */
	
	protected Document document;
	protected LinkedList<Element> parents = new LinkedList<>();
	protected List<XMLEvent> buffer = new LinkedList<>();
	
	public Element nextTag() {
		while (xmlIterator.hasNext()) {
			XMLEvent event;
			try {
				event = xmlIterator.nextEvent();
			}
			catch (XMLStreamException exc) {
				throw new XMLStreamerException(exc);
			}
			
			
			if (event.isStartDocument()) {
				try {
					document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					continue;
				}
				catch (ParserConfigurationException exc) {
					throw new IllegalStateException(exc);
				}
			}
			
			if (event.isStartElement() || event.isEndElement()) {
				Element element = asElement(event);
				buffer.clear();
				return element;
			}
			
			buffer.add(event);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected Element asElement(XMLEvent event) {
		if (event.isStartElement()) {
			StartElement start = event.asStartElement();
			Element element = document.createElement(start.getName().getLocalPart());
			
			start.getAttributes().forEachRemaining(a -> {
				Attribute atr = ((Attribute) a);
				element.setAttribute(atr.getName().getLocalPart(), atr.getValue());
			});
			
			return element;
		}
		
		if (event.isEndElement()) {
			//TODO consume buffer ?!?!
			return null;
		}
		
		return null;
	}
	
	
	public static class XMLStreamerException extends RuntimeException {
		public XMLStreamerException(XMLStreamException exc) {
			super(exc);
		}
	}
}
