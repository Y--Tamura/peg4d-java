package org.peg4d.regex;

import java.util.LinkedHashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	private Set<String> set;
	public RegCharSet(ParsingObject po) {
		super(po);
		set = new LinkedHashSet<String>();
		System.out.println(po.getText());
		setCharSet(po.getText());
	}

	private void setCharSet(String s) {
		if(s.length() == 1) {
			set.add(s);
			return;
		}
		int i = 1;
		int max = s.length() - 1;
		do {
			int next = i + 1;
			int next2 = i + 2;
			if(next < s.length() && s.charAt(next)=='-' && next2 < s.length()) {
				for(char j = s.charAt(i); j <= s.charAt(next2); j++) {
					set.add(String.valueOf(j));
				}
				i += 3;
			} else {
				set.add(s.substring(i, next));
				++i;
			}
		} while(i < max);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(String s: set.toArray(new String[set.size()])) {
			sb.append("'");
			sb.append(s);
			sb.append("' ");
		}
		return sb.toString();
	}

}
