package org.peg4d.regex;

import java.util.LinkedHashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	private Set<String> set;

	public RegCharSet(ParsingObject po) {
		super(po);
		set = new LinkedHashSet<String>();
		setCharSet(po.get(1).getText());
		this.addQuantifier(po);
	}

	private void setCharSet(String s) {
		if(s.startsWith("\\u")){
//			s = unicodeToStr(s);
			set.add(s);
			return;
		} else if(s.startsWith("\\")) {
			s = s.substring(1);
		}

		if(s.length() == 1) {
			set.add(s);
			return;
		}else if(s.length() == 2 && s.charAt(0)=='\\'){
			//escapedchar
			set.add(s);
			return;
		}
		int i = 1;
		int max = s.length() - 1;
		do {
			//bracket expression
			int next = i + 1;
			int next2 = i + 2;
			if(i == 1 && s.charAt(i)=='^'){
				//exceptfor
				this.not = true;
				i++;
				continue;
			}
			if(next < s.length() && s.charAt(next)=='-' && next2 < s.length()) {
				//range
				for(char j = s.charAt(i); j <= s.charAt(next2); j++) {
					set.add(String.valueOf(j));
				}
				i += 3;
			} else if("[:".equals(s.substring(i, next2))){
				//class
				int count = i + 3;
				while(s.charAt(count) != ':') count++;
				addClassChar(s.substring(i+2, count));
				i += count;
				++i;
			} else if("\\u".equals(s.substring(i, next2))){
				//unicode
				set.add(s.substring(i, i+6));
				i += 6;
			} else if(s.charAt(i)=='\\'){
				//escapedchar
				set.add(s.substring(i, next2));
				i += 2;
			} else {
				set.add(s.substring(i, next));
				++i;
			}
		} while(i < max);

		// remove '\', add '\\'.
		char c = 0x5c;
		if(set.remove(String.valueOf(c))){
			set.add("\\\\");
		};
	}

//	private String unicodeToStr(String unicode){
//		int codePoint[] = new int[1];
//		codePoint[0] = Integer.parseInt(unicode.substring(2), 16);
//		String string = new String(codePoint, 0, 1);
//		return string;
//	}

	private void addClassChar(String className){
		char c;
		switch(className){
		case "ascii":
		case "ASCII":
			for(c = 0x00; c <= 0x7F; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "alnum":
		case "Alnum":
			for(c = '0'; c <= '9'; c++) {
				set.add(String.valueOf(c));
			}
		case "alpha":
		case "Alpha":
			for(c = 'a'; c <= 'z'; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "blank":
		case "Blank":
//			char tab = 0x09;
//			set.add(String.valueOf(tab));
			set.add("\\t");
			set.add(" ");
			break;
		case "cntrl":
		case "Cntrl":
			for(c = 0x00; c <= 0x1F; c++) {
				set.add(String.valueOf(c));
			}
			c = 0x7E;
			set.add(String.valueOf(c));
			break;
		case "digit":
		case "Digit":
			for(c = '0'; c <= '9'; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "glaph":
		case "Graph":
			for(c = 0x21; c <= 0x7E; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "lower":
		case "Lower":
			for(c = 'a'; c <= 'z'; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "print":
		case "Print":
			for(c = 0x20; c <= 0x7E; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "punct":
		case "Punct":
			for(c = 0x21; c <= 0x47; c++) {
				set.add(String.valueOf(c));
			}
			for(c = 0x3A; c <= 0x40; c++) {
				set.add(String.valueOf(c));
			}
			for(c = 0x5B; c <= 0x60; c++) {
				set.add(String.valueOf(c));
			}
			for(c = 0x7B; c <= 0x7E; c++) {
				set.add(String.valueOf(c));
			}
			break;
		case "xdigit":
		case "XDigit":
			for(c = '0'; c <= '9'; c++) {
				set.add(String.valueOf(c));
			}
			for(c = 'a'; c <= 'f'; c++) {
				set.add(String.valueOf(c));
			}
			for(c = 'A'; c <= 'F'; c++) {
				set.add(String.valueOf(c));
			}
			break;
		default:
			System.out.println("Unsupported class name(s) is(are) inputted.");
		}
	}

	@Override
	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
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
		if(this.not == true){
			if(this.quantifier != null) sb.append("( ");
			sb.append("!");
		}
		if(arr.length == 1){
			sb.append("'");
			sb.append(arr[0]);
			sb.append("'");
		}else{
			sb.append("( ");
			sb.append("'");
			sb.append(arr[0]);
			sb.append("' ");
			for(int i = 1; i < arr.length; i++) {
				sb.append("/ '");
				sb.append(arr[i]);
				sb.append("' ");
			}
			sb.append(")");
		}
		if(this.not == true){
			sb.append(" .");
			if(this.quantifier != null) sb.append(")");
		}
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

	public boolean contains(RegexObject obj) {
		if (obj instanceof RegCharSet) {
			RegCharSet that = (RegCharSet) obj;
			boolean b = false;
			for(String e: that.set.toArray(new String[that.set.size()])) {
				b = b || this.set.contains(e);
			}
			return b;
		}
		return false;
	}
}
