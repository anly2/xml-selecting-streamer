package aanchev.utils;

import static java.util.Arrays.asList;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternUtils {

    /* Named Groups Helpers */

    public static boolean hasNamedGroup(Pattern pattern, String groupName) {
        return (getNamedGroup(pattern, groupName) != null);
    }

    public static Pattern getNamedGroup(Pattern pattern, String groupName) {
        String p = pattern.pattern();

        Matcher m = Pattern.compile("(?<" + groupName + ">", Pattern.LITERAL)
                .matcher(p);

        while (m.find()) {
            int start = m.start();

            if (isEscaped(m.start(), p))
                continue;

            int end = findMatching(start, p).end();

            return Pattern.compile(p.substring(start, end), pattern.flags());
        }

        return null;
    }


    /* String Utils */

    /** Escaping **/

    public static boolean isEscaped(int index, String haystack) {
        int count = 0;

        for (int i = index - 1; i >= 0; i--) {
            if (haystack.charAt(i) != '\\')
                break;

            count++;
        }

        return (count % 2 != 0);
    }


    /* Find Matching spouse */

    /** Auto-Detect pair **/

    private static final List<Pair<Pattern, Pattern>> pairs = asList(
            _pp("(", ")"),
            _pp("{", "}"),
            _pp("[", "]"),

            _pp("\"", "\""),
            _pp("'", "'"),

            /* Tags
            // Not complete; Too hard to implement it in a generic way
            new Pair<Pattern, Pattern>(
                    Pattern.compile("<[a-zA-Z][a-zA-Z0-9_]*+(?:>|\\s)"),
                    Pattern.compile("</[a-zA-Z][a-zA-Z0-9_]*+>")),
            //*/
            _pp("<", ">")
            );

    private static Pair<Pattern, Pattern> _pp(CharSequence p1, CharSequence p2) {
        return new Pair<Pattern, Pattern>(
                Pattern.compile(p1.toString(), Pattern.LITERAL | Pattern.DOTALL),
                Pattern.compile(p2.toString(), Pattern.LITERAL | Pattern.DOTALL));
    }


    public static MatchResult findMatching(int index, String haystack) {
        for (Pair<Pattern, Pattern> pair : pairs) {
            if (pair.getLeft().matcher(haystack).region(index, haystack.length()).lookingAt())
                return findMatchingClosing(pair.getLeft(), pair.getRight(), haystack, index);

            if (pair.getRight().matcher(haystack).region(index, haystack.length()).lookingAt())
                return findMatchingOpening(pair.getLeft(), pair.getRight(), haystack, index);
        }

        return null;
    }

    public static MatchResult findPreviousMatching(int index, String haystack) {
        for (Pair<Pattern, Pattern> pair : pairs) {
            if (pair.getRight().matcher(haystack).region(index, haystack.length()).lookingAt())
                return findMatchingOpening(pair.getLeft(), pair.getRight(), haystack, index);
        }

        return null;
    }


    /** Overloads - Pair **/

    public static MatchResult findMatching(Pair<Pattern, Pattern> groupBoundaries, String haystack, int index) {
        return findMatching(groupBoundaries.getLeft(), groupBoundaries.getRight(), haystack, index);
    }

    public static MatchResult findMatchingClosing(Pair<Pattern, Pattern> groupBoundaries, String haystack, int index) {
        return findMatchingClosing(groupBoundaries.getLeft(), groupBoundaries.getRight(), haystack, index);
    }

    public static MatchResult findMatchingOpening(Pair<Pattern, Pattern> groupBoundaries, String haystack, int index) {
        return findMatchingOpening(groupBoundaries.getLeft(), groupBoundaries.getRight(), haystack, index);
    }


    /** Overloads - String **/

    public static MatchResult findMatching(String openingRegex, String closingRegex, String haystack, int index) {
        return findMatching(Pattern.compile(openingRegex), Pattern.compile(closingRegex), haystack, index);
    }

    public static MatchResult findMatchingClosing(String openingRegex, String closingRegex, String haystack, int index) {
        return findMatchingClosing(Pattern.compile(openingRegex), Pattern.compile(closingRegex), haystack, index);
    }

    public static MatchResult findMatchingOpening(String openingRegex, String closingRegex, String haystack, int index) {
        return findMatchingOpening(Pattern.compile(openingRegex), Pattern.compile(closingRegex), haystack, index);
    }


    /** Functionality **/

    public static MatchResult findMatching(Pattern opening, Pattern closing, String haystack, int index) {
        if (opening.matcher(haystack).region(index, haystack.length()).lookingAt())
            return findMatchingClosing(opening, closing, haystack, index);

        if (closing.matcher(haystack).region(index, haystack.length()).lookingAt())
            return findMatchingOpening(opening, closing, haystack, index);

        return null;
    }

    public static MatchResult findMatchingClosing(Pattern opening, Pattern closing, String haystack, int index) {
        Matcher mOpening = opening.matcher(haystack).region(index, haystack.length());
        Matcher mClosing = closing.matcher(haystack).region(index, haystack.length());

        if (!mOpening.lookingAt())
            return null;


        int open = 1;
        int i = mOpening.end();
        mClosing.region(i, haystack.length());

        do {
            if (!mClosing.find())
                throw new PatternSyntaxException("Unbalanced expression!", haystack, i);

            mOpening.region(i, mClosing.start());

            while (mOpening.find()) {
                i = mOpening.end();
                open++;
            }

            open--;
        } while (open > 0);

        return mClosing;
    }

    public static MatchResult findMatchingOpening(Pattern opening, Pattern closing, String haystack, int index) {
        Matcher mOpening = opening.matcher(haystack).region(index, haystack.length());
        Matcher mClosing = closing.matcher(haystack).region(index, haystack.length());

        if (!mClosing.lookingAt())
            return null;


        // Cache all the 'openings' because we can't do 'reverseFind'
        List<MatchResult> openings = new LinkedList<MatchResult>();

        mOpening.region(0, index);

        while (mOpening.find())
            openings.add(mOpening.toMatchResult());


        // Do the same thing as findMatching but in reverse
        int unopen = 1;
        int i = index;
        int k = openings.size();
        MatchResult token = null;

        do {
            if (k == 0)
                throw new PatternSyntaxException("Unbalanced expression!", haystack, i);

            token = openings.get(--k);
            mClosing.region(token.end(), i);

            while (mClosing.find())
                unopen++;

            i = token.start();
            unopen--;
        } while (unopen > 0);

        return token;
    }
}
