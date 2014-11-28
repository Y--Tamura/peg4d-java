package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class Quantifier {

	private String label;
	private String sign;
	private int time = -1;
	private int min = -1;
	private int max = -1;

	public Quantifier(ParsingObject po) {
		this.label = po.getTag().toString();
		if(label.equals("Times")) {
			ParsingObject p = po.get(0);
			if(p.getTag().toString().equals("AndMore")) {
				this.setMin(Integer.parseInt(p.getText()));
			} else {
				this.setTime(Integer.parseInt(p.getText()));
			}
			if(po.size() > 1) {
				this.setMax(Integer.parseInt(po.get(1).getText()));
			}
		} else {
			sign = po.getText();
		}
	}

	public String getLabel() {
		return label;
	}

	public int getTime() {
		return time;
	}

	private void setTime(int time) {
		this.time = time;
	}

	public int getMin() {
		return min;
	}

	private void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	private void setMax(int max) {
		this.max = max;
	}

	private String getSign() {
		return sign;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String s = getSign();
		if(s != null) {
			sb.append(s);
		} else {
			sb.append("{");
			sb.append(min);
			sb.append(",");
			sb.append(max);
			sb.append("}");
		}
		return sb.toString();
	}
}
