package org.peg4d.regex;

import java.util.Map;
import java.util.Map.Entry;

import org.peg4d.writer.Generator;

public class RegexPegGenerator extends Generator {

	private Map<String, RegexObject> rules;
	private final static String LINE = "Line";
	private final static String MATCH = "Match";
	private final static String UNMATCH = "Unmatch";

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
		String rule = r.toString();
		writeLn(MATCH);
		write("    = { ");
		write(rule);
		writeLn(" #Matched }\n");
		writeLn(UNMATCH);
		write("    = { ( !(");
		write(rule);
		writeLn(") !NL . )+ #Unmatched }\n");

		for(Entry<String, RegexObject> s: rules.entrySet()) {
			if(s.getKey().equals("TopLevel")) {
				continue;
			}
			writeLn(s.getKey());
			write("    = ");
			s.getValue().setWriteMode(true);
			writeLn(s.getValue().toString() + "\n");
		}
	}

	private void writeHeader() {
		writeLn("File");
		writeLn("    = _ TopLevel _");
		writeLn("");
		writeLn("Chunk");
		writeLn("    = TopLevel");
		writeLn("");
		writeLn("_");
		writeLn("    = [ \\t]*");
		writeLn("");
		writeLn("NL");
		writeLn("    = [\\r\\n]+");
		writeLn("");
		writeLn("TopLevel");
		write("    = { @");
		write(LINE);
		write(" ( _ NL? @");
		write(LINE);
		writeLn(" )* #Source }");
		writeLn("");
		writeLn(LINE);
		write("    = { ( @");
		write(MATCH);
		write(" / _ @");
		write(UNMATCH);
		writeLn(" )+ #Line }\n");
	}

}
