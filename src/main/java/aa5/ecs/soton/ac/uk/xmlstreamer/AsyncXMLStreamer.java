package aa5.ecs.soton.ac.uk.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

import aa5.ecs.soton.ac.uk.xmlstreamer.selectors.Selector;

public class AsyncXMLStreamer extends BasicXMLStreamer {

	/* Properties */

	private Map<String, Set<Consumer<Element>>> actions = new HashMap<>();
	private Selector.Compiler compiler = new Selector.Compiler(this);
	
	
	/* Constructors */
	
	public AsyncXMLStreamer(File file) {
		super(file);
	}

	public AsyncXMLStreamer(InputStream inputStream) {
		super(inputStream);
	}

	public AsyncXMLStreamer(Reader reader) {
		super(reader);
	}

	public AsyncXMLStreamer(XMLEventReader iterator) {
		super(iterator);
	}
	
	
	/* Accessors */
	
	protected Selector.Compiler compiler() {
		return this.compiler;
	}
	
	
	/* Functionality */

	public void on(String selector, Consumer<Element> action) {
		Selector sel = compiler().compile(selector);
		
		Set<Consumer<Element>> acts = actions.computeIfAbsent(sel.getSelector(), k -> new LinkedHashSet<>());
		
		if (acts.contains(action))
			return;
			
		sel.attach();
		acts.add(action);
	}
	
	public void off(String selector, Consumer<Element> action) {
		Optional.ofNullable(actions.get(compiler().compile(selector)))
			.ifPresent(actions -> actions.remove(action));
	}
	
	public void off(Consumer<Element> action) {
		actions.forEach((selector, actions) -> actions.remove(action));
	}
	
	
	protected void fire(String selector, Element element) {
		Set<Consumer<Element>> acts = actions.get(selector);
		if (acts != null)
			acts.forEach(action -> action.accept(element));
	}
	
	
	/* Hooks */
	
	public Element nextTag() {
		Element element = super.nextTag();
		
		if (element == null) return null;
		
		if (element.isClosed())
			fire("*:after", element);
		else
			fire("*:before", element);
		
		return element;
	}
	

	
	/* Terminal operations */
	
	/**
	 * BLOCKING!
	 */
	public void drain() {
		while (super.hasNext())
			super.next();
	}
}
