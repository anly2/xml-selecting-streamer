package aanchev.xmlstreamer;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;

import aanchev.utils.Pair;
import aanchev.xmlstreamer.selectors.Selector;

public class AsyncXMLStreamer extends BasicXMLStreamer implements TagEventNotifier {

	/* Properties */

	private Set<Consumer<Element>> actionsOpen  = new LinkedHashSet<>();
	private Set<Consumer<Element>> actionsClose = new LinkedHashSet<>();
	private Map<Consumer<Element>, Set<Selector>> activators = new HashMap<>();
	private Selector.Compiler compiler = Selector.compilerFor(this);

	private Deque<Pair<Element, Boolean>> events = new LinkedList<>();
	private boolean processingEvent = false;


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


	/* TagEventNotifier contract / Base Async Functionality */

	@Override
	public Consumer<Element> onTagStart(Consumer<Element> action) {
		actionsOpen.add(action);
		return action;
	}

	@Override
	public boolean offTagStart(Consumer<Element> action) {
		return actionsOpen.remove(action);
	}


	@Override
	public Consumer<Element> onTagEnd(Consumer<Element> action) {
		actionsClose.add(action);
		return action;
	}

	@Override
	public boolean offTagEnd(Consumer<Element> action) {
		return actionsClose.remove(action);
	}


	/* Functionality */

	public Consumer<Element> on(String selector, Consumer<Element> action) {
		Selector sel = compile(selector);

		sel.triggers(e -> {
			if (!e.isClosed())
				keepChildren(true);

			action.accept(e);
		});
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

	@Override
	public Element nextTag() {
		Element element = super.nextTag();

		if (element == null)
			return null;

		fire(element, element.isClosed());

		return element;
	}

	protected synchronized void fire(Element element, boolean closes) {
		if (processingEvent) {
			events.push(new Pair<>(element, closes));
			return;
		}

		processingEvent = true;
		Set<Consumer<Element>> actions = closes? actionsClose : actionsOpen;
		actions.forEach(a -> a.accept(element));

		processingEvent = false;
		Pair<Element, Boolean> event = events.pollLast();
		if (event == null) return;
		fire(event.left, event.right);
	}


	/* Terminal operations */

	/**
	 * BLOCKING!
	 */
	public void drain() {
		while (hasNext())
			next();
	}
}
