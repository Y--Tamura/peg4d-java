package org.peg4d.regex;

import org.peg4d.ParsingObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToLongBiFunction;

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
			RegexObject pi = pi(ro, new RegNull());
			RegexObject pi2 = pi2(new RegNull(), pi);
			rules.put("TopLevel", pi2);
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
			 if(last.getChild().get(0) instanceof RegChoice){
				RegChoice target = (RegChoice)last.getChild().get(0);
				ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
				for(RegexObject r: target.getList()){
					sortChoice(rcList, r);
				}
				RegChoice newRC = new RegChoice();
				RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
				Quantifier lastQ;
				Quantifier targetQ;
				if(last.getQuantifier() != null){
					lastQ = last.getQuantifier();
				}else{
					lastQ = null;
				}
				if(target.getQuantifier() != null){
					targetQ = target.getQuantifier();
				}else{
					targetQ = null;
				}
				target.rmQuantifier();
				for(RegexObject r: rcArray){
					RegSeq tmp = new RegSeq();
					tmp.push(pi(r, new RegNull()));
					tmp.push(k);
					RegNonTerminal newRule = new RegNonTerminal(createConvertId());
					newRule.setChild(tmp);
					tmp.setParent(newRule);
					newRC.push(newRule);
					if(targetQ != null){
						tmp.get(0).setQuantifier(targetQ);
					}
					if(lastQ == null){
						rules.put(newRule.toString(), tmp);
					}else{
						RegSeq tmpSq = new RegSeq();
						tmpSq.add(tmp.popHead());
						tmpSq.setQuantifier(lastQ);
						tmp.pushHead(tmpSq);
						rules.put(newRule.toString(), tmp);
					}
				}
				rules.remove(((RegNonTerminal) last).getLabel());
				return pi(e, newRC);
			}else if(last.getChild() instanceof RegSeq){
				RegSeq left = (RegSeq) last.getChild();
				RegexObject converted = pi(left, new RegNull());
				if(last.getQuantifier() != null){
					converted.setQuantifier(last.getQuantifier());
					last.rmQuantifier();
				}
				rules.remove(((RegNonTerminal) last).getLabel());
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
		}else if(last instanceof RegChoice){
			RegChoice target = (RegChoice)last;
			ArrayList<RegexObject> rcList = new ArrayList<RegexObject>();
			for(RegexObject r: target.getList()){
				sortChoice(rcList, r);
			}
			RegChoice newRC = new RegChoice();
			RegexObject[] rcArray = rcList.toArray(new RegexObject[rcList.size()]);
			Quantifier targetQ;
			if(target.getQuantifier() != null){
				targetQ = target.getQuantifier();
			}else{
				targetQ = null;
			}
			target.rmQuantifier();
			rules.remove(target.toString());
			for(RegexObject r: rcArray){
				RegSeq tmp = new RegSeq();
				tmp.push(pi(r, new RegNull()));
				tmp.push(k);
				RegNonTerminal newRule = new RegNonTerminal(createConvertId());
				newRule.setChild(tmp);
				tmp.setParent(newRule);
				newRC.push(newRule);
				if(targetQ != null){
					tmp.setQuantifier(targetQ);
				}
				rules.put(newRule.toString(), tmp);
			}
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
			if(ro.getLetter().length() >= list.get(0).getLetter().length()){
				list.add(0, ro);
			}else if(ro.getLetter().length() < list.get(list.size() -1).getLetter().length()){
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
		RegexObject target = e;
		RegexObject continuation = k;

		if(target instanceof RegNonTerminal && !target.toString().startsWith(rulePrefix)){
			RegNonTerminal targetNT = (RegNonTerminal) target;
			if(targetNT.getIsa()== false && targetNT.getRefer() == false){
				target = targetNT.getChild();
				rules.remove(targetNT.getLabel());
			}
		}
		if(continuation instanceof RegNonTerminal && !continuation.toString().startsWith(rulePrefix)){
			RegNonTerminal continuaitonNT = (RegNonTerminal) continuation;
			if(continuaitonNT.getIsa()== false && continuaitonNT.getRefer() == false){
				continuation = continuaitonNT.getChild();
				rules.remove(continuaitonNT.getLabel());
			}
		}

		if(target.getQuantifier() == null || (target.get(0) != null && target.get(0).getQuantifier() == null)){
			if( k == null || k instanceof RegNull || (k instanceof RegSeq && "".equals(k.toString()))){
				return e;
			}
		}

		if(target instanceof RegSeq && target.getQuantifier() == null && target.size() == 1){
			target = target.get(0);
		}

		if(target instanceof RegSeq && target.getQuantifier() != null && target.size() == 1 && target.get(0).getQuantifier() == null){
			Quantifier targetQ = target.getQuantifier();
			target = target.get(0);
			target.setQuantifier(targetQ);
		}

		if(target != null && (!(target instanceof RegNull) || !(target instanceof RegNull))){
			if(target instanceof RegSeq) {
				Quantifier targetQuantifier = target.getQuantifier();
				if(target.size() > 0){
					RegexObject targetFirst = target.get(0);
					RegexObject targetContinuation = target.getContinuation();
					target = pi2(targetFirst, targetContinuation);
				}
				if(target.getQuantifier() != null && targetQuantifier != null){
					RegSeq newTarget = new RegSeq();
					newTarget.setQuantifier(targetQuantifier);
					newTarget.add(target);
					target = newTarget;
				}else if(targetQuantifier != null){
					target.setQuantifier(targetQuantifier);
				}
			}
		}

		if(target != null && (!(target instanceof RegNull) && !(target instanceof RegNonTerminal))){
			if(target.getQuantifier() != null && !"Times".equals(target.getTag())){
				RegexObject tmp = continuation.popHead();
				RegexObject tmp2 = continuation;
				if(tmp == null){
					return continuationBasedConversion(target, new RegNull(), tmp2);
				}else{
					return pi2(continuationBasedConversion(target, tmp, tmp2), tmp2);
				}
			}
		}

		RegexObject c;
		if(continuation instanceof RegSeq && continuation.getQuantifier() == null){
			c = continuation.popHead();
		}else{
			c = continuation;
			continuation = new RegNull();
		}
		RegSeq left = new RegSeq();
		left.add(target);
		if(c == null){
			left.add(continuation);
			return left;
		}else{
			left.add(c);
			return pi2(left, continuation);
		}
	}

	private RegexObject continuationBasedConversion(RegexObject roLeft, RegexObject roMid ,RegexObject roRight){
		if(roLeft == null){
			roLeft = new RegNull();
		}
		if(roMid == null){
			roMid = new RegNull();
		}
		if(roRight == null){
			roRight = new RegNull();
		}
		RegNonTerminal nt = new RegNonTerminal(createRuleId());
		RegexObject tmp;
		RegexObject continuation = pi2(roMid, roRight);
		Boolean hasRule = false;
		if(continuation instanceof RegSeq && continuation.get(0) != null && continuation.get(0) instanceof RegNonTerminal){
			hasRule = true;
		}

		String tag = roLeft.getTag().toString();
		Quantifier leftQuantifier = roLeft.getQuantifier();
		roLeft.rmQuantifier();
		if(hasRule || continuation == null || continuation instanceof RegNull){
			switch(tag){
			case "ZeroMoreL":
				nt.setChild(roLeft);
				roLeft.setParent(nt);
				roLeft.setQuantifier(leftQuantifier);
				rules.put(nt.toString(), roLeft);
				return nt;
			case "ZeroMoreS": //FIXME?
				nt.setChild(roLeft);
				roLeft.setParent(nt);
				rules.put(nt.toString(), roLeft);
				return nt;
			case "OneMoreL":
				nt.setChild(roLeft);
				roLeft.setParent(nt);
				roLeft.setQuantifier(leftQuantifier);
				rules.put(nt.toString(), roLeft);
				return nt;
			case "OneMoreS": //FIXME?
				RegSeq newRule = new RegSeq();
				RegCharAny newCont = new RegCharAny();
				newRule.add(newCont);
				newRule.look = true;
				newRule.not = true;
				tmp = new RegSeq();
				tmp.add(nt);
				createNewShortestZeroMoreRule(roLeft, newRule, nt);
				tmp.pushHead(roLeft);
				return tmp;
			case "OptionalL":
				nt.setChild(roLeft);
				roLeft.setParent(nt);
				roLeft.setQuantifier(leftQuantifier);
				rules.put(nt.toString(), roLeft);
				return nt;
			case "OptionalS":
				nt.setChild(roLeft);
				roLeft.setParent(nt);
				roLeft.setQuantifier(new Quantifier("?"));
				rules.put(nt.toString(), roLeft);
				return nt;
			default:
				System.err.print("Sorry!! An error occurred on conversion(NT).");
				return null;
			}
		}else{
			switch(tag){
			case "ZeroMoreL":	//a*a
				tmp = nt;
				createNewLongestZeroMoreRule(roLeft, continuation, nt);
				return tmp;
			case "ZeroMoreS":	//a*?a
				tmp = nt;
				createNewShortestZeroMoreRule(roLeft, continuation, nt);
				return tmp;
			case "OneMoreL":	//a+a
				tmp = new RegSeq();
				tmp.add(nt);
				createNewLongestZeroMoreRule(roLeft, continuation, nt);
				tmp.pushHead(roLeft);
				return tmp;
			case "OneMoreS":	//a+?a
				tmp = new RegSeq();
				tmp.add(nt);
				createNewShortestZeroMoreRule(roLeft, continuation, nt);
				tmp.pushHead(roLeft);
				return tmp;
			case "OptionalL":	//a?a
				tmp = nt;
				createNewLongestOptionalRule(roLeft, continuation, nt);
				return tmp;
			case "OptionalS": 	//a??a
				tmp = nt;
				createNewShortestOptionalRule(roLeft, continuation, nt);
				return tmp;
			default:
				System.err.print("Sorry!! An error occurred on conversion(NT).");
				return null;
			}
		}
	}

	private void createNewLongestZeroMoreRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a E0 / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(pi2(rHead, nt));
		RegSeq s2 = new RegSeq();
		s2.add(rTail);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		nt.setChild(newRule);
		newRule.setParent(nt);
		rules.put(nt.toString(), newRule);
	}

	private void createNewShortestZeroMoreRule(RegexObject rHead, RegexObject rTail, RegNonTerminal nt) {
		//E0 = a / a E0
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rTail);
		RegSeq s2 = new RegSeq();
		s2.add(pi2(rHead, nt));
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		nt.setChild(newRule);
		newRule.setParent(nt);
		rules.put(nt.toString(), newRule);
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
		nt.setChild(newRule);
		newRule.setParent(nt);
		rules.put(nt.toString(), newRule);
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
		nt.setChild(newRule);
		newRule.setParent(nt);
		rules.put(nt.toString(), newRule);
	}

	private void createTerminalSequence(RegChoice newRule, RegexObject rHead, RegNonTerminal nt) {
		newRule.add(rHead);
		newRule.add(nt);
		rules.put(nt.toString(), newRule);
	}

}
