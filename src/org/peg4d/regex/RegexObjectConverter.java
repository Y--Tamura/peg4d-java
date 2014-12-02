package org.peg4d.regex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.peg4d.ParsingObject;

public class RegexObjectConverter {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	public RegexObjectConverter(ParsingObject po) {
		this.po = po;
	}

	public Map<String, RegexObject> convert() {
		ParsingObject tokens = po.get(0);
		RegSeq rs = new RegSeq();
		for(ParsingObject e: tokens) {
			rs.add(createRegexObject(e));
		}

		rules = new HashMap<String, RegexObject>();
		RegexObject continuation = rs.popContinuation();
		pi(rs, continuation);
		rules.put("TopLevel", rs);
		return rules;
	}

	private RegexObject createRegexObject(ParsingObject e) {
		switch(e.getTag().toString()){
		case "Or":
			RegChoice roOr = new RegChoice();
			RegSeq r1 = new RegSeq();
			for(ParsingObject blockChild: e.get(0)) {
				r1.add(createRegexObject(blockChild));
			}
			RegSeq r2 = new RegSeq();
			for(ParsingObject blockChild: e.get(1)) {
				r2.add(createRegexObject(blockChild));
			}
			roOr.add(r1);
			roOr.add(r2);
			return roOr;
		case "Item":
			RegSeq roItem = new RegSeq();
			for(ParsingObject blockChild: e.get(0)) {
				roItem.add(createRegexObject(blockChild));
			}
			return roItem;
		default: {
			switch(e.get(1).getTag().toString()) {
			case "Char":
			case "OneOf":
			case "ExceptFor":
				RegexObject roChar = new RegCharSet(e);
				roChar.addQuantifier(e);
				return roChar;
			case "Block":
				RegSeq roBlock = new RegSeq();
				for(ParsingObject blockChild: e.get(1)) {
					roBlock.add(createRegexObject(blockChild));
				}
				roBlock.addQuantifier(e);
				return roBlock;
			default:
				System.err.println("Sorry!!!");
				return null;
			}
		}
		}
	}

	private RegexObject pi(RegexObject target, RegexObject continuation) {
		int target_size = target.size();
		if(target_size == 0) {
			return continuation;
		}
		else if(target_size == 1) {
			RegexObject child = target.get(0);
			if(child instanceof RegChoice) {
				//(a|b)c -> pi(a, c) / pi(b, c)
				RegexObject r1 = child.get(0);
				RegexObject r2 = child.get(1);
				RegChoice r = new RegChoice();
				r.add(pi(r1, continuation));
				r.add(pi(r2, continuation));
				return r;
			}
			else if(child instanceof RegSeq) {
				//pi((ab), c) -> pi(ab, c)
				return pi(child, continuation);
			}
			else {
				target.concat(continuation);
				return target;
			}
		}
		else {
			RegexObject c2 = target.popContinuation();
			return pi(target, pi(c2, continuation));
		}
	}
}
