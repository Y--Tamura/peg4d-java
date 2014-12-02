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
		switch(e.getTag().toString()){
		case "Or":
			RegChoice roOr = new RegChoice(null);
			System.out.println("size :"+e.size());
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
//		if(target == null){
//			return continuation;
//		}else if(target.size()==1){
//			target.add(continuation);
//			return target;
//		}else if(target.size()>1){
//			return null;
//		}else{
//			System.err.println("method 'pi' error!!");
			return null;
//		}
	}
}
