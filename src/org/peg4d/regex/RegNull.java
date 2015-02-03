package org.peg4d.regex;

public class RegNull extends RegexObject {

	public RegNull() {
		super(null);
	}

	@Override
	public String getLetter() {
		return "";
	}

	@Override
	public String toString() {
		return "";
	}
}
