package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected List<RegexObject> list;
	protected ParsingObject ref;
	protected Quantifier quantifier;
	protected RegexObject parent;
	public boolean beginWith;
	public boolean endWith;

	public RegexObject(ParsingObject po) {
		this(po, null);
	}

	public RegexObject(ParsingObject po, RegexObject parent) {
		this.parent = parent;
		this.beginWith = false;
		this.endWith = false;
		this.ref = po;
		this.list = new ArrayList<RegexObject>();
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
		this.list.add(e);
	}

	public List<RegexObject> getList(){
		return this.list;
	}

	public int size() {
		return this.list.size();
	}

	public ParsingObject getRef(){
		return this.ref;
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

}
