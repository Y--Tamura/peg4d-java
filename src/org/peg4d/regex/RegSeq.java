package org.peg4d.regex;

import org.peg4d.ParsingObject;


public class RegSeq extends RegexObject {

	public RegSeq() {
		super(null);
	}

	public RegSeq(ParsingObject po) {
		super(po);
	}

	@Override
	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
			sb.append(e.getLetter());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(look){
			if(not) sb.append("!");
			else sb.append("&");
		}
		if(quantifier != null || look) {
			sb.append("( ");
		}
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		if(quantifier != null || look) {
			sb.append(")");
		}
		if(quantifier != null) {
			sb.append(quantifier.toString());
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()) return this.quantifier.repeatRule(sb.toString());
		else return sb.toString();
	}
}
