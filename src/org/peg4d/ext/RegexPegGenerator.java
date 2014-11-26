package org.peg4d.ext;


import org.peg4d.ParsingObject;

public class RegexPegGenerator extends Generator {

	private String LF = "\n";
	private int TerminalCount = 0;
	private String tmp = null;

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

	private void writeTerminalExpression(String indent, int index){
		this.write(LF+"E"+index+LF+indent+"=");
	}

	private void writeTerminalExpression(int index){
		this.writeTerminalExpression("",index);
	}

	private void parseRegex(ParsingObject pego){
		String tag = pego.getTag().toString();
//		System.out.println(tag+" hash:"+pego.hashCode());
		int i = 0;
//		System.out.println(tag);
		if(tag == null){
			System.out.println("parsing error in Regex AST.");
		}else switch(tag){
		case "comment":
		case "BiginWith":
		case "EndWith":
			break;
		case "onemoreL":
//			if(next == null){
//				this.write("+");
//			}else{
				this.write(" E"+this.TerminalCount+LF);
				this.writeTerminalExpression(this.TerminalCount);
				this.write(" '"+this.tmp+"' E"+this.TerminalCount+" /");
				this.TerminalCount++;
//			}
			break;
		case "Char":
			this.tmp = pego.getText();
			this.write(" '"+this.tmp+"'");
			break;
		default:
			while(i<pego.size()){
//				if(i+1<pego.size()){
//					this.parseRegex(pego.get(i),pego.get(i+1));
//				}else{
					this.parseRegex(pego.get(i));
//				}
				i++;
			}
			i = 0;
			break;
		}
	}

}
