package aanchev.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

import aanchev.xmlstreamer.selectors.Selector;

public class BlockingXMLStreamer extends AsyncXMLStreamer {

	/* Properties */	
	
	private SelectorRegistry selectors;
	
	
	/* Constructors */
	
	public BlockingXMLStreamer(File file) {
		super(file);
	}

	public BlockingXMLStreamer(InputStream inputStream) {
		super(inputStream);
	}

	public BlockingXMLStreamer(Reader reader) {
		super(reader);
	}

	public BlockingXMLStreamer(XMLEventReader iterator) {
		super(iterator);
	}

	
	/* Accessors */
	
	/**
	 * A Builder for registering / attaching Selectors.
	 */
	public SelectorRegistry selectors() {
		return this.selectors;
	}
	
	
	/* Hooks */


	@Override
	public Consumer<Element> on(String selector, Consumer<Element> action) {
		selectors().register(selector);
		return super.on(selector, action);
	}
	
	@Override
	public void off(String selector, Consumer<Element> action) {
//		Optional.ofNullable(selectors().asMap().get(selector))
//			.ifPresent(sel -> sel.detach(this));
//		super.off(selector, action);
	}
	
	
	/* SelectorRegistry - Helper Class */
	
	public class SelectorRegistry implements Iterable<String> {
		
		private Map<String, Selector> selectors;
		
		protected SelectorRegistry() {
			this.selectors = new HashMap<String, Selector>();
		}
		
		
		/* Accessors */
		
		protected Map<String, Selector> asMap() {
			return this.selectors;
		}

		
		/* Builder functionality */
		
		public SelectorRegistry register(String selector) {
//			if (selectors.containsKey(selector)) {
//				selectors.get(selector).attach(BlockingXMLStreamer.this); //may be compiled but detached
//				return this;
//			}
//			
//			Selector sel = compiler(selector);
//			sel.attach();
			
			return this;
		}
		
		
		/* Extra convenience */

		public Iterator<String> iterator() {
			return selectors.keySet().iterator();
		}
		
		public Set<String> keys() {
			return selectors.keySet();
		}
	}
}
