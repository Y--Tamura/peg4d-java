package org.peg4d.regex;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

import org.peg4d.ParsingObject;

public class RegexObjectConverter {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	private int ruleId = 1;
	private final static String rulePrefix = "E";
	private int blockId = 1;
	private final static String blockPrefix = "B";
	private int groupId = 1;
	private final static String groupPrefix = "G";

	public RegexObjectConverter(ParsingObject po) {
		this.po = po;
	}

	private String createRuleId() {
		return rulePrefix + ruleId++;
	}

	private String createBlockId(){
		return blockPrefix + blockId++;
	}

	private String createGroupId(){
		return groupPrefix + groupId++;
	}

	public Map<String, RegexObject> convert() {
		ParsingObject tokens = po.get(0);
		RegSeq rs = new RegSeq();
		rules = new TreeMap<String, RegexObject>();
		for(ParsingObject e: tokens) {
			rs.add(createRegexObject(e));
		}
		rs.pushRegNull();
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
			createOrSequence(roOr, e);
			return roOr;
		case "Item":
			return createSequence(e.get(0));
		default: {
			switch(e.get(1).getTag().toString()) {
			case "Char":
			case "EscapedChar":
				return new RegCharSet(e);
			case "WildCard":
				return new RegCharAny(e);
			case "OneOf":
			case "ExceptFor":
				return new RegCharBracket(e);
			case "Block":
				RegNonTerminal rnBlock = new RegNonTerminal(createBlockId());
				RegSeq rsBlock = createSequence(e.get(1));
				rsBlock.setParent(rnBlock);
				rnBlock.setChild(rsBlock);
				rnBlock.setDefName();
				this.rules.put(rnBlock.toString(), rsBlock);
				rnBlock.addQuantifier(e);
				return rnBlock;
			case "Group":
				RegNonTerminal rnGroup = new RegNonTerminal(createGroupId());
				RegSeq rsGroup = createSequence(e.get(1));
				rsGroup.setParent(rnGroup);
				rnGroup.setChild(rsGroup);
				this.rules.put(rnGroup.toString(), rsGroup);
				rnGroup.addQuantifier(e);
				return rnGroup;
			case "BlockReference":
				int refId = Integer.parseInt(e.get(1).get(0).getText());
				String ntName = blockPrefix + refId;
				RegexObject roBRefer = rules.get(ntName);
				if(roBRefer != null && (roBRefer.getParent()) instanceof RegNonTerminal){
					RegNonTerminal rnBRefer = new RegNonTerminal(ntName);
					rnBRefer.addQuantifier(e);
					rnBRefer.setIsa();
					((RegNonTerminal) roBRefer.getParent()).setRefer(true);
					return rnBRefer;
				}
				else {
					System.err.println("An undefined reference.");
					return null;
				}
			default:
				System.err.println("Sorry!! An error occurred on 'createRegexObject'.");
				return null;
			}
		}
		}
	}

	private void createOrSequence(RegChoice roOr, ParsingObject e) {
		if(e.get(0).getTag().toString().equals("Or")){
			createOrSequence(roOr, e.get(0));
		}else{
			roOr.add(createSequence(e.get(0)));
		}
		if(e.get(1).getTag().toString().equals("Or")){
			createOrSequence(roOr, e.get(1));
		}else{
			roOr.add(createSequence(e.get(1)));
		}
	}

	private RegexObject pi(RegexObject target, RegexObject continuation) {
		int target_size = target.size();
		if(target_size == 0) {
			//(1)
			return continuation;
		}
		if(target_size == 1) {
			RegexObject child = target.get(0);
			if( child instanceof RegNonTerminal && child.getChild() != null && child.getChild() instanceof RegSeq && child.getChild().get(0) instanceof RegChoice ) {
				//(4)
				//(a|b)c -> pi(a, c) / pi(b, c)
				RegChoice targetRC = (RegChoice)child.getChild().get(0);
				ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
				for(RegexObject r: targetRC.getList()){
					sortChoice(rcList, r);
				}
				RegChoice newRC = new RegChoice();
				RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
				for(RegexObject r: rcArray){
					RegSeq tmp = new RegSeq();
					tmp.add(r);
					tmp.add(continuation);
					newRC.pushHead(tmp);
				}
//				if(continuation instanceof RegSeq && continuation.get(0) instanceof RegNonTerminal && continuation.get(0).getChild() != null && continuation.get(0).getChild() instanceof RegSeq && continuation.get(0).getChild().get(0) instanceof RegChoice ){
//					continuation = pi(continuation.get(0), new RegNull());
//				}
				child.setChild(newRC);
				rules.put(child.toString(), newRC);
				return child;
			}
			else if(child instanceof RegNonTerminal || ( continuation.size() > 0 && continuation.get(0) instanceof RegNonTerminal)){
				if(child instanceof RegNonTerminal && continuation.size() > 0 && continuation.get(0) instanceof RegNonTerminal){
					if(child.getChild() != null && continuation.get(0).getChild() != null && child.getChild().not == continuation.get(0).getChild().not && ((RegSeq) child.getChild()).contains(continuation.get(0).getChild())){
						if(child.getQuantifier() != null && !"Times".equals(child.getTag())){
							// (a)*(a)
							RegSeq sq = continuation.getContinuation();
							continuation = continuationBasedConversionNT(child, continuation.get(0), continuation);
							child.getChild().rmQuantifier();
							sq.pushHead(continuation);
							return sq;
						}
					}
				}
				else if(child instanceof RegNonTerminal){
					if(child.getChild() != null && continuation instanceof RegSeq && child.getChild().not == continuation.not && ((RegSeq) child.getChild()).contains(continuation)){
						if(child.getQuantifier() != null && !"Times".equals(child.getTag())){
							//(a)*a
//							RegSeq sq = continuation.getContinuation();
//							sq.pushHead(continuation.get(0));
							continuation.pushHead(child);
							RegexObject tmp = continuation;
							continuation = continuationBasedConversionNT(child, continuation, tmp);
//							sq.pushHead(continuation);
							return continuation;
						}
					}
				}
				else if( continuation.size() > 0 && continuation.get(0) instanceof RegNonTerminal){
					if(child instanceof RegCharSet && continuation.get(0).getChild() != null && child.not == continuation.get(0).getChild().not && ((RegCharSet) child).contains(continuation.get(0).getChild())){
						if(child.getQuantifier() != null && !"Times".equals(child.getTag())){
							//a*(a)
							RegSeq sq = continuation.getContinuation();
							continuation = continuationBasedConversionNT(child, continuation.get(0), continuation);
							child.rmQuantifier();
							sq.pushHead(continuation);
							return sq;
							}
					}
				}
				else{
					System.err.println("Sorry!! An error occurred on pi(NT).");
					return null;
				}
				target.concat(continuation);
				return target;
			}
			else if(child instanceof RegChoice) {
				//(4)
				//(a|b)c -> pi(a, c) / pi(b, c)
				RegChoice targetRC = (RegChoice)child;
				ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
				for(RegexObject r: targetRC.getList()){
					sortChoice(rcList, r);
				}
				RegChoice newRC = new RegChoice();
				RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
				for(RegexObject r: rcArray){
					RegSeq tmp = new RegSeq();
					tmp.add(r);
					tmp.add(continuation);
					newRC.pushHead(tmp);
				}
				return newRC;
			}
			else if(child instanceof RegSeq) {
				//pi((ab), c) -> pi(ab, c)
				//return pi(child, continutaion);
				if(!(continuation instanceof RegSeq)){
					return pi(child, continuation);
				}
				else if(((RegSeq) child).contains(continuation) && child.not == continuation.not ){
					if(child.getQuantifier() != null && !"Times".equals(child.getTag())){
						continuation.pushHead(child);
						continuation = continuationBasedConversion((RegSeq) child, continuation);
						child.rmQuantifier();
						return continuation.get(0);
					}
					else{
						target.concat(continuation);
						return target;
					}
				}
				else if(child.getQuantifier() != null){
					continuation.pushHead(child);
					return continuation;
				}
				else{
					return pi(child, continuation);
				}
			}
			else if(child instanceof RegCharSet && continuation.size() > 0){
				RegexObject rHead = continuation.get(0);
				RegCharSet charSet = (RegCharSet)child;
//				if(rHead instanceof RegCharSet && rHead.not == charSet.not && charSet.contains(rHead)){
				if(rHead instanceof RegCharSet && rHead.not == charSet.not && rHead.getLetter().startsWith(charSet.getLetter())){
					if(charSet.getQuantifier() != null && !"Times".equals(charSet.getTag())){
						return continuationBasedConversionCS(charSet, charSet, continuation);
					}
					else{
						target.concat(continuation);
						return target;
					}
				}
				//(2)
				else{
				target.concat(continuation);
				return target;
				}
			}
			else{
				target.concat(continuation);
				return target;
//				System.err.println("Sorry!! An error occurred on 'pi'.");
//				return null;
			}
		}
		else {
			//(3)
			RegexObject c2 = target.popContinuation();
			return pi(target, pi(c2, continuation));
		}
	}

	private void sortChoice(ArrayList<RegexObject> list, RegexObject ro) {
		if(list.size() == 0){
			list.add(ro);
		}else{
			if(ro.getLetter().length() <= list.get(0).getLetter().length()){
				list.add(0, ro);
			}else if(ro.getLetter().length() > list.get(list.size() -1).getLetter().length()){
				list.add(ro);
			}else{
				int i;
				for(i = 0; i < list.size() -1; i++){
					if(ro.getLetter().length() == list.get(i).getLetter().length()) break;
				}
				list.add(i, ro);
			}
		}
	}

	private RegexObject continuationBasedConversionNT(RegexObject roLeft, RegexObject roMid ,RegexObject roRight){
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		RegexObject tmp;
		switch(roLeft.getTag()){
		case "ZeroMoreL":	//a*a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = nt;
			createNewLongestZeroMoreRule(roLeft, roMid, nt);
			roRight.popHead();
			roRight.popHead();
			return tmp;
		case "ZeroMoreS":	//a*?a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = nt;
			createNewShortestZeroMoreRule(roMid, roLeft, nt);
			roRight.popHead();
			roRight.popHead();
			return tmp;
		case "OneMoreL":	//a+a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = new RegSeq();
			tmp.add(nt);
			createNewLongestZeroMoreRule(roLeft, roMid, nt);
			roRight.popHead();
			roRight.popHead();
			tmp.pushHead(roLeft);
			return tmp;
		case "OneMoreS":	//a+?a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = new RegSeq();
			tmp.add(nt);
			createNewShortestZeroMoreRule(roLeft, roMid, nt);
			roRight.popHead();
			roRight.popHead();
			tmp.pushHead(roLeft);
			return tmp;
		case "OptionalL":	//a?a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = nt;
			createNewLongestOptionalRule(roLeft, roMid, nt);
			roRight.popHead();
			roRight.popHead();
			return tmp;
		case "OptionalS": 	//a??a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = nt;
			createNewShortestOptionalRule(roLeft, roMid, nt);
			roRight.popHead();
			roRight.popHead();
			return tmp;
		default:
			System.err.print("Sorry!! An error occurred on conversion(NT).");
			return null;
		}
	}

	private RegexObject continuationBasedConversion(RegSeq rcLeft, RegexObject roRight){
		RegSeq rHeadSeq = (RegSeq)roRight.popHead();
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		switch(rcLeft.getTag()){
		case "ZeroMoreL":	//a*a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadSeq, nt);
			return roRight;
		case "ZeroMoreS":	//a*?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadSeq, nt);
			return roRight;
		case "OneMoreL":	//a+a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadSeq, nt);
			roRight.pushHead(rHeadSeq);
			return roRight;
		case "OneMoreS":	//a+?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadSeq, nt);
			roRight.pushHead(rHeadSeq);
			return roRight;
		case "OptionalL":	//a?a
			roRight.pushHead(nt);
			createNewLongestOptionalRule(rHeadSeq, nt);
			return roRight;
		case "OptionalS": 	//a??a
			roRight.pushHead(nt);
			createNewShortestOptionalRule(rHeadSeq, nt);
			return roRight;
		default:
			System.err.print("Sorry!! An error occurred on conversion(seq).");
			return null;
		}
	}

	private RegexObject continuationBasedConversionCS(RegCharSet rcLeft, RegexObject roMid, RegexObject roRight){
		RegCharSet rHeadChar = (RegCharSet)roRight.popHead();
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		String tag = roMid.getTag();
		roMid.rmQuantifier();
		roRight.pushHead(nt);
		switch(tag){
		case "ZeroMoreL":	//a*a
			createNewLongestZeroMoreRule(roMid, rHeadChar, nt);
			return roRight;
		case "ZeroMoreS":	//a*?a
			createNewShortestZeroMoreRule(rHeadChar, roMid, nt);
			return roRight;
		case "OneMoreL":	//a+a
			createNewLongestZeroMoreRule(roMid, rHeadChar, nt);
			roRight.pushHead(roMid);
			return roRight;
		case "OneMoreS":	//a+?a
			createNewShortestZeroMoreRule(roMid, rHeadChar, nt);
			roRight.pushHead(roMid);
			return roRight;
		case "OptionalL":	//a?a
			createNewLongestOptionalRule(roMid, rHeadChar, nt);
			return roRight;
		case "OptionalS": 	//a??a
			createNewShortestOptionalRule(roMid, rHeadChar, nt);
			return roRight;
		default:
			System.err.print("Sorry!! An error occurred on conversion(charset).");
			return null;
		}
	}

	private void createNewLongestZeroMoreRule(RegexObject rHead, RegNonTerminal nt) {
		this.createNewLongestZeroMoreRule(rHead, rHead, nt);
	}

	private void createNewLongestZeroMoreRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a E0 / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		s1.add(nt);
		RegSeq s2 = new RegSeq();
		s2.add(rTail);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewShortestZeroMoreRule(RegexObject rHead, RegNonTerminal nt) {
		this.createNewShortestZeroMoreRule(rHead, rHead, nt);
	}

	private void createNewShortestZeroMoreRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a / a E0
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rTail);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		s2.add(nt);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewLongestOptionalRule(RegexObject rHead, RegNonTerminal nt) {
		this.createNewLongestOptionalRule(rHead, rHead, nt);
	}

	private void createNewLongestOptionalRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a a / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		s1.add(rTail);
		RegSeq s2 = new RegSeq();
		s2.add(rTail);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewShortestOptionalRule(RegexObject rHead, RegNonTerminal nt) {
		this.createNewShortestOptionalRule(rHead, rHead, nt);
	}

	private void createNewShortestOptionalRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a / a a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rTail);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		s2.add(rTail);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

}
