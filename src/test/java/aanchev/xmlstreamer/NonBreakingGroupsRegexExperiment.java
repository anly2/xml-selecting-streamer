package aanchev.xmlstreamer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NonBreakingGroupsRegexExperiment {
	public static void main(String[] args) {
//		String input = ".list:not(.list .list) li:not(li li)"; //9-21, 29-35
		String input = ".list:not(.list .list):not(a *) li:not(li li)"; //9-21, 26-30, 38-44
		
		Matcher mpairs = Pattern.compile("\\(.*?\\)").matcher(input);
		while (mpairs.find())
			System.out.println("pair: "+mpairs.start()+" - "+(mpairs.end()-1));
		
		
//		String regex = "(?<!^.{9,21})(.*?)(?<!^.{9,21})(?<!^.{29,35}) (.*)";
		String regex = "(?<!^.{9,21})(.*?)(?<!^.{9,21})(?<!^.{26,30}) (.*)";
		
		Matcher m = Pattern.compile(regex).matcher(input);
		if (m.find()) {
			System.out.println("<<< "+m.group());
			for (int g=0; g<m.groupCount(); g++)
				System.out.println(m.group(g+1));
		}
	}
}
