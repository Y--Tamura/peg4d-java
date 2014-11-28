package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public class RegList extends RegexObject {

	protected List<RegexObject> l;

	public RegList(ParsingObject po) {
		super(po);
		l = new ArrayList<RegexObject>();
	}

	public void add(RegexObject e) {
		l.add(e);
	}

	public int size() {
		return l.size();
	}

}
