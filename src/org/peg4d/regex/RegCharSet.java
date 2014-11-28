package org.peg4d.regex;

import java.util.HashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	private Set<String> set;
	public RegCharSet(ParsingObject po) {
		super(po);
		set = new HashSet<String>();
		setCharSet(po.getText());
	}

	private void setCharSet(String s) {
		if(s.length() == 1) {
			set.add(s);
			return;
		}
		int i = 0;
		do {
			switch(s.charAt(i)) {
			case '-':
			default: {
				int n = i + 1;
				if(n < s.length()) {
					
				}
			}
			}
		} while(i < s.length());
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
