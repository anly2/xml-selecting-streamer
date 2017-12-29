package aanchev.xmlstreamer.selectors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

import aanchev.parser.SimpleParser;
import aanchev.xmlstreamer.AsyncXMLStreamer;
import aanchev.xmlstreamer.Element;

public interface Selector {
	public void attach();
	public void detach();
	public String getSelector();
	public Consumer<Element> trigger(Consumer<Element> action);

	public interface Compiler {
		public Selector compile(CharSequence selector);


		public interface BuilderDecorator {
			public void decorate(SimpleParser.Builder builder);
			public Selector.Compiler intercept(Selector.Compiler result);
		}


		public static class Builder {
			private LinkedList<BuilderDecorator> decorators = new LinkedList<>();

			public Builder chain(BuilderDecorator decorator) {
				decorators.add(decorator);
				return this;
			}

			public Selector.Compiler build() {
				SimpleParser.Builder builder = new SimpleParser.Builder();

				for (BuilderDecorator decorator : decorators) {
					decorator.decorate(builder);
					builder.lastly(); //"reset" the insertion index, just in case
				}

				Selector.Compiler result = new ParserBridge(builder.build());

				for (Iterator<BuilderDecorator> it=decorators.descendingIterator(); it.hasNext();)
					result = it.next().intercept(result);

				return result;
			}
		}

		public static class ParserBridge implements Selector.Compiler {
			private SimpleParser parser;

			public ParserBridge(SimpleParser parser) {
				this.parser = parser;
			}

			@Override
			public Selector compile(CharSequence selector) {
				return parser.parse(selector).cast();
			}
		}

	}

	public static Compiler compilerFor(AsyncXMLStreamer streamer) {
		return new Compiler.Builder()
					.chain(new BasicSelectorProvider(streamer))
					.chain(new ContentMatchingSelectorProvider(streamer))
					.chain(new TargetingSelectorProvider(streamer))
					.build();
	}
}