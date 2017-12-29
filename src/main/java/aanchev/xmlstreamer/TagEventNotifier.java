package aanchev.xmlstreamer;

import java.util.function.Consumer;

public interface TagEventNotifier {

	public Consumer<Element> onTagStart(Consumer<Element> action);
	public Consumer<Element> onTagEnd(Consumer<Element> action);

	public boolean offTagStart(Consumer<Element> action);
	public boolean offTagEnd(Consumer<Element> action);

}
