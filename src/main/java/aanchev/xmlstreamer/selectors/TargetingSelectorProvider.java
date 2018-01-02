package aanchev.xmlstreamer.selectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import aanchev.parser.SimpleParser;
import aanchev.parser.SimpleParser.AST;
import aanchev.xmlstreamer.ChildCulling;
import aanchev.xmlstreamer.Element;
import aanchev.xmlstreamer.ReactiveXMLStreamer;
import aanchev.xmlstreamer.selectors.Selector.Compiler;

public class TargetingSelectorProvider extends AbstractSelectorProvider implements Selector.Compiler {

	/* Inner State */

	private Selector.Compiler kernelParser;
	private Map<Thread, Reference<Element>> targets = Collections.synchronizedMap(new HashMap<>()); // maintain Targets as Context/State

	/* Construction */

	public TargetingSelectorProvider() {
		this(null);
	}

	public TargetingSelectorProvider(ReactiveXMLStreamer notifier) {
		super(notifier);
	}


	/* Extend and Hook into the Parser */

	@Override
	public void decorate(SimpleParser.Builder builder) {
		builder.firstly() //add rules to the "top" (tried first)
			.rule("\\s*+" + "\\$([^\\$\\s>~\\+]++)" + "\\s*+", m -> selTargetedFirst())
			.rule("\\s*+" + "([^\\$\\s>~\\+]++)\\$" + "\\s*+", m -> selTargetedLast())
			.lastly(); //"reset" the insertion index just in case
	}

	@Override
	public Compiler intercept(Compiler result) {
		this.kernelParser = result;
		return this;
	}


	/* Selector.Compiler Contract */

	@Override
	public Selector compile(CharSequence selector) {
		final Reference<Element> target = new Reference<>(null);

		return new SelectorProxy(this.compileWith(selector, target)) {
			@Override
			public Consumer<Element> triggers(Consumer<Element> action) {
				return super.triggers(element -> {
					Element e;

					if (target.value != null) {
						 e = target.value;
						 target.value = null;
					}
					else
						e = element;

					action.accept(e);
				});
			}
		};
	}

	protected synchronized Selector compileWith(CharSequence selector, Reference<Element> target) {
		targets.put(Thread.currentThread(), target);
		Selector sel = kernelParser.compile(selector);
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


		@Override
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
		public Consumer<Element> triggers(Consumer<Element> action) {
			return base.triggers(action);
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

				inner.triggers(element -> {
					refTarget.value = element;

					if (notifier instanceof ChildCulling)
						((ChildCulling) notifier).keepChildren(true);

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

				inner.triggers(element -> {
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
