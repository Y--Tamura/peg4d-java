package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected List<RegexObject> l;
	protected ParsingObject ref;
	protected Quantifier quantifier;
	protected RegexObject parent;
	public boolean beginWith;
	public boolean endWith;

	public RegexObject(ParsingObject po) {
		parent = null;
		beginWith = false;
		endWith = false;
		ref = po;
		l = new ArrayList<RegexObject>();
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

	public void add(RegexObject e) {
		l.add(e);
	}

	public int size() {
		return l.size();
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

}
