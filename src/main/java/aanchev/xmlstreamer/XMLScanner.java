package aanchev.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

//TODO: implement Closable for all Streamers

public class XMLScanner extends AsyncXMLStreamer {

	private Element found;
	private String lastQuery = null;
	private Consumer<Element> lastAction = null;


	/* Constructors */

	public XMLScanner(File file) {
		super(file);
	}

	public XMLScanner(InputStream inputStream) {
		super(inputStream);
	}

	public XMLScanner(Reader reader) {
		super(reader);
	}

	public XMLScanner(XMLEventReader iterator) {
		super(iterator);
	}


	/* Added functionality */

	/**
	 * Blocking!
	 */
	public Element next(String selector) {
		found = null;

		if (lastQuery == null || !lastQuery.equals(selector)) {
			clearLastQuery();
			lastAction = on(selector, e -> found = e);
			lastQuery = selector;
		}

		while (hasNext() && found == null)
			super.nextTag(); //## `this.next()` and `super.next()==nextDeclared()` also clear the last query

		return found;
	}

	private void clearLastQuery() {
		if (lastQuery != null)
			off(lastQuery, lastAction);

		lastQuery = null;
		lastAction = null;
	}


	/* Clear Last Query on any other seek */

	@Override
	public Element nextSibling() {
		clearLastQuery();
		return super.nextSibling();
	}

	@Override
	public Element nextDeclared() {
		clearLastQuery();
		return super.nextDeclared();
	}

	@Override
	public Element nextTag() {
		clearLastQuery();
		return super.nextTag();
	}

	@Override
	public Element next() {
		clearLastQuery();
		return super.next();
	}

	@Override
	public void drain() {
		clearLastQuery();
		super.drain();
	}
}
