package org.peg4d.regex;

import org.peg4d.ParsingObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class RegexObjectGenerator {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	private int ruleId = 1;
	private int convertId = 1;
	private int blockId = 1;
	private int groupId = 1;
	private final static String rulePrefix = "R";
	private final static String convertPrefix = "E";
	private final static String blockPrefix = "B";
	private final static String groupPrefix = "G";

	public RegexObjectGenerator(ParsingObject po){
		this.po = po;
	}

	private String createConvertId(){
		return convertPrefix + convertId++;
	}

	private String createRuleId(){
		return rulePrefix + ruleId++;
	}

	private String createBlockId(){
		return blockPrefix + blockId++;
	}

	private String createGroupId(){
		return groupPrefix + groupId++;
	}

	public Map<String, RegexObject> convert(){
		ParsingObject token = po.get(0);
		RegSeq regex = new RegSeq();
		if("Regex".equals(token.getTag().toString())){
			rules = new TreeMap<String, RegexObject>();
			RegexObject ro = generate(regex, token);
//			rules.put("TopLevel", pi(ro, new RegNull()));
//			rules.put("TopLevel", pi2(new RegNull(), ro));
			rules.put("TopLevel", pi2(new RegNull(), pi(ro, new RegNull())));
			return rules;
		}else{
			System.err.println("The input file isn't a regex file.");
			return null;
		}
	}

	private RegSeq generate(RegSeq regex, ParsingObject po){
		ParsingObject token;
		for(int i = 0; i < po.size(); i++){
			token = po.get(i);
			switch(token.getTag().toString()){
			case "Or":
				RegChoice or = new RegChoice();
				createOrSequence(or, token);
				regex.add(or);
				break;
			case "Token":
				regex.add(createTokenObject(token));
				break;
			case "Stmt":
				regex.add(createStmtObject(token));
				break;
			}
		}
		return regex;
	}

	private void createOrSequence(RegChoice rc, ParsingObject po){
		if("Or".equals(po.get(0).getTag().toString())){
			createOrSequence(rc, po.get(0));
		}else{
			RegSeq rs0 = new RegSeq();
			rc.add(generate(rs0, po.get(0)));
		}
		RegSeq rs1 = new RegSeq();
		rc.add(generate(rs1, po.get(1)));
	}

	private RegexObject createTokenObject(ParsingObject po){
		RegexObject ro;

		switch(po.get(1).getTag().toString()){
		case "Char":
		case "EscapedChar":
			ro = new RegCharSet(po);
			break;
		case "WildCard":
			ro = new RegCharAny(po);
			break;
		case "OneOf":
		case "ExceptFor":
			ro = new RegCharBracket(po);
			break;
		case "BlockReference":
			int refId = Integer.parseInt(po.get(1).get(0).getText());
			RegexObject refer = rules.get(blockPrefix + refId);
			if(refer == null){
				ro = new RegNull();
				System.err.println("An undefined reference $" + refId + " is written in the regex file.");
			}else{
				ro = new RegNonTerminal(blockPrefix + refId);
				((RegNonTerminal) ro).setIsa();
				((RegNonTerminal) refer.getParent()).setRefer(true);
				((RegNonTerminal) refer.getParent()).setDefName();;
			}
			break;
		default:
			ro = new RegNull();
			System.err.println(po.get(1).getTag().toString() + " is unsupported tag.");
		}

		if("BeginWith".equals(po.get(0).getTag().toString())){
			ro.beginWith = true;
		}
		if("EndWith".equals(po.get(0).getTag().toString())){
			ro.endWith = true;
		}

		ro.addQuantifier(po);
		return ro;
	}

	private RegexObject createStmtObject(ParsingObject po){
		RegexObject ro;
		RegSeq stmt = new RegSeq();
		switch(po.get(1).getTag().toString()){
		case "Block":
			ro = new RegNonTerminal(createBlockId());
			ro.setChild(generate(stmt, po.get(1)));
			stmt.setParent(ro);
			rules.put(ro.toString(), stmt);
			break;
		case "Group":
			ro = new RegNonTerminal(createGroupId());
			ro.setChild(generate(stmt, po.get(1)));
			stmt.setParent(ro);
			rules.put(ro.toString(), stmt);
			break;
		case "Comment":
			ro = new RegNull();
			break;
		case "LookBehind":
		case "NegLookBehind":
			//FIXME
			ro = new RegNull();
			break;
		case "NegLookAhead":
			stmt.not = true;
		case "LookAhead":
			stmt.look = true;
			ro = new RegNonTerminal(createGroupId());
			ro.setChild(generate(stmt, po.get(1)));
			stmt.setParent(ro);
			rules.put(ro.toString(), stmt);
			break;
		default:
			ro = new RegNull();
			System.err.println(po.get(1).getTag().toString() + " is unsupported tag.");
		}

		if("BeginWith".equals(po.get(0).getTag().toString())){
			ro.beginWith = true;
		}
		if("EndWith".equals(po.get(0).getTag().toString())){
			ro.endWith = true;
		}

		ro.addQuantifier(po);
		return ro;
	}

	private RegexObject pi(RegexObject e, RegexObject k){
		RegexObject last = e.pop();
		if(last == null){
			return k;
		}
		if(last instanceof RegNonTerminal && (((RegNonTerminal) last).getIsa() || ((RegNonTerminal) last).getRefer())){
			//FIXME
			if(k instanceof RegSeq){
				k.pushHead(last);
				return pi(e, k);
			}else{
				RegSeq unit = new RegSeq();
				unit.push(k);
				unit.pushHead(last);
				return pi(e, unit);
			}
		}
		if(last instanceof RegNonTerminal && !last.toString().startsWith(convertPrefix)){
			 if(last.getChild() instanceof RegSeq){
					RegSeq left = (RegSeq) last.getChild();
					RegexObject converted = pi(left, new RegNull());
					if(last.getQuantifier() != null){
						converted.setQuantifier(last.getQuantifier());
						last.rmQuantifier();
					}
					rules.remove(last.toString());
					if(k instanceof RegSeq){
						k.pushHead(converted);
						return pi(e, k);
					}else{
						RegSeq unit = new RegSeq();
						unit.push(k);
						unit.pushHead(converted);
						return pi(e, unit);
					}
				}
			 else if(last.getChild().get(0) instanceof RegChoice){
				RegChoice target = (RegChoice)last.getChild().get(0);
				ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
				for(RegexObject r: target.getList()){
					sortChoice(rcList, r);
				}
				RegChoice newRC = new RegChoice();
				RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
				Quantifier lastQ;
				if(last.getQuantifier() != null){
					lastQ = last.getQuantifier();
				}else{
					lastQ = null;
				}
				for(RegexObject r: rcArray){
					RegSeq tmp = new RegSeq();
					tmp.push(pi(r, new RegNull()));
					tmp.push(k);
					RegNonTerminal newRule = new RegNonTerminal(createConvertId());
					newRule.setChild(tmp);
					tmp.setParent(newRule);
					newRC.push(newRule);
					if(lastQ != null){
						tmp.setQuantifier(lastQ);
					}
					rules.put(newRule.toString(), tmp);
				}
				last.rmQuantifier();
				rules.remove(last.toString());
				return pi(e, newRC);
			}
		}else if(last instanceof RegChoice){
			RegChoice target = (RegChoice)last;
			ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
			for(RegexObject r: target.getList()){
				sortChoice(rcList, r);
			}
			RegChoice newRC = new RegChoice();
			RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
			Quantifier lastQ;
			if(last.getQuantifier() != null){
				lastQ = last.getQuantifier();
			}else{
				lastQ = null;
			}
			for(RegexObject r: rcArray){
				RegSeq tmp = new RegSeq();
				tmp.push(pi(r, new RegNull()));
				tmp.push(k);
				RegNonTerminal newRule = new RegNonTerminal(createConvertId());
				newRule.setChild(tmp);
				tmp.setParent(newRule);
				newRC.push(newRule);
				if(lastQ != null){
					tmp.setQuantifier(lastQ);
				}
				rules.put(newRule.toString(), tmp);
			}
			last.rmQuantifier();
			return pi(e, newRC);
		}
		if(k instanceof RegSeq){
			k.pushHead(last);
			return pi(e, k);
		}else{
			RegSeq unit = new RegSeq();
			unit.push(k);
			unit.pushHead(last);
			return pi(e, unit);
		}
	}

	private void sortChoice(ArrayList<RegexObject> list, RegexObject ro){
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

	private RegexObject pi2(RegexObject e, RegexObject k){
		if( k == null || k instanceof RegNull){
			return e;
		}
		RegexObject target = e;
		RegexObject continuation = k;

		if(target instanceof RegNonTerminal && !target.toString().startsWith(rulePrefix)){
			RegNonTerminal targetNT = (RegNonTerminal) target;
			if(targetNT.getIsa()== false && targetNT.getRefer() == false){
				target = targetNT.getChild();
			}
		}
		if(continuation instanceof RegNonTerminal && !target.toString().startsWith(rulePrefix)){
			RegNonTerminal continuaitonNT = (RegNonTerminal) continuation;
			if(continuaitonNT.getIsa()== false && continuaitonNT.getRefer() == false){
				target = continuaitonNT.getChild();
			}
		}

		if(target instanceof RegSeq && target.size() == 1){
			target = e.get(0);
		}

		if(target instanceof RegSeq &&  !(target == null || target instanceof RegNull)) {
			RegexObject targetLast = target.pop();
			if(target.not == continuation.not && targetLast.contains(continuation)){
				if(targetLast.getQuantifier() != null && !"Times".equals(targetLast.getTag())){
					RegexObject tmp = continuation.popHead();
					RegexObject tmp2 = continuation;
					if(tmp == null){
						target.push(continuationBasedConversion(targetLast, tmp, tmp2));
						return target;
					}else{
						target.push(continuationBasedConversion(targetLast, tmp, tmp2));
						target.push(tmp2);
						return pi2(target, continuation);
					}
				}else{
					target.push(targetLast);
				}
			}else{
				target.push(targetLast);
			}
		}else{
			if(target.not == continuation.not && target.contains(continuation)){
				if(target.getQuantifier() != null && !"Times".equals(target.getTag())){
					RegexObject tmp = continuation.popHead();
					RegexObject tmp2 = continuation;
					if(tmp == null){
						return continuationBasedConversion(target, tmp, tmp2);
					}else{
						return pi2(continuationBasedConversion(target, tmp, tmp2), tmp2);
					}
				}
			}

		}

		RegexObject c = continuation.popHead();
		if(target instanceof RegSeq){
			if(c == null){
				target.add(continuation);
				return target;
			}
			target.push(c);
			return pi2(target, continuation);
		}else{
			RegSeq left = new RegSeq();
			left.add(e);
			if(c == null){
				left.add(continuation);
				return left;
			}else{
				left.add(c);
				return pi2(left, continuation);
			}
		}
	}

/*	private RegexObject continuationBasedConversion(RegexObject Left, RegexObject Mid, RegexObject Right) {
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		RegexObject tmp;
		String tag = Left.getTag();
		Left.rmQuantifier();
		Right.pushHead(nt);
		switch(tag){
		case "ZeroMoreL":	//a*a
			tmp = nt;
			createNewLongestZeroMoreRule(Left, Mid, nt);
			return tmp;
		case "ZeroMoreS":	//a*?a
			tmp = nt;
			createNewShortestZeroMoreRule(Mid, Left, nt);
			return tmp;
		case "OneMoreL":	//a+a
			tmp = new RegSeq();
			tmp.add(nt);
			createNewLongestZeroMoreRule(Left, Mid, nt);
			tmp.pushHead(Left);
			return tmp;
		case "OneMoreS":	//a+?a
			tmp = new RegSeq();
			tmp.add(nt);
			createNewShortestZeroMoreRule(Left, Mid, nt);
			tmp.pushHead(Left);
			return Right;
		case "OptionalL":	//a?a
			tmp = nt;
			createNewLongestOptionalRule(Left, Mid, nt);
			return tmp;
		case "OptionalS": 	//a??a
			tmp = nt;
			createNewShortestOptionalRule(Left, Mid, nt);
			return tmp;
		default:
			System.err.print("Sorry!! An error occurred on conversion.");
			return null;
		}
	}
*/
	private RegexObject continuationBasedConversion(RegexObject roLeft, RegexObject roMid ,RegexObject roRight){
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		RegexObject tmp;
		switch(roLeft.getTag()){
		case "ZeroMoreL":	//a*a
			roLeft.rmQuantifier();
			roRight.pushHead(nt);
			tmp = nt;
			createNewLongestZeroMoreRule(roLeft, roMid, nt);
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
