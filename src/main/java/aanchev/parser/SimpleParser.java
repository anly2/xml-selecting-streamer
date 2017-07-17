package aanchev.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleParser {
	private final List<Rule> rules;


	public SimpleParser(Rule... rules) {
		this(Arrays.asList(rules));
	}

	public SimpleParser(List<Rule> rules) {
		this.rules = rules;
	}


	public static class Builder {
		private List<Rule> rules = new LinkedList<>();
		private int i = 0; //index to insert next at


		public Builder firstly() {
			this.i = 0;
			return this;
		}

		public Builder lastly() {
			this.i = rules.size();
			return this;
		}


		public Builder rule(Rule rule) {
			rules.add(i++, rule);
			return this;
		}

		public Builder rule(String pattern, Function<MatchResult, AST> applier) {
			return rule(new Rule(pattern, applier));
		}

		public Builder rule(String pattern, Function<MatchResult, AST> applier, Integer... consumedGroups) {
			return rule(new Rule.Consuming(pattern, applier, consumedGroups));
		}


		public SimpleParser build() {
			return new SimpleParser(rules);
		}
	}


	public static class Rule implements Iterable<Integer> {
		public final Pattern pattern;
		public final Function<MatchResult, AST> applier;

		public Rule(Pattern pattern, Function<MatchResult, AST> applier) {
			this.pattern = pattern;
			this.applier = applier;
		}

		public Rule(String pattern, Function<MatchResult, AST> applier) {
			this(Pattern.compile(pattern), applier);
		}


		@Override
		public Iterator<Integer> iterator() {
			final int groups = pattern.matcher("").groupCount();

			return new Iterator<Integer>() {
				private int i = 1;

				@Override
				public boolean hasNext() {
					return i <= groups;
				}

				@Override
				public Integer next() {
					return i++;
				}
			};
		}


		public static class Consuming extends Rule {
			private Set<Integer> consumedGroups;

			public Consuming(String pattern, Function<MatchResult, AST> applier, Integer... consumedGroups) {
				this(pattern, new HashSet<Integer>(Arrays.asList(consumedGroups)), applier);
			}
			public Consuming(Pattern pattern, Function<MatchResult, AST> applier, Integer... consumedGroups) {
				this(pattern, new HashSet<Integer>(Arrays.asList(consumedGroups)), applier);
			}

			public Consuming(String pattern, Set<Integer> consumedGroups, Function<MatchResult, AST> applier) {
				super(Pattern.compile(pattern), applier);
				this.consumedGroups = consumedGroups;
			}
			public Consuming(Pattern pattern, Set<Integer> consumedGroups, Function<MatchResult, AST> applier) {
				super(pattern, applier);
				this.consumedGroups = consumedGroups;
			}

			@Override
			public Iterator<Integer> iterator() {
				final int groups = pattern.matcher("").groupCount();

				Iterator<Integer> it = new Iterator<Integer>() {
					private int i = 0;

					@Override
					public boolean hasNext() {
						return i < groups;
					}

					@Override
					public Integer next() {
						int r = i;

						do i++;
						while (consumedGroups.contains(i));

						return r;
					}
				};
				it.next();

				return it;
			}

		}

	}


	public static interface Castable {
		@SuppressWarnings("unchecked")
		public default <E> E cast() {
			try {
				return (E) this;
			}
			catch (ClassCastException e) {
				return null;
			}
		}

		public default <E> Optional<E> maybe() {
			return Optional.ofNullable(this.cast());
		}
	}

	public static interface AST extends Castable {

		public AST getChild(int i);
		public void setChild(int i, AST child);
		public int getChildCount();


		public static class Node implements AST {
			protected List<AST> children;

			protected Node(AST... children) {
				this(Arrays.asList(children));
			}

			protected Node(List<AST> children) {
				this.children = children;
			}


			@Override
			public AST getChild(int i) {
				return this.children.get(i);
			}

			@Override
			public void setChild(int i, AST child) {
				this.children.set(i, child);
			}

			@Override
			public int getChildCount() {
				return this.children.size();
			}
		}

		public static class Leaf implements AST {

			@Override
			public AST getChild(int i) {
				return null;
			}

			@Override
			public void setChild(int i, AST child) {}

			@Override
			public int getChildCount() {
				return 0;
			}
		}
	}


	public AST parse(CharSequence input) {
		return parse(input, 0, input.length());
	}

	protected AST parse(CharSequence input, int start, int end) {
		for (Rule rule : rules) {
			Matcher m = rule.pattern.matcher(input).region(start, end);

			if (!m.matches())
				continue;

//			System.out.println("M: |"+input.subSequence(start, end)+"|  ->  "+rule.pattern.pattern());

			try {
				AST node = rule.applier.apply(m.toMatchResult());

				int i = 0;
				for (Integer group : rule) {
					int s = m.start(group);
					int e = m.end(group);

					node.setChild(i++, (s==-1 || e==-1)? null : parse(input, s, e));
				}

				return node;
			}
			catch (UnexpectedChildException e) {
				continue;
			}
		}

		return null;
	}


	public static class UnexpectedChildException extends RuntimeException {
	}
}
