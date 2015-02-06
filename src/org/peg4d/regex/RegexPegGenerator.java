package org.peg4d.regex;

import java.util.Map;
import java.util.Map.Entry;

import org.peg4d.writer.Generator;

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
		writeLn("// regex PEG\n");
		writeHeader();
		RegexObject r = rules.get("TopLevel");
		r.setWriteMode(true);
		writeLn("TopLevel");
		write("    = { ");
		this.write(r.toString());
		writeLn(" #Matched }");
		writeLn("");

		for(Entry<String, RegexObject> s: rules.entrySet()) {
			if(s.getKey().equals("TopLevel")) {
				continue;
			}
			writeLn(s.getKey());
			write("    = ");
			s.getValue().setWriteMode(true);
			writeLn(s.getValue().toString());
		}
	}

	private void writeHeader() {
		writeLn("File");
		writeLn("    = { @TopLevel #Source } _");
		writeLn("");
		writeLn("Chunk");
		writeLn("    = TopLevel");
		writeLn("");
		writeLn("_");
		writeLn("    = [ \\t\\r\\n]*");
		writeLn("\n");
	}
}
