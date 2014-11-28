package org.peg4d.regex;

import java.util.Map;
import java.util.Map.Entry;

import org.peg4d.ext.Generator;

public class RegexPegGenerator extends Generator {

	private Map<String, RegexObject> rules;

	public RegexPegGenerator(String fileName, Map<String, RegexObject> rules) {
		super(fileName);
		this.rules = rules;//new HashMap<String, List<RegexObject>>();
	}

	private void writeLn(String s) {
		this.write(s + "\n");
	}

	public void writePeg() {
		writeHeader();
		RegexObject r = rules.get("TopLevel");
		write("TopLevel = { ");
		this.write(r.toString());
		writeLn(" #Matched }");

		for(Entry<String, RegexObject> s: rules.entrySet()) {
			if(s.getKey().equals("TopLevel")) {
				continue;
			}
			writeLn(s.getKey());
			write("    ");
			write("= ");
			writeLn(s.getValue().toString());
		}
	}

	private void writeHeader() {
		writeLn("File  = { @TopLevel #Source } _");
		writeLn("");
		writeLn("Chunk = TopLevel");
		writeLn("");
	}

}
