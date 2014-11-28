package org.peg4d.regex;


public class RegSeq extends RegList {

	public RegSeq() {
		super(null); //FIXME
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: l) {
			sb.append(e.toString());
		}
		return sb.toString();
	}
}
