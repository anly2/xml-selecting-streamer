package aanchev.xmlstreamer.selectors;

import java.util.function.Consumer;

import aanchev.xmlstreamer.AsyncXMLStreamer;
import aanchev.xmlstreamer.Element;

public interface Selector {
	public void attach();
	public void detach();
	public String getSelector();
	public Consumer<Element> trigger(Consumer<Element> action);
	
	public interface Compiler {
		public Selector compile(CharSequence selector);
	}
	
	public static Compiler compilerFor(AsyncXMLStreamer streamer) {
		return new TargetingBasicSelectorParser(streamer);
	}
}