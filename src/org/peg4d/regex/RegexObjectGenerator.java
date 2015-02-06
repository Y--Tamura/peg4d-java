package org.peg4d.regex;

import org.peg4d.ParsingObject;

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
			rules.put("TopLevel", generate(regex, token));
			return rules;
		}else{
			System.err.println("The input file isn't a regex file.");
			return null;
		}
	}

	private RegexObject generate(RegSeq regex, ParsingObject po){
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

		switch(po.get(1).getTag().toString()){
		case "Block":
			ro = new RegNonTerminal(createBlockId());
			ro.setChild(generate(new RegSeq(), po.get(1)));
			ro.getChild().setParent(ro);
			rules.put(ro.toString(), ro.getChild());
			break;
		case "Group":
			ro = new RegNonTerminal(createGroupId());
			ro.setChild(generate(new RegSeq(), po.get(1)));
			ro.getChild().setParent(ro);
			rules.put(ro.toString(), ro.getChild());
			break;
		case "Comment":
			ro = new RegNull();
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
}
