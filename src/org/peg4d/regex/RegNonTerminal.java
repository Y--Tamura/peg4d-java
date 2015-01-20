package org.peg4d.regex;


public class RegNonTerminal extends RegexObject {

	private String label;
	public RegNonTerminal(String label) {
		super(null); //FIXME
		this.label = label;
	}

	@Override
	public String getLetter() {
		return "";
	}

	@Override
	public String toString() {
		if(this.quantifier == null) return label;
		else return label + this.quantifier.toString();
	}

}
