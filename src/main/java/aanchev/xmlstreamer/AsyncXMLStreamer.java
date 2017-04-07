package aanchev.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

import aanchev.xmlstreamer.selectors.Selector;

public class AsyncXMLStreamer extends BasicXMLStreamer {
	
	public static void main(String[] args) {
		/*
		AsyncXMLStreamer streamer = new AsyncXMLStreamer((XMLEventReader)null);
		
		streamer.fire("*", new Node("1"));
		Consumer<Element> a = streamer.on("*", e -> System.out.println("ELEMENT: "+e.getTag()));
		streamer.fire("*", new Node("2"));
		streamer.off(a);
		streamer.fire("*", new Node("3"));
		*/
	}

	/* Properties */

	private Set<Consumer<Element>> actionsOpen  = new LinkedHashSet<>();
	private Set<Consumer<Element>> actionsClose = new LinkedHashSet<>();
	private Map<Consumer<Element>, Set<Selector>> activators = new HashMap<>();
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
	
	protected Selector compile(String selector) {
		return this.compiler.compile(selector);
	}
	
	
	/* Rudimentary Functionality */
	
	public Consumer<Element> onTagStart(Consumer<Element> action) {
		actionsOpen.add(action);
		return action;
	}
	
	public boolean offTagStart(Consumer<Element> action) {
		return actionsOpen.remove(action);
	}
	
	
	public Consumer<Element> onTagEnd(Consumer<Element> action) {
		actionsClose.add(action);
		return action;
	}
	
	public boolean offTagEnd(Consumer<Element> action) {
		return actionsClose.remove(action);
	}
	
	
	/* Functionality */

	public Consumer<Element> on(String selector, Consumer<Element> action) {
		Selector sel = compile(selector);
		
		sel.trigger(action);
		sel.attach();
		
		activators
			.computeIfAbsent(action, k -> new LinkedHashSet<>())
			.add(sel);
		
		return action;
	}

	public void off(Consumer<Element> action) {
		activators.computeIfPresent(action, (a, sels) -> {
			sels.forEach(Selector::detach);
			return null;
		});
	}
	
	public void off(String selector, Consumer<Element> action) {
		if (!activators.containsKey(action))
			return;
		
		if (!remove(selector, action))
			remove(compile(selector).getSelector(), action);
	}

	private boolean remove(String selector, Consumer<Element> action) {
		boolean found = false;
		
		Iterator<Selector> selsIt = activators.get(action).iterator();
		while (selsIt.hasNext()) {
			Selector s = selsIt.next();
			
			if (!s.getSelector().equals(selector))
				continue;
			
			s.detach();
			selsIt.remove();
			found = true;
		}
		
		return found;
	}
	
	
	/* Hooks */
	
	public Element nextTag() {
		Element element = super.nextTag();
		
		if (element == null)
			return null;
		
		fire(element, element.isClosed());
		
		return element;
	}
	
	private void fire(Element element, boolean closes) {
		Set<Consumer<Element>> actions = closes? actionsClose : actionsOpen;
		actions.forEach(a -> a.accept(element));
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
