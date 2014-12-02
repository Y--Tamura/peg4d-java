package org.peg4d.regex;

import org.peg4d.ParsingObject;


public class RegSeq extends RegexObject {

	public RegSeq() {
		super(null);
	}

	public RegSeq(ParsingObject po) {
		super(po);
		this.addQuantifier(po);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		return sb.toString();
	}
}
