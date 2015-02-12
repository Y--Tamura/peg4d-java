package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class RegCharAny extends RegCharSet {

	public RegCharAny(ParsingObject e) {
		super(e);
	}

	public RegCharAny(){
		super(".");
	}

	@Override
	public String getLetter() {
		return ".";
	}

	@Override
	public String toString() {
		if(not) return "!.";
		else return ".";
	}
}
