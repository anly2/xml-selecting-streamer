package aa5.ecs.soton.ac.uk.xmlstreamer.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.w3c.dom.Element;

import aa5.ecs.soton.ac.uk.xmlstreamer.AsyncXMLStreamer;
import aa5.ecs.soton.ac.uk.xmlstreamer.selectors.SimpleParser.AST;
import aa5.ecs.soton.ac.uk.xmlstreamer.selectors.SimpleParser.Builder;

public interface Selector {
	public void attach();
	public void detach();
	public Selector on(Consumer<Element> action);
	public String getSelector();

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
			parser = new Builder()
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+   "~"    +"\\s*+" +"(.*+)", m -> s.selSibling())
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+  "\\+"   +"\\s*+" +"(.*+)", m -> s.selImmediateSibling())
				.rule("\\s*+"+ "(\\S++)"+"\\s*+"+   ">"    +"\\s*+" +"(.*+)", m -> s.selImmediateDescendent())
				.rule("\\s*+"+ "(\\S++)"+         "\\s++"           +"(.++)", m -> s.selDescendent())
				.rule("\\s*+"+ "([^\\[\\]]++)?" + "\\[([^\\s\\]]++)\\]" +"\\s*+", m -> s.selAttribute(m.group(2)), 2)
				.rule("\\s*+"+ "([^\\s#]++)?"   + "\\#([\\w\\-]++)"     +"\\s*+", m -> s.selAttrId(m.group(2)), 2)
				.rule("\\s*+"+ "([^\\s\\.]++)?" + "\\.([\\w\\-]++)"     +"\\s*+", m -> s.selAttrClass(m.group(2)), 2)
				.rule("\\s*+"+                    "([a-zA-Z]\\w*+)"     +"\\s*+", m -> s.selTag(m.group()), 1)
				.rule("\\s*+"+ "([^\\s:]++)?"   + ":not\\(([^\\)]++)\\)", m -> s.selTraitNot())
				.rule("\\*", m -> s.selAny())
				.build();
		}
		
		public Selector compile(CharSequence selector) {
			return parser.parse(selector).<Selector>to();
		}
	}
	
	
	/* Bound Factory */
	
	public static class Factory {
		private AsyncXMLStreamer streamer;

		public Factory(AsyncXMLStreamer xmlStreamer) {
			this.streamer = xmlStreamer;
		}
		
		
		/* Selector types */
		
		private abstract class SelectorNode extends AST.Node implements Selector {
			public String raw;
			
			public SelectorNode(String raw, int childrenCount) {
				super(array(childrenCount));
				this.raw = raw;
			}

			public Selector on(Consumer<Element> action) {
				streamer.on(getSelector(), action);
				return this;
			}

			public String getSelector() {
				return this.raw;
			}

			public String toString() {
				return getSelector();
			}
		}

		private abstract class SelectorLeaf extends AST.Leaf implements Selector {
			public String raw;
			
			public SelectorLeaf(String raw) {
				this.raw = raw;
			}

			public Selector on(Consumer<Element> action) {
				streamer.on(getSelector(), action);
				return this;
			}

			public String getSelector() {
				return this.raw;
			}
			
			@Override
			public String toString() {
				return getSelector();
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
				public void attach() {
//					streamer.
				}

				public void detach() {
				}
			};
		}
		
		public AST selTag(String tag) {
			return new SelectorLeaf(tag) {
				public void attach() {
				}

				public void detach() {
				}
			};
		}

		
		public AST selAttribute(String attr) {
			return new SelectorNode("["+attr+"]", 1) {
				public void attach() {
				}

				public void detach() {
				}
			
				@Override
				public String toString() {
					return any(getChild(0)) + getSelector();
				}
			};
		}

		public AST selAttrId(String id) {
			return new SelectorNode("#"+id, 1) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String toString() {
					return any(getChild(0)) + getSelector();
				}
			};
		}
		
		public AST selAttrClass(String cls) {
			return new SelectorNode("."+cls, 1) {
				public void attach() {
				}

				public void detach() {
				}

				@Override
				public String toString() {
					return any(getChild(0)) + getSelector();
				}
			};
		}

		
		public AST selDescendent() {
			return new SelectorNode("	", 2) {
				public void attach() {
				}
				
				public void detach() {
				}

				@Override
				public String toString() {
					return getChild(0) + getSelector() + getChild(1);
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
				public String toString() {
					return getChild(0) + getSelector() + getChild(1);
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
				public String toString() {
					return getChild(0) + getSelector() + getChild(1);
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
				public String toString() {
					return getChild(0) + getSelector() + getChild(1);
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
				public String toString() {
					return getChild(0) + ":not(" + getChild(1) + ")";
				}
			};
		}
	}
}