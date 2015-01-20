package org.peg4d.regex;


public class RegNonTerminal extends RegexObject {

	private String defName = null;
	private final static String defSuffix = "_D";
	private boolean isa = false;
	private boolean refer = false;
	private String label;

	public RegNonTerminal(String label) {
		super(null);
		this.label = label;
	}

	public void setDefName() {
		this.defName = this.label + defSuffix;
	}

	public String getDefName() {
		return this.defName;
	}

	public void setIsa() {
		this.setDefName();
		this.isa = true;
	}

	public boolean getIsa() {
		return this.isa;
	}

	public void setRefer(boolean b){
		this.refer = b;
	}

	public boolean getRefer(){
		return this.refer;
	}

	@Override
	public String getLetter() {
		return "";
	}

	@Override
	public String toString() {
		if(writePegMode){
			if(isa){
				return "<isa " + defName + ">";
			}
			else if(refer){
				return "<def " + defName + " " + label + ">";
			}else{
				if(this.quantifier == null) return label;
				else return label + this.quantifier.toString();
			}
		}else{
			if(this.quantifier == null) return label;
			else return label + this.quantifier.toString();
		}
	}

}
