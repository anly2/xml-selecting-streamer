package aanchev.xmlstreamer.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import aanchev.parser.SimpleParser;
import aanchev.parser.SimpleParser.AST;
import aanchev.xmlstreamer.AsyncXMLStreamer;
import aanchev.xmlstreamer.Element;

public abstract class AbstractSelectorProvider implements Selector.Compiler.BuilderDecorator {

	/* Bound Properties */

	protected AsyncXMLStreamer streamer;


	/* Constructors */

	public AbstractSelectorProvider(AsyncXMLStreamer streamer) {
		this.streamer = streamer;
	}


	/* ParserDecorator Contract */

	@Override
	public abstract void decorate(SimpleParser.Builder builder);

	@Override
	public Selector.Compiler intercept(Selector.Compiler result) {
		return result;
	}


	/*** Selector.Factory ***/

	/* Selector types */

	protected abstract class SelectorNode extends AST.Node implements Selector {
		public String raw;
		protected Consumer<Element> action;

		public SelectorNode(String raw, int childrenCount) {
			super(array(childrenCount));
			this.raw = raw;
		}

		@Override
		public String getSelector() {
			return this.raw;
		}

		@Override
		public String toString() {
			return getSelector();
		}


		@Override
		public Consumer<Element> trigger(Consumer<Element> action) {
			this.action = action;
			return action;
		}
	}

	protected abstract class SelectorLeaf extends AST.Leaf implements Selector, Consumer<Element> {
		public String raw;
		protected Consumer<Element> action;

		public SelectorLeaf(String raw) {
			this.raw = raw;
		}

		@Override
		public String getSelector() {
			return this.raw;
		}

		@Override
		public String toString() {
			return getSelector();
		}


		@Override
		public Consumer<Element> trigger(Consumer<Element> action) {
			this.action = action;
			return action;
		}


		@Override
		public void attach() {
			streamer.onTagStart(this);
		}

		@Override
		public void detach() {
			streamer.offTagStart(this);
		}
	}


	protected abstract class BinarySelectorNode extends SelectorNode {
		public BinarySelectorNode(String raw) {
			super(raw, 2);
		}

		@Override
		public String getSelector() {
			return getChild(0) + super.getSelector() + getChild(1);
		}
	}


	/* Helper methods */

	private static <E> List<E> array(int size) {
		return new ArrayList<E>(Collections.nCopies(size, null));
	}

}
