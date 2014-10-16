package org.peg4d.infer;

public interface Lattice {
	public void appendMatchedRule(String ruleName, long startPos, long endPos);
//	public ArrayList<ArrayList<String>> generateShortestPath();
}