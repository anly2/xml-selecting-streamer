package aanchev.xmlstreamer.selectors;

import java.util.function.Consumer;

import aanchev.parser.SimpleParser.AST;
import aanchev.parser.SimpleParser.Builder;
import aanchev.xmlstreamer.AsyncXMLStreamer;
import aanchev.xmlstreamer.Element;

public class TargetingBasicSelectorParser extends BasicSelectorParser {

	public TargetingBasicSelectorParser(AsyncXMLStreamer streamer) {
		super(streamer);
	}
	
	
	private Reference<Element> target = new Reference<>(null);
	
	private static class Reference<E> {
		public E value;
		
		public Reference(E value) {
			this.value = value;
		}
	}
	
	private static class SelectorProxy implements Selector {
		private Selector base;
		
		public SelectorProxy(Selector base) {
			this.base = base;
		}
		
		
		public void attach() {
			base.attach();
		}

		@Override
		public void detach() {
			base.detach();
		}

		@Override
		public String getSelector() {
			return base.getSelector();
		}

		@Override
		public Consumer<Element> trigger(Consumer<Element> action) {
			return base.trigger(action);
		}
	
		@Override
		public String toString() {
			return base.toString();
		}
	}
	
	
	@Override
	public Selector compile(CharSequence selector) {
		target.value = null;
		return new SelectorProxy(super.compile(selector)) {
			@Override
			public Consumer<Element> trigger(Consumer<Element> action) {
				return super.trigger(element -> {
					Element e = element;
					
					if (target.value != null) //#? is it possible to be null?
						e = target.value;
					
					action.accept(e);					
				});
			}
		};
	}
	
	@Override
	protected void initParser(Builder builder) {
		builder
			.rule("\\s*+" + "\\$([^\\$\\s>~\\+]++)" + "\\s*+", m -> selTargeted(target))
			.rule("\\s*+" + "([^\\$\\s>~\\+]++)\\$" + "\\s*+", m -> selTargeted(target));
		
		super.initParser(builder);
	}
	
	
	public AST selTargeted(final Reference<Element> refTarget) {
		return new SelectorNode("$", 1) {
			@Override
			public void attach() {
				Selector inner = getChild(0).cast(); //potential NullPointerException
				
				inner.trigger(element -> {
					refTarget.value = element;
					action.accept(element);
				});
				inner.attach();
			}

			@Override
			public void detach() {
				Selector inner = getChild(0).cast(); //potential NullPointerException
				inner.detach();
				refTarget.value = null;
			}
			
			@Override
			public String getSelector() {
				return "$" + getChild(0);
			}
		};
	}
}
