package org.peg4d.ext;


import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class RegexPegGenerator extends Generator {

	private String LF = "\n";
	private int TerminalCount = 0;

	public RegexPegGenerator(String fileName) {
		super(fileName);
	}

	public void writeRegexPego(ParsingObject pego){
		this.writeRegexPego(pego, "");
		this.write(LF);
		this.close();
	}

	public void writeRegexPego(ParsingObject pego,String indent){
		this.write("File"+LF+indent+"= TopLevel"+LF+" "+LF);
		this.write("Chunk"+LF+indent+"= TopLevel"+LF+" "+LF);
		this.write("TopLevel"+LF+indent+"=");

		this.parseRegex(pego);

		this.close();
//		System.out.println("'"+(char)Integer.parseInt("97",10)+"'");
	}

//	private void writeTerminalExpression(String expression, String indent, int index){
//		this.write("e"+index+LF+indent+"= "+expression);
//	}

	private void parseRegex(ParsingObject pego){
		String tag = pego.getTag().toString();
		int i = 0;
//		System.out.println(tag);
		if(tag == null){
			System.out.println("parsing error in Regex AST.");
		}else switch(tag){
		case "comment":
		case "BiginWith":
		case "EndWith":
			break;
		case "Char":
			this.write(" '"+pego.getText()+"'");
			break;
		default:
			while(i<pego.size()){
				this.parseRegex(pego.get(i));
				i++;
			}
			i = 0;
			break;
		}
	}

	private String makeParsingExpression(ParsingObject pego){
		int i = 0;
		ParsingSource source = pego.getSource();
		String expression = null; //pego.getSource().utf8[i];
		return expression;
	}

}
