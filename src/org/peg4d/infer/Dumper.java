package org.peg4d.infer;

public interface Dumper {
	void dump(StringWriter writer);
}

@FunctionalInterface
interface StringWriter {
	public void write(String text);
}