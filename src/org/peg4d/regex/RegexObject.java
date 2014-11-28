package org.peg4d.regex;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected ParsingObject ref;
	protected Quantifier quantifier;
	public boolean beginWith;
	public boolean endWith;

	public RegexObject(ParsingObject po) {
		beginWith = false;
		endWith = false;
		ref = po;
	}

	public void addQuantifier(ParsingObject po) {
			if(is(po.get(0), "BeginWith")) {
				beginWith = true;
			}
			if(po.size() > 3) {
				quantifier = new Quantifier(po.get(2));
			}
			if(po.size() > 4 && is(po.get(3), "EndWith")) {
					endWith = true;
			}
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

}
