package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class RegChoice extends RegexObject {

	public RegChoice(ParsingObject po) {
		super(po);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(l.size() > 0) {
			sb.append(l.get(0).toString());
		}
		for(int i = 1; i < l.size(); i++) {
			sb.append(" / ");
			sb.append(l.get(i).toString());
		}
		return sb.toString();
	}
}
