package org.peg4d.regex;

import org.peg4d.ParsingObject;


public class RegSeq extends RegexObject {

	public RegSeq() {
		super(null);
	}

	public RegSeq(ParsingObject po) {
		super(po);
		this.addQuantifier(po);
	}

	public boolean contains(RegexObject obj){
		if(this.size() == obj.size()){
			boolean result = true;
			int i;
			for(i = 0; i < this.size(); i++){
				if(!this.get(i).toString().equals(obj.get(i).toString())){
					result = false;
					break;
				}
			}
			return result;
		}
		else if(this.size() == 1 && this.get(0) instanceof RegSeq){
			return ((RegSeq)this.get(0)).contains(obj);
		}
		else if(obj.size() == 1 && obj.get(0) instanceof RegSeq){
			return this.contains(obj.get(0));
		}else{
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(quantifier != null) {
			sb.append("(");
		}
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		if(quantifier != null) {
			sb.append(")");
			sb.append(quantifier.toString());
		}
		return sb.toString();
	}
}
