package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected boolean writePegMode;
	protected List<RegexObject> list;
	protected ParsingObject ref;
	protected Quantifier quantifier;
	protected RegexObject parent;
	protected boolean refer;
	public boolean beginWith;
	public boolean endWith;
	public boolean not;

	public RegexObject(ParsingObject po) {
		this.writePegMode = false;
		this.refer = false;
		this.beginWith = false;
		this.endWith = false;
		this.not = false;
		this.ref = po;
		this.quantifier = null;
		this.list = new ArrayList<RegexObject>();
	}

	public void addQuantifier(ParsingObject po) {
			if(is(po.get(0), "BeginWith")) {
				beginWith = true;
			}
			if(po.size() > 2) {
				quantifier = new Quantifier(po.get(2));
			}
			if(po.size() > 3 && is(po.get(3), "EndWith")) {
				endWith = true;
			}
	}

	public void setWriteMode(boolean b){
		this.writePegMode = b;
		for(RegexObject r: list){
			r.setWriteMode(b);
		}
	}

	public void add(RegexObject e) {
		this.list.add(e);
	}

	public RegexObject get(int i) {
		return this.list.get(i);
	}

	public List<RegexObject> getList(){
		return this.list;
	}

	public int size() {
		return this.list.size();
	}

	private RegexObject pop() {
		return this.list.remove(this.list.size() - 1);
	}

	public RegexObject popHead() {
		return this.list.remove(0);
	}

	public void pushHead(RegexObject that) {
		this.list.add(0, that);
	}

	public ParsingObject getRef(){
		return this.ref;
	}

	public Quantifier getQuantifier(){
		return this.quantifier;
	}

	public void rmQuantifier(){
		this.quantifier = null;
	}

	public String getTag(){
		return this.quantifier.getLabel();
	}

	public RegexObject getParent(){
		return this.parent;
	}

	public void setParent(RegexObject r){
		this.parent = r;
	}

	public boolean getRefer(){
		return this.refer;
	}

	public void setRefer(boolean b){
		this.refer = b;
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

	public RegexObject popContinuation() {
		RegSeq rs = new RegSeq();
		if(this.size() < 1) {
			return rs;
		}
		rs.add(this.pop());
		return rs;
	}

	public void concat(RegexObject ro) {
		if(ro instanceof RegSeq) {
			for(int i = 0; i < ro.size(); i++) {
				this.add(ro.get(i));
			}
		} else {
			this.add(ro);
		}
	}

	abstract String getLetter();
}