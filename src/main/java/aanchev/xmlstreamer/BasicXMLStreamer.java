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
	protected int depthTracked = -1;


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


	public boolean keepChildren() {
		return (depthTracked >= 0);
	}

	public void keepChildren(boolean shouldKeep) {
		depthTracked = shouldKeep? keepChildren()? Math.min(depthTracked, open.size()) : open.size() : -1;
	}


	public boolean keepAllChildren() {
		return (depthTracked == 0);
	}

	public void keepAllChildren(boolean shouldKeep) {
		depthTracked = shouldKeep? 0 : -1;
	}


	/* Iterator Contract */

	@Override
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
		keepChildren(true);

		Element element;
		while ((element = nextDeclared()) != null) {
			if (element.isClosed() && open.size() == depth) {
				keepChildren(false);
				open.peek().children.remove(element); //because here is one step too late to disable `attachChildren`
				return element;
			}
		}

		keepChildren(false);
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

				if (keepChildren() && !open.isEmpty())
					open.peek().appendChild(element);

				open.push(element);
				return element;
			}

			if (event.isEndElement()) {
				int depth = open.size();

				Node element = open.pop();
				element.close();

//				if (keepChildren() && !open.isEmpty())
//					open.peek().appendChild(element);

				if (depth == depthTracked)
					keepChildren(false);

				return element;
			}

			if (event.isCharacters()) {
				if (keepChildren())
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

		@Override
		public String getTag() {
			return this.tag;
		}

		/**
		 * POTENTIALLY BLOCKING!!!
		 */
		@Override
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
		@Override
		public Collection<Element> getChildren() {
			if (!isClosed())
				doComplete.run(); //auto closes the element; BLOCKING!!!

			return this.children;
		}

		void appendChild(Element child) {
			this.children.add(child);
		}


		@Override
		public Object getAttribute(String attr) {
			return this.attributes.get(attr);
		}

		void setAttribute(String attr, Object value) {
			this.attributes.put(attr, value);
		}


		@Override
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

		@Override
		public String getText() {
			return this.text;
		}


		@Override
		public String getTag() {
			return "";
		}

		@Override
		public Object getAttribute(String attr) {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Collection<Element> getChildren() {
			return Collections.EMPTY_SET;
		}

		@Override
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

		final int elementDepth = open.size() + 1;
		element.doComplete = () -> {
			//manual "keepChildren(true)" because current depth would be improper
			if (depthTracked < 0 || depthTracked > elementDepth)
				depthTracked = elementDepth;

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
