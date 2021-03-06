package aanchev.xmlstreamer.selectors;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import aanchev.parser.SimpleParser.AST;
import aanchev.parser.SimpleParser.Builder;
import aanchev.xmlstreamer.Element;
import aanchev.xmlstreamer.ReactiveXMLStreamer;

public class ContentMatchingSelectorProvider extends BasicSelectorProvider {

	/* Construction */

	public ContentMatchingSelectorProvider(ReactiveXMLStreamer notifier) {
		super(notifier);
	}


	/* Compiler.Decorator Contract */

	@Override
	public void decorate(Builder builder) {
		builder.firstly()
			.rule("\\s*+"+ "([^\\s\\{]++)?" + "\\{([^\\}]*+)\\}" +"\\s*+", m -> selTrimMatchingContent(m.group(2)), 2)
			.rule("\\s*+"+ "([^\\s\\|]++)?" + "\\|([^\\|]*+)\\|" +"\\s*+", m -> selExactMatchingContent(m.group(2)), 2)
			.rule("\\s*+"+ "([^\\s\\/]++)?" + "\\/([^\\/]*+)\\/" +"\\s*+", m -> selRegexMatchingContent(m.group(2)), 2)
			.lastly();
	}


	/* Selector instance creators */

	private AST selTrimMatchingContent(String content) {
		final String trimmedContent = content.trim();

		return new SimpleSelectorNode("{"+content+"}") {
			@Override
			protected boolean matches(Element element) {
				return (element.getText().trim().equals(trimmedContent)); //#!!! careful for infinite recursion with .getText() before complete match
			}
		};
	}

	private AST selExactMatchingContent(String content) {
		return new SimpleSelectorNode("|"+content+"|") {
			@Override
			protected boolean matches(Element element) {
				return (element.getText().equals(content)); //#!!! careful for infinite recursion with .getText() before complete match
			}
		};
	}

	private AST selRegexMatchingContent(String contentRegex) {
		final Predicate<String> contentPredicate = Pattern.compile(contentRegex).asPredicate();

		return new SimpleSelectorNode("/"+contentRegex+"/") {
			@Override
			protected boolean matches(Element element) {
				return (contentPredicate.test(element.getText())); //#!!! careful for infinite recursion with .getText() before complete match
			}
		};
	}
}
