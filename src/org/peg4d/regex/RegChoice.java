package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class RegChoice extends RegexObject {

	public RegChoice() {
		this(null);
	}

	public RegChoice(ParsingObject po) {
		this(po, null);
	}

	public RegChoice(ParsingObject po, RegexObject parent) {
		super(po, parent);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(quantifier != null) {
			sb.append("(");
		}
		if(list.size() > 0) {
			sb.append(list.get(0).toString());
		}
		for(int i = 1; i < list.size(); i++) {
			sb.append(" / ");
			sb.append(list.get(i).toString());
		}
		if(quantifier != null) {
			sb.append(")");
			sb.append(quantifier.toString());
		}
		return sb.toString();
	}
}
