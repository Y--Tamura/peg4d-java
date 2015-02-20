package org.peg4d.regex;

import java.util.LinkedHashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	protected Set<String> set;

	public RegCharSet(ParsingObject po) {
		super(po);
		set = new LinkedHashSet<String>();
		String s = po.get(1).getText();
//		if(s != null && s.startsWith("\\")) setCharSet("\\" + s);
//		else setCharSet(s);
		setCharSet(s);
	}

	public RegCharSet(String s) {
		super();
		set = new LinkedHashSet<String>();
//		if(s != null && s.startsWith("\\")) setCharSet("\\" + s);
//		else setCharSet(s);
		setCharSet(s);
	}

	private void setCharSet(String s) {
		int i = 0;
		int max = s.length() - 1;
		char[] c = s.toCharArray();
		char token;
		while(i <= max){
			token = c[i];
			if(token == '\\'){
	//			s = unicodeToStr(s);
				char fix;
				if(c.length > i+1) fix = c[i+1];
				else{
					set.add("\\" + String.valueOf(token));
					i++;
					continue;
				}
				switch(fix){
				case 'u':
					char[] unicode = {'\\', 'u', c[i+2], c[i+3], c[i+4], c[i+5]};
					String u = "\\" + String.valueOf(unicode);
					set.add(u);
					i += 6;
					break;
				case 'x':
				case 'o':
					//FIXME
					i++;
					break;
				default:
					char[] escape = {'\\', c[i+1]};
					if(escape[1] == '\\'){
						set.add("\\\\");
						i += 3;
					}else if(('A' <= escape[1] && escape[1] <= 'Z') || ('a' <= escape[1] && escape[1] <= 'z') ){
						String e = "\\" + String.valueOf(escape[1]);
						set.add(e);
						i += 2;
					}else{
						String e = String.valueOf(escape[1]);
						set.add(e);
						i += 2;
					}
					break;
				}
			}else{
				if(token == '\''){
					set.add("\\'");
				}else {
					set.add(String.valueOf(token));
				}
				i++;
			}
		}

		// remove '\', add '\\'.
		char bs = 0x5c;
		if(set.remove(String.valueOf(bs))){
			set.add("\\\\");
		};
		/* 
		for(String str: set){
			if(str.startsWith("\\")){
				str = "\\" + str;
			}
		} */
	}

//	private String unicodeToStr(String unicode){
//		int codePoint[] = new int[1];
//		codePoint[0] = Integer.parseInt(unicode.substring(2), 16);
//		String string = new String(codePoint, 0, 1);
//		return string;
//	}

	@Override
	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(Object e: this.set.toArray()) {
			sb.append(e.toString());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String[] arr = set.toArray(new String[set.size()]);
		if(this.quantifier != null && this.quantifier.hasRepeat() && arr.length > 1){
			sb.append("(");
		}

		sb.append("'");
		for(int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);
		}
		sb.append("'");

		if(this.quantifier != null) {
			sb.append(this.quantifier.toString());
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()){
			if(arr.length > 1) sb.append(")");
			return this.quantifier.repeatRule(sb.toString());
		}else{
			return sb.toString();
		}
	}
}
