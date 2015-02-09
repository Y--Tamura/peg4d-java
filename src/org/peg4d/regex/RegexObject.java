package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected boolean writePegMode;
	protected List<RegexObject> list = null;
	protected ParsingObject ref;
	protected Quantifier quantifier;
	protected RegexObject parent;
	protected RegexObject child;
	public boolean look;
	public boolean beginWith;
	public boolean endWith;
	public boolean not;

	public RegexObject(ParsingObject po) {
		this.writePegMode = false;
		this.look = false;
		this.beginWith = false;
		this.endWith = false;
		this.not = false;
		this.ref = po;
		this.quantifier = null;
		this.parent = null;
		this.child = null;
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
		if(!(e instanceof RegNull)) this.list.add(e);
	}

	public RegexObject get(int i) {
		if(this.list == null) return new RegNull();
		else if(i >= this.size()) return new RegNull();
		return this.list.get(i);
	}

	public void setList(int i, RegexObject e){
		this.list.set(i, e);
	}

	public List<RegexObject> getList(){
		return this.list;
	}

	public int size() {
		return this.list.size();
	}

	public RegexObject pop() {
		if(this.size() > 0) return this.list.remove(this.list.size() - 1);
		else return null;
	}

	public void push(RegexObject that) {
		if(!(that instanceof RegNull)) this.list.add(that);
	}

	public void pushRegNull() {
		this.list.add(new RegNull());
	}

	public RegexObject popHead() {
		if(this.size() > 0) return this.list.remove(0);
		else return null;
	}

	public void pushHead(RegexObject that) {
		if(!(that instanceof RegNull)) this.list.add(0, that);
	}

	public void remove(int i){
		this.list.remove(i);
	}

	public ParsingObject getRef(){
		return this.ref;
	}

	public Quantifier getQuantifier(){
		return this.quantifier;
	}

	public void setQuantifier(Quantifier q){
		this.quantifier = q;
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

	public RegexObject getChild(){
		return this.child;
	}

	public void setChild(RegexObject r){
		this.child = r;
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

	public RegexObject popContinuation() {
		if(this.size() < 1) {
			return new RegNull();
		}
		RegSeq rs = new RegSeq();
		rs.add(this.pop());
		return rs;
	}

	public RegSeq getContinuation() {
		RegSeq rs = new RegSeq();
		for(RegexObject e: this.list){
			rs.push(e);
		}
		if(rs.size() > 0) rs.popHead();
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

	public boolean contains(RegexObject obj){
		if(obj == null || obj instanceof RegNull || this instanceof RegNull) return false;

		String thisLetter = this.getLetter();
		String objLetter = obj.getLetter();

		if(thisLetter == null || objLetter == null) return false;

		if("".equals(thisLetter) || "".equals(objLetter)) return false;

		return objLetter.equals(thisLetter) || objLetter.startsWith(thisLetter) || thisLetter.endsWith(objLetter);
	}

	abstract String getLetter();
}
