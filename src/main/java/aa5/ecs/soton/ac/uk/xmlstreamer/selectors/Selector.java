package aa5.ecs.soton.ac.uk.xmlstreamer.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import aa5.ecs.soton.ac.uk.xmlstreamer.AsyncXMLStreamer;
import aa5.ecs.soton.ac.uk.xmlstreamer.Element;
import aa5.ecs.soton.ac.uk.xmlstreamer.parser.SimpleParser;
import aa5.ecs.soton.ac.uk.xmlstreamer.parser.SimpleParser.AST;

public interface Selector {
	public void attach(AsyncXMLStreamer streamer);
	public void detach(AsyncXMLStreamer streamer);
	public String getSelector();

	public static void main(String[] args) {
		String input = "#main .title ~ ul > li:not(a) + [href] ~ a#target.link[href^=\"https\"]";
		Selector root = new Selector.Compiler().compile(input);
		System.out.println(root);
	}

	
	/* Bound Compiler */
	
	public static class Compiler {
		private SimpleParser parser;
		
		public Compiler() {
			
			final Selector.Factory s = new Selector.Factory();
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
		
		/* Selector types */
		
		private abstract class SelectorNode extends AST.Node implements Selector, Consumer<Element> {
			public String raw;
			
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

			
			@Override
			public void attach(AsyncXMLStreamer streamer) {
				streamer.on(getSelector(), this);

				for (int i=0; i<getChildCount(); i++)
					getChild(i).<Selector>maybe().ifPresent(s -> s.attach(streamer));
			}

			@Override
			public void detach(AsyncXMLStreamer streamer) {
				streamer.off(getSelector(), this);

				for (int i=0; i<getChildCount(); i++)
					getChild(i).<Selector>maybe().ifPresent(s -> s.detach(streamer));
			}


			@Override
			public void accept(Element element) {
//				if (allChildrenSatisfied() && matches(element))
//					fire.run();
			}
		}

		private abstract class SelectorLeaf extends AST.Leaf implements Selector, Consumer<Element> {
			public String raw;
			
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
			public void attach(AsyncXMLStreamer streamer) {
				streamer.on(getSelector(), this);
			}

			@Override
			public void detach(AsyncXMLStreamer streamer) {
				streamer.off(getSelector(), this);
			}

			@Override
			public void accept(Element t) {
				//TODO: REMOVE AND DO
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
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
					streamer.off(getSelector(), this);
				}
			
				public void accept(Element e) {
					System.out.println(e.getTag());
				}
			};
		}
		
		public AST selTag(String tag) {
			return new SelectorLeaf(tag) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}
			};
		}

		
		public AST selAttribute(String attr) {
			return new SelectorNode("["+attr+"]", 1) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}
			
				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}

		public AST selAttrId(String id) {
			return new SelectorNode("#"+id, 1) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}
		
		public AST selAttrClass(String cls) {
			return new SelectorNode("."+cls, 1) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}

		
		public AST selDescendent() {
			return new SelectorNode("	", 2) {
				public void attach(AsyncXMLStreamer streamer) {
				}
				
				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}

		public AST selImmediateDescendent() {
			return new SelectorNode(">", 2) {
				public void attach(AsyncXMLStreamer streamer) {
				}
				
				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}
		
		
		public AST selSibling() {
			return new SelectorNode("~", 2) {
				public void attach(AsyncXMLStreamer streamer) {
				}
				
				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}

		public AST selImmediateSibling() {
			return new SelectorNode("+", 2) {
				public void attach(AsyncXMLStreamer streamer) {
				}
				
				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return getChild(0) + super.getSelector() + getChild(1);
				}
			};
		}

		
		public AST selTraitBefore() {
			return new SelectorNode(":before", 1) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}

		public AST selTraitAfter() {
			return new SelectorNode(":after", 1) {
				public void attach(AsyncXMLStreamer streamer) {
				}

				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return any(getChild(0)) + super.getSelector();
				}
			};
		}
		
		
		public AST selTraitNot() {
			return new SelectorNode(":not()", 2) {
				public void attach(AsyncXMLStreamer streamer) {
				}
				
				public void detach(AsyncXMLStreamer streamer) {
				}

				@Override
				public String getSelector() {
					return getChild(0) + ":not(" + getChild(1) + ")";
				}
			};
		}
	}
}