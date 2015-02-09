package org.peg4d.regex;

import org.peg4d.ParsingObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class RegexObjectGenerator {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	private int ruleId = 1;
	private int blockId = 1;
	private int groupId = 1;
	private final static String rulePrefix = "E";
	private final static String blockPrefix = "B";
	private final static String groupPrefix = "G";

	public RegexObjectGenerator(ParsingObject po){
		this.po = po;
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
			rules.put("TopLevel", pi(ro, new RegNull()));
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
		if(last instanceof RegNonTerminal && !last.toString().startsWith(rulePrefix)){
			 if(last.getChild() instanceof RegSeq){
					RegSeq left = (RegSeq) last.getChild();
					RegexObject converted = pi(left, new RegNull());
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
				for(RegexObject r: rcArray){
					RegSeq tmp = new RegSeq();
					tmp.push(pi(r, new RegNull()));
					tmp.push(k);
					RegNonTerminal newRule = new RegNonTerminal(createRuleId());
					newRule.setChild(tmp);
					tmp.setParent(newRule);
					newRC.push(newRule);
					rules.put(newRule.toString(), tmp);
				}
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
			for(RegexObject r: rcArray){
				RegSeq tmp = new RegSeq();
				tmp.push(pi(r, new RegNull()));
				tmp.push(k);
				RegNonTerminal newRule = new RegNonTerminal(createRuleId());
				newRule.setChild(tmp);
				tmp.setParent(newRule);
				newRC.push(newRule);
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
}
