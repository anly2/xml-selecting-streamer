package aanchev.xmlstreamer.selectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import aanchev.parser.SimpleParser.AST;
import aanchev.parser.SimpleParser.Builder;
import aanchev.xmlstreamer.AsyncXMLStreamer;
import aanchev.xmlstreamer.Element;

public class TargetingBasicSelectorParser extends BasicSelectorParser {

	/* Construction */

	public TargetingBasicSelectorParser(AsyncXMLStreamer streamer) {
		super(streamer);
	}
	
	
	/* Extend and Hook into the Parser */
	
	@Override
	public Selector compile(CharSequence selector) {
		final Reference<Element> target = new Reference<>(null);
		
		return new SelectorProxy(this.compileWith(selector, target)) {
			@Override
			public Consumer<Element> trigger(Consumer<Element> action) {
				return super.trigger(element -> {
					Element e = target.value; //#? can this be null?
					target.value = null;
					
					action.accept(e);					
				});
			}
		};
	}

	@Override
	protected void initParser(Builder builder) {
		builder
			.rule("\\s*+" + "\\$([^\\$\\s>~\\+]++)" + "\\s*+", m -> selTargetedFirst())
			.rule("\\s*+" + "([^\\$\\s>~\\+]++)\\$" + "\\s*+", m -> selTargetedLast());
		
		super.initParser(builder);
	}

	
	/* Maintain Targets as Context/State */
	
	private Map<Thread, Reference<Element>> targets = Collections.synchronizedMap(new HashMap<>());
	
	protected synchronized Selector compileWith(CharSequence selector, Reference<Element> target) {
		targets.put(Thread.currentThread(), target);
		Selector sel = super.compile(selector);
		targets.remove(Thread.currentThread());
		return sel;
	}

	
	/* Inner Classes */

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
	
	
	/* AST Selector Nodes Creation */
	
	public AST selTargetedLast() {
		final Reference<Element> refTarget = targets.get(Thread.currentThread());
		
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
				return getChild(0) + "$";
			}
		};
	}
	
	public AST selTargetedFirst() {
		final Reference<Element> refTarget = targets.get(Thread.currentThread());
		
		return new SelectorNode("$", 1) {
			@Override
			public void attach() {
				Selector inner = getChild(0).cast(); //potential NullPointerException
				
				inner.trigger(element -> {
					if(refTarget.value == null)
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
