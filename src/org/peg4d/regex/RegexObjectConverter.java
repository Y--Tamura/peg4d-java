package org.peg4d.regex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.peg4d.ParsingObject;

public class RegexObjectConverter {

	private ParsingObject po;
	public RegexObjectConverter(ParsingObject po) {
		this.po = po;
	}

	public Map<String, RegexObject> convert() {
		ParsingObject tokens = po.get(0);
		RegSeq rs = new RegSeq();
		for(ParsingObject e: tokens) {
			rs.add(createRegexObject(e));
		}

		Map<String, RegexObject> ret = new HashMap<String, RegexObject>();
		ret.put("TopLevel", rs);
		return ret;
	}

	private RegexObject createRegexObject(ParsingObject e) {
		switch(e.get(1).getTag().toString()) {
		case "Char":
		case "OneOf":
		case "ExceptFor":
			RegexObject ro = new RegCharSet(e);
			ro.addQuantifier(e);
			return ro;
		case "Block":
			RegSeq r = new RegSeq();
			for(ParsingObject blockChild: e.get(1)) {
				r.add(createRegexObject(blockChild));
			}
			r.addQuantifier(e);
			return r;
		default:
			System.err.println("Sorry!!!");
			return null;
		}
	}

	private List<RegexObject> pi(List<RegexObject> ro1, List<RegexObject> ro2) {
		return null;
	}
}
