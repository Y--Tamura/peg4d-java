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

		if(this.size() < 1) return "";

		for(RegexObject e: list) {
			String str = e.getLetter();
			if(str != null) sb.append(e.getLetter());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(look){
			if(not) sb.append("!(");
			else sb.append("&(");
		}
		if(quantifier != null) {
			sb.append("( ");
		}
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		if(quantifier != null) {
			sb.append(")");
			sb.append(quantifier.toString());
		}
		if(look){
			sb.append(")");
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()) return this.quantifier.repeatRule(sb.toString());
		else return sb.toString();
	}
}
