package aanchev.xmlstreamer;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.stream.XMLEventReader;

public class ElementStreamer extends SelectingReactiveXMLStreamer {

	private Iterator<Element> elements;


	/* Construction */

	public ElementStreamer(Iterable<Element> elements) {
		this(elements.iterator());
	}

	public ElementStreamer(Iterator<Element> elements) {
		super((XMLEventReader) null);
		this.elements = flatten(elements);
	}


	/* Flattening / In-Depth Iteration */

	protected Iterator<Element> flatten(Iterator<Element> elements) {
		return new DeepIterator(elements);
	}

	protected static class DeepIterator implements Iterator<Element> {
		private Deque<Iterator<Element>> walkers;

		public DeepIterator(Iterator<Element> elements) {
			this.walkers = new LinkedList<>();
			push(elements);
		}

		@Override
		public boolean hasNext() {
			return (!walkers.isEmpty() && walkers.peekFirst().hasNext());
		}

		@Override
		public Element next() {
			while (!walkers.isEmpty()) {
				Iterator<Element> deepest = walkers.peekLast();

				if (deepest.hasNext()) {
					Element next = deepest.next();

					if (!next.isClosed())
						push(next.getChildren().iterator());

					return next;
				}

				//else
				walkers.removeLast();
			}

			return null;
		}


		protected void push(Iterator<Element> elements) {
			walkers.addLast(new BoundaryIterator(elements));
		}
	}

	protected static class BoundaryIterator implements Iterator<Element> {

		private Iterator<Element> elements;
		private Element current = null;

		public BoundaryIterator(Iterator<Element> elements) {
			this.elements = elements;
		}


		@Override
		public boolean hasNext() {
			return ((current != null) || elements.hasNext());
		}

		@Override
		public Element next() {
			if (current == null) {
				current = elements.next();
				return open(current);
			}

			Element closed = close(current);
			current = null;
			return closed;
		}


		/* State Spoofing */

		private static Element open(Element element) {
			if (!element.isClosed())
				return element;

			return new OpenedElementProxy(element);
		}

		private static Element close(Element element) {
			if (!element.isClosed()) //something is wrong if element is not closed at this point
				throw new IllegalStateException("The Element was expected to be closed but was not. " + element);

			return element;
		}

		private static class OpenedElementProxy implements Element {
			private Element proto;

			public OpenedElementProxy(Element proto) {
				this.proto = proto;
			}

			@Override
			public boolean isClosed() {
				return false;
			}


			/* Proxy */

			@Override
			public String getTag() {
				return proto.getTag();
			}

			@Override
			public String getText() {
				return proto.getText();
			}

			@Override
			public Object getAttribute(String attr) {
				return proto.getAttribute(attr);
			}

			@Override
			public Collection<Element> getChildren() {
				return proto.getChildren();
			}
		}
	}


	/* Overridden functionality */

	@Override
	public boolean hasNext() {
		return elements.hasNext();
	}

	@Override
	public Element next() {
		return nextTag();
	}

	@Override
	public Element nextTag() {
		if (!elements.hasNext())
			return null;

		Element element = elements.next();

		fire(element, element.isClosed());

		return element;
	}
}
