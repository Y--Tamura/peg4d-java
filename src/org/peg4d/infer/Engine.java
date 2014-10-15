package org.peg4d.infer;

import java.util.ArrayList;
import java.util.HashMap;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingRule;
import org.peg4d.ParsingSource;
import org.peg4d.UList;
import org.peg4d.query.Pair;

public class Engine {
	private final Grammar grammar;

	public Engine() {
		this(new GrammarFactory().newGrammar("main"));
	}

	public Engine(Grammar grammar) {
		this.grammar = grammar;
	}

	public void addExternalGrammar(String pathToGrammar) {
		grammar.importGrammar(pathToGrammar);
		grammar.verifyRules();
	}
	public void addExternalGrammar(ArrayList<String> pathsToGrammar) {
		for (String pathToGrammar: pathsToGrammar) {
			this.addExternalGrammar(pathToGrammar);
		}
	}

/*
	public void infer(ParsingSource[] sources) {
		for (int i = 0; i < sources.length; i++) {
			this.infer(sources[i]);
		}
	}
*/
	public void infer(String pathToTarget) {
		ParsingSource source = org.peg4d.Main.loadSource(this.grammar, pathToTarget);
		ParsingContext context = new ParsingContext(source);
		LatticeNeo4j lattice = new LatticeNeo4j(source);
		UList<ParsingRule> ruleList = this.grammar.getRuleList();
		for (ParsingRule rule : ruleList) {
			context.resetSource(source, 0);
			String ruleName = rule.getRuleName();
			this.collectRule(context, source, ruleName, lattice);
		}
		if (Options.verbose) {
			lattice.dump();
			lattice.dumpToGraphviz();
		}
		ArrayList<ArrayList<String>> formats = lattice.generateShortestPath();
		for (ArrayList<String> format : formats) {
			this.structure(format, 2);
		}
	}

	private void collectRule(ParsingContext context, ParsingSource source, String ruleName, LatticeNeo4j lattice) {
		long startPos = context.getPosition();
		while (source.length() > startPos) {
			context.parse(this.grammar, ruleName);
			if (context.isFailure()) {
				startPos++;
				context.setPosition(startPos);
			}
			else {
				long endPos = context.getPosition();
				//context.setPosition(endPos);
				lattice.appendMatchedRule(ruleName, startPos, endPos);
				startPos = endPos;
			}
		}
	}

	private void structure(ArrayList<String> format, int ngram) {
		HashMap<Pair<String, String>, Long> m = new HashMap<>();
		String elem1 = null, elem2 = null;
		Pair<String, String> key = null;
		for (int i = 0; i < format.size() - (ngram - 1); i++) {
			elem1 = format.get(i);
			elem2 = format.get(i + 1);
			key = new Pair<>(elem1, elem2);
			if (m.containsKey(key)) {
				m.put(key, m.get(key) + 1);
			}
			else {
				m.put(key, 1L);
			}
		}
	}
}

