package org.peg4d.regex;

import java.util.LinkedHashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	private Set<String> set;

	public RegCharSet(ParsingObject po) {
		this(po, null);
	}

	public RegCharSet(ParsingObject po, RegexObject parent) {
		super(po, parent);
		set = new LinkedHashSet<String>();
		setCharSet(po.get(1).getText());
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
		String[] arr = set.toArray(new String[set.size()]);
		if(arr.length == 1){
			sb.append("'");
			sb.append(arr[0]);
			sb.append("'");
		}else{
			sb.append("( ");
			sb.append("'");
			sb.append(arr[0]);
			sb.append("' ");
			for(int i = 1; i < arr.length; i++) {
				sb.append("/ '");
				sb.append(arr[i]);
				sb.append("' ");
			}
			sb.append(")");
		}
		if(this.quantifier != null) {
			sb.append(this.quantifier.toString());
		}
		return sb.toString();
	}

}
