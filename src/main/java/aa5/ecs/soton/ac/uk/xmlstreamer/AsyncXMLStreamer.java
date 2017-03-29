package aa5.ecs.soton.ac.uk.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

import aa5.ecs.soton.ac.uk.xmlstreamer.selectors.Selector;

public class AsyncXMLStreamer extends BasicXMLStreamer {
	
	public static void main(String[] args) {
		AsyncXMLStreamer streamer = new AsyncXMLStreamer((XMLEventReader)null);
		
		streamer.fire("*", new Node("1"));
		Consumer<Element> a = streamer.on("*", e -> System.out.println("ELEMENT: "+e.getTag()));
		streamer.fire("*", new Node("2"));
		streamer.off(a);
		streamer.fire("*", new Node("3"));
	}

	/* Properties */

	private ActionMap<Consumer<Element>> actions = new ActionMap<>();
	private Selector.Compiler compiler = new Selector.Compiler();
	
	
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
	
	
	/* ActionMap Helper Class */
	
	protected class ActionMap<ACTION> extends HashMap<String, Set<ACTION>> {
		private Set<String> activated = new HashSet<>();
		
		public void add(String key, ACTION action) {
			if (!isActivated(key))
				activate(key);
				
			super.computeIfAbsent(key, k -> new LinkedHashSet<>())
				.add(action);
		}
		
		public boolean remove(String key, ACTION action) {
			Set<ACTION> actions = super.get(key);
			
			if (actions == null)
				return false;
			
			boolean result = actions.remove(action);
			
			if (actions.isEmpty())
				deactivate(key);
			
			return result;
		}
		
		
		protected boolean isActivated(String key) {
			return activated.contains(key);
		}
		
		protected void activate(String key) {
			Selector selector = compiler().compile(key);
			
			activated.add(key);
			selector.attach(AsyncXMLStreamer.this);
		}
		
		protected void deactivate(String key) {
			//detach selectors
			//	keep a reference to them?!
			//	don't detach a sub-selector if another action relies on it as activator
			activated.remove(key);
		}
	}
	
	
	/* Functionality */

	public Consumer<Element> on(String selector, Consumer<Element> action) {
		actions.add(selector, action);
		return action;
	}
	
	public void off(String selector, Consumer<Element> action) {
		actions.remove(selector, action);
	}
	
	public void off(Consumer<Element> action) {
		actions.forEach((selector, v) -> actions.remove(selector, action));
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
