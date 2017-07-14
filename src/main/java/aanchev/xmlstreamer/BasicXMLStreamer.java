package aanchev.xmlstreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class BasicXMLStreamer implements Iterable<Element>, Iterator<Element> {
	
	/* Properties */
	
	private XMLEventReader xmlIterator;

	protected LinkedList<Node> open = new LinkedList<>();
	
	
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
	
	public Element nextSibling() {
		int depth = open.size();
		Element element;
		while ((element = nextDeclared()) != null) {
			if (element.isClosed() && open.size() == depth)
				return element;
		}
		return null;
	}
	
	public Element nextDeclared() {
		Element element;
		while ((element = nextTag()) != null) {
			if (element.isClosed())
				return element;
		}
		return null;
	}

	public Element nextTag() {
		while (xmlIterator.hasNext()) {
			XMLEvent event;
			try {
				event = xmlIterator.nextEvent();
			}
			catch (XMLStreamException exc) {
				throw new XMLStreamerException(exc);
			}
			

			if (event.isStartElement()) {
				Node element = asElement(event);
				open.push(element);
				return element;
			}
			
			if (event.isEndElement()) {
				Node element = open.pop();
				element.close();
				
				if (!open.isEmpty())
					open.peek().appendChild(element);
					
				return element;
			}
			
			if (event.isCharacters()) {
				open.peek().appendChild(new Text(event.asCharacters().getData()));
			}
		}
		return null;
	}
	
	
	/* Element Classes */
	
	protected static class Node implements Element {
		private final String tag;
		private Map<String, Object> attributes = new HashMap<>();
		private List<Element> children = new LinkedList<>();
		private Runnable doComplete = null;

		
		public Node(String tag) {
			this.tag = tag;
		}
		
		public String getTag() {
			return this.tag;
		}
		
		/**
		 * POTENTIALLY BLOCKING!!!
		 */
		public String getText() {
			if (!isClosed())
				doComplete.run(); //auto closes the element; BLOCKING!!!

			StringBuilder sb = new StringBuilder();
			for (Element child : children) {
				sb.append(child.getText());
				sb.append(" ");
			}

			if (!children.isEmpty())
				sb.deleteCharAt(sb.length()-1);

			return sb.toString();
		}
		
		
		/**
		 * POTENTIALLY BLOCKING!!!
		 */
		public Collection<Element> getChildren() {
			if (!isClosed())
				doComplete.run(); //auto closes the element; BLOCKING!!!

			return this.children;
		}

		void appendChild(Element child) {
			this.children.add(child);
		}

		
		public Object getAttribute(String attr) {
			return this.attributes.get(attr);
		}

		void setAttribute(String attr, Object value) {
			this.attributes.put(attr, value);
		}


		public boolean isClosed() {
			return this.doComplete == null;
		}
		
		void close() {
			//## dont execute doComplete - it is probably already done at this point
			this.doComplete = null;
		}
	}

	protected static class Text implements Element {
		private final String text;
		
		public Text(String text) {
			this.text = text;
		}
		
		public String getText() {
			return this.text;
		}
		
		
		public String getTag() {
			return "";
		}
		
		public Object getAttribute(String attr) {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		public Collection<Element> getChildren() {
			return Collections.EMPTY_SET;
		}
		
		public boolean isClosed() {
			return true;
		}
	}

	
	
	/* Helper Methods */
	
	@SuppressWarnings("unchecked")
	protected Node asElement(XMLEvent event) {
		if (!event.isStartElement())
			return null;
		
		StartElement start = event.asStartElement();
		Node element = new Node(start.getName().getLocalPart());
		
		start.getAttributes().forEachRemaining(a -> {
			Attribute atr = ((Attribute) a);
			element.setAttribute(atr.getName().getLocalPart(), atr.getValue());
		});
		
		element.doComplete = () -> {
			Element e;
			while ((e = nextTag()) != null) {
				if (e == element) {
					element.close(); //## obsolete, but can save from trouble when doComplete is (tried to be) repeatedly called
					return;
				}
			}
		};

		return element;
	}
	
	
	/* Exceptions */
	
	public static class XMLStreamerException extends RuntimeException {
		public XMLStreamerException(XMLStreamException exc) {
			super(exc);
		}
	}
}
