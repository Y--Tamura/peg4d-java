package org.peg4d.regex;

import java.util.HashMap;
import java.util.Map;

import org.peg4d.ParsingObject;

public class RegexObjectConverter {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	private int ruleId = 0;
	private final static String rulePrefix = "E";

	public RegexObjectConverter(ParsingObject po) {
		this.po = po;
	}

	private String createId() {
		return rulePrefix + ruleId++;
	}

	public Map<String, RegexObject> convert() {
		ParsingObject tokens = po.get(0);
		RegSeq rs = new RegSeq();
		for(ParsingObject e: tokens) {
			rs.add(createRegexObject(e));
		}

		rules = new HashMap<String, RegexObject>();
		RegexObject continuation = rs.popContinuation();
		RegexObject top = pi(rs, continuation);
		rules.put("TopLevel", top);
		return rules;
	}

	private RegSeq createSequence(ParsingObject po) {
		RegSeq r = new RegSeq();
		for(ParsingObject child: po) {
			r.add(createRegexObject(child));
		}
		return r;
	}

	private RegexObject createRegexObject(ParsingObject e) {
		switch(e.getTag().toString()){
		case "Or":
			RegChoice roOr = new RegChoice();
			roOr.add(createSequence(e.get(0)));
			roOr.add(createSequence(e.get(1)));
			return roOr;
		case "Item":
			return createSequence(e.get(0));
		default: {
			switch(e.get(1).getTag().toString()) {
			case "Char":
			case "OneOf":
			case "ExceptFor":
				return new RegCharSet(e);
			case "Block":
				RegSeq roBlock = createSequence(e.get(1));
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
				if(child instanceof RegCharSet && child.isZeroMore()) {
					int continuation_size = continuation.size();
					if(continuation_size > 0) {
						RegexObject rHead = continuation.get(0);
						RegCharSet charSet = (RegCharSet)child;
						if(rHead instanceof RegCharSet && charSet.contains(rHead)) {
							RegCharSet rHeadChar = (RegCharSet)continuation.popHead();
							RegNonTerminal nt = new RegNonTerminal(createId());
							continuation.pushHead(nt);
							createNewZeroMoreRule(rHeadChar, nt);
							return continuation;
						}
					}
				}
				target.concat(continuation);
				return target;
			}
		}
		else {
			RegexObject c2 = target.popContinuation();
			return pi(target, pi(c2, continuation));
		}
	}

	private void createNewZeroMoreRule(RegCharSet rHeadChar, RegNonTerminal nt) {
		//E0 = a E0 / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHeadChar);
		s1.add(nt);
		RegSeq s2 = new RegSeq();
		s2.add(rHeadChar);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}
}
