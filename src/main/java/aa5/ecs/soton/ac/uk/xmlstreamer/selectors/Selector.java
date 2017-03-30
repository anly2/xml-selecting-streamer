package aa5.ecs.soton.ac.uk.xmlstreamer.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import aa5.ecs.soton.ac.uk.xmlstreamer.AsyncXMLStreamer;
import aa5.ecs.soton.ac.uk.xmlstreamer.Element;
import aa5.ecs.soton.ac.uk.xmlstreamer.parser.SimpleParser;
import aa5.ecs.soton.ac.uk.xmlstreamer.parser.SimpleParser.AST;

public interface Selector {
	public void attach();
	public void detach();
	public String getSelector();
	public Consumer<Element> trigger(Consumer<Element> action);

	public static void main(String[] args) {
		String input = "#main .title ~ ul > li:not(a) + [href] ~ a#target.link[href^=\"https\"]";
		Selector root = new Selector.Compiler(null).compile(input);
		System.out.println(root);
	}

	
	/* Bound Compiler */
	
	public static class Compiler {
		private SimpleParser parser;
		
		public Compiler(AsyncXMLStreamer streamer) {
			final Selector.Factory s = new Selector.Factory(streamer);
			
			parser = new SimpleParser.Builder()
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+   "~"    +"\\s*+" +"(.*+)", m -> s.selSibling())
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+  "\\+"   +"\\s*+" +"(.*+)", m -> s.selImmediateSibling())
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+   ">"    +"\\s*+" +"(.*+)", m -> s.selImmediateDescendent())
				.rule("\\s*+"+ "(\\S++)"+         "\\s++"           +"(.++)", m -> s.selDescendent())
				.rule("\\s*+"+ "([^\\[\\]]++)?" + "\\[([^\\s\\]]++)\\]" +"\\s*+", m -> s.selAttribute(m.group(2)), 2)
				.rule("\\s*+"+ "([^\\s#]++)?"   + "\\#([\\w\\-]++)"     +"\\s*+", m -> s.selAttrId(m.group(2)), 2)
				.rule("\\s*+"+ "([^\\s\\.]++)?" + "\\.([\\w\\-]++)"     +"\\s*+", m -> s.selAttrClass(m.group(2)), 2)
				.rule("\\s*+"+                    "([a-zA-Z]\\w*+)"     +"\\s*+", m -> s.selTag(m.group()), 1)
				.rule("\\s*+"+ "([^\\s:]++)?"   + ":before" +"\\s*+", m -> s.selTraitBefore())
				.rule("\\s*+"+ "([^\\s:]++)?"   + ":after"  +"\\s*+", m -> s.selTraitAfter())
				.rule("\\s*+"+ "([^\\s:]++)?"   + ":not\\(([^\\)]++)\\)", m -> s.selTraitNot())
				.rule("\\*", m -> s.selAny())
				.build();
		}
		
		public Selector compile(CharSequence selector) {
			return parser.parse(selector).<Selector>cast();
		}
	}
	
	
	/* Bound Factory */
	
	public static class Factory {
		
		private AsyncXMLStreamer streamer;
		
		public Factory(AsyncXMLStreamer streamer) {
			this.streamer = streamer;
		}
		
		/* Selector types */
		
		private abstract class SelectorNode extends AST.Node implements Selector {
			public String raw;
			protected Consumer<Element> action;
			
			public SelectorNode(String raw, int childrenCount) {
				super(array(childrenCount));
				this.raw = raw;
			}

			public String getSelector() {
				return this.raw;
			}

			public String toString() {
				return getSelector();
			}

			
			public Consumer<Element> trigger(Consumer<Element> action) {
				this.action = action;
				return action;
			}
		}

		private abstract class SelectorLeaf extends AST.Leaf implements Selector, Consumer<Element> {
			public String raw;
			protected Consumer<Element> action;
			
			public SelectorLeaf(String raw) {
				this.raw = raw;
			}

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
			
			
			public void attach() {
				streamer.onTagStart(this);
			}

			public void detach() {
				streamer.onTagEnd(this);
			}
		}

		private abstract class SimpleSelectorNode extends SelectorNode {
			public SimpleSelectorNode(String raw) {
				super(raw, 1);
			}

			public void attach() {
				Selector inner = getChild(0).cast();
				
				if (getChild(0) == null) {
					AST e = selAny();
					setChild(0, e);
					inner = e.cast();
				}
				
				inner.trigger(element -> {
					if (matches(element))
						action.accept(element);
				});
				inner.attach();
			}

			public void detach() {
				getChild(0).<Selector>cast().detach();
			}

			
			protected abstract boolean matches(Element element);
			
			
			@Override
			public String getSelector() {
				return any(getChild(0)) + super.getSelector();
			}
		}
		
		private abstract class BinarySelectorNode extends SelectorNode {
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
		
		private static String any(Object o) {
			return o == null? "*" : o.toString();
		}
		
		
		/* Selector instance creators */
		
		public AST selAny() {
			return new SelectorLeaf("*") {
				@Override
				public void accept(Element element) {
					action.accept(element);
				}
			};
		}
		
		public AST selTag(String tag) {
			return new SelectorLeaf(tag) {
				@Override
				public void accept(Element element) {
					if (element.getTag().equalsIgnoreCase(tag))
						action.accept(element);
				}
			};
		}

		
		public AST selAttribute(String attr) {
			return new SimpleSelectorNode("["+attr+"]") {
				protected boolean matches(Element element) {
					return (element.getAttribute(attr) != null);
				}
			};
		}

		public AST selAttrId(String id) {
			return new SimpleSelectorNode("#"+id) {
				protected boolean matches(Element element) {
					final Object v = element.getAttribute("id");
					return (v != null && id.equals(v));
				}
			};
		}
		
		public AST selAttrClass(String cls) {
			return new SimpleSelectorNode("."+cls) {
				private Predicate<String> pred = Pattern.compile("(?:^|\\s)\\Q"+cls+"\\E(?:$|\\S)").asPredicate();
				protected boolean matches(Element element) {
					final Object v = element.getAttribute("class");
					return (v != null && pred.test(v.toString()));
				}
			};
		}

		
		public AST selDescendent() {

			//TODO
			return new BinarySelectorNode("	") {
				private Selector startTracker = selTraitBefore().cast();
				private Selector endTracker = selTraitAfter().cast();
				private int acceptableParents = 0;
				
				public void attach() {
					getChild(0).<Selector>cast().trigger(e -> {
						acceptableParents++;
						onClose(e, e_ -> acceptableParents--);
					});
					
					startTracker.attach();
					endTracker.attach();
				}

				public void detach() {
					startTracker.detach();
					endTracker.detach();
				}
				
				private void onClose(Element e, Consumer<Element> a) {
				}
			};
		}

		public AST selImmediateDescendent() {
			return new SelectorNode(">", 2) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}
		
		
		public AST selSibling() {
			return new SelectorNode("~", 2) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}

		public AST selImmediateSibling() {
			return new SelectorNode("+", 2) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}

		
		public AST selTraitBefore() {
			return new SelectorNode(":before", 1) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}

		public AST selTraitAfter() {
			return new SelectorNode(":after", 1) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}
		
		
		public AST selTraitNot() {
			return new SelectorNode(":not()", 2) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String getSelector() {
					return getChild(0) + ":not(" + getChild(1) + ")";
				}
			};
		}
	}
}