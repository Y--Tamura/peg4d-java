package org.peg4d.infer;

import java.util.ArrayList;

import org.peg4d.query.ArgumentsParser;

public class Main {
	public static void main(String args[]) {
		Options options = Options.createFromCommandLineArguments(args);
		System.out.println(options);

		Engine engine = new Engine();
		engine.addExternalGrammar(options.grammars);
		engine.infer(options.target);

		return;
	}

	/*
	public final static void performShell3(InferenceEngine engine) {
		displayShellVersion(engine.grammar);
		int linenum = 1;
		String line = null;
		while ((line = readMultiLine("?>>> ", "    ")) != null) {
			ParsingSource source = new StringSource(engine.grammar, "(stdin)", linenum, line);
			engine.infer(source);
			linenum = linenum + 1;
		}
		System.out.println("");
	}
	private void parseArgs(String args[]) {
	}
	 */		
}

class Options {
	public ArrayList<String> grammars = new ArrayList<>();
	public String target = null;

	static public Options createFromCommandLineArguments(String args[]) {
		Options newOptions = new Options();
		ArgumentsParser argsParser = new ArgumentsParser();
		argsParser.addDefaultAction(s -> argsParser.printHelpBeforeExit(System.err, 1))
		.addHelp("h", "help", false, "show this help message", 
				s -> argsParser.printHelpBeforeExit(System.out, 0))
		.addOption("g", "grammar", true, "peg definition of target data format", true, true,
				s -> newOptions.grammars.add(s.get()))
		.addOption("t", "target", true, "target data file", true, false, 
				s -> newOptions.target = s.get());
		try {
			argsParser.parseAndInvokeAction(args);
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
			argsParser.printHelpBeforeExit(System.err, 1);
		}		
		return newOptions;
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += "grammars:" + this.grammars.toString() + ", ";
		ret += "target:" + this.target;
		return ret;
	}
}