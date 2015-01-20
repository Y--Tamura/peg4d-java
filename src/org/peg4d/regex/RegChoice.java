package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class RegChoice extends RegexObject {

	public RegChoice() {
		super(null);
	}

	public RegChoice(ParsingObject po) {
		super(po);
	}

	@Override
	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.size() - 1; i++) {
			sb.append(this.get(i).getLetter());
			sb.append(" /");
		}
		sb.append(this.get(this.size() - 1).getLetter());
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if(list.size() > 0) {
			sb.append(list.get(0).toString());
		}
		for(int i = 1; i < list.size(); i++) {
			sb.append(" / ");
			sb.append(list.get(i).toString());
		}
		sb.append(")");
		if(quantifier != null) {
			sb.append(quantifier.toString());
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()) return this.quantifier.repeatRule(sb.toString());
		else return sb.toString();
	}
}
