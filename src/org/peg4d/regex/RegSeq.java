package org.peg4d.regex;

import org.peg4d.ParsingObject;


public class RegSeq extends RegexObject {

	public RegSeq() {
		super(null);
	}

	public RegSeq(ParsingObject po) {
		super(po);
	}

	public boolean contains(RegexObject obj){
		StringBuilder sbThis = new StringBuilder();
		StringBuilder sbObj = new StringBuilder();
		int count = 0;

		for(RegexObject e: this.list){
			sbThis.append(e.getLetter());
			count++;
		}

		if(obj instanceof RegCharSet){
			sbObj.append(obj.getLetter());
		}
//		else for(RegexObject e: obj.getList()){
		else for(int i = 0; i < count; i++){
			if(i < obj.size()) sbObj.append(obj.get(i).getLetter());
		}

		return sbThis.toString().equals(sbObj.toString());
	}

	@Override
	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
			sb.append(e.getLetter());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(quantifier != null) {
			sb.append("( ");
		}
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		if(quantifier != null) {
			sb.append(")");
			sb.append(quantifier.toString());
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()) return this.quantifier.repeatRule(sb.toString());
		else return sb.toString();
	}
}
