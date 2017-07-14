package aanchev.xmlstreamer;

import java.util.Collection;

public interface Element {
	public String getTag();
	public String getText();
	public Object getAttribute(String attr);
	public Collection<Element> getChildren();
	public boolean isClosed();


	/* Extensions */

	default Element find(String query) {
		ElementStreamer substream = new ElementStreamer(getChildren());
		Element[] refElement = { null };
		substream.on(query, e -> refElement[0] = e);

		while (substream.hasNext() && refElement[0] == null)
			substream.next();

		return refElement[0];
	}

	default Collection<Element> findAll(String query) {
		ElementStreamer substream = new ElementStreamer(getChildren());
		Collection<Element> elements = new java.util.LinkedList<>();
		substream.on(query, elements::add);
		substream.drain();
		return elements;
	}

}