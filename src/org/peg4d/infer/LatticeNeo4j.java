package org.peg4d.infer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.peg4d.ParsingSource;

public class LatticeNeo4j {
	private GraphDatabaseService graphDb;
	private RelationshipFactory relFactory;
	private Node bosNode;
	private Node eosNode;
	private TreeMap<Long, Node> nodeMap;

	LatticeNeo4j(ParsingSource source) {
		this(source, false);
	}
	LatticeNeo4j(ParsingSource source, boolean initializeAllSource) {
		this.nodeMap = new TreeMap<>();
		this.relFactory = new RelationshipFactory(source);
		this.graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(Const.dbPath);
		try (Transaction tx = this.graphDb.beginTx()) {
			this.bosNode = this.getOrCreateNode(0L);
			tx.success();
		}
		if (initializeAllSource) {
			try (Transaction tx = this.graphDb.beginTx()) {
				for (long i = 0; i < source.length(); i++) {
					this.getOrCreateNode(i + 1);
				}
				tx.success();
			}
		}
	}

	public void appendMatchedRule(String ruleName, long startPos, long endPos) {
		if (Options.verbose) {
			System.out.print(ruleName);
			System.out.print("[");
			System.out.print(startPos);
			System.out.print(":");
			System.out.print(endPos);
			System.out.print("]");
			System.out.print("\n");
		}
		try (Transaction tx = this.graphDb.beginTx()) {
			Node startNode = this.getOrCreateNode(startPos);
			Node endNode = this.getOrCreateNode(endPos);
			this.relFactory.createRuleRel(startNode, endNode, ruleName);
			tx.success();
		}
	}

	private Node getOrCreateNode(Long pos) {
		Node ret = null;
		if (pos == null) throw new RuntimeException("pos is null");
		if ((ret = this.nodeMap.get(pos)) == null) {
			ret = this.graphDb.createNode();
			//ret.setProperty("creation time", ZonedDateTime.now().toString());
			ret.setProperty("pos", pos);
			Map.Entry<Long, Node> tmp = null;
			if ((tmp = this.nodeMap.lowerEntry(pos)) != null) {
				this.relFactory.createNaturalRel(tmp.getValue(), ret);
			}
			if ((tmp = this.nodeMap.higherEntry(pos)) != null) {
				this.relFactory.createNaturalRel(ret, tmp.getValue());
			}
			else {
				this.eosNode = ret;
			}
			this.nodeMap.put(pos, ret);
		}
		return ret;
	}

	public ArrayList<ArrayList<String>> generateShortestPath() {
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		try (Transaction tx = this.graphDb.beginTx()) {
			PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(
					PathExpanders.forTypesAndDirections(
							RelType.NATURAL, Direction.OUTGOING,
							RelType.RULES, Direction.OUTGOING
							), Const.SIZE);
			Iterable<WeightedPath> paths = finder.findAllPaths(this.bosNode, this.eosNode);
			ArrayList<String> format = null;
			String prev = null;
			for (WeightedPath path : paths) {
				format = new ArrayList<>();
				prev = "";
				for (Relationship rel : path.relationships()) {
					if (rel.hasProperty(Const.RULE)) {
						if (!prev.isEmpty()) {
							format.add("\"" + prev + "\"");
							prev = "";
						}
						format.add(Arrays.toString((String[])rel.getProperty(Const.RULE)));
					}
					else if (rel.hasProperty(Const.SYMBOL)) {
						prev += rel.getProperty(Const.SYMBOL).toString().replace("\n", "\\n");
					}
					else {
						throw new RuntimeException("FIX ME!! unknown relation");
					}
				}
				if (!prev.isEmpty()) format.add("\"" + prev + "\""); 
				ret.add(format);
				if (Options.verbose) System.out.println(format);
			}
			tx.success();
		}
		return ret;
	}

	private Traverser traverse(final Node startNode, boolean includeStartNode, EnumSet<RelType> types) {
	    TraversalDescription td = graphDb.traversalDescription()
	            .breadthFirst()
	    		.relationships(RelType.NATURAL, Direction.OUTGOING)
	            .relationships(RelType.RULE, Direction.OUTGOING)
	    		.relationships(RelType.RULES, Direction.OUTGOING);
	    if (!includeStartNode) td = td.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelType.NATURAL, RelType.RULE, RelType.RULES));
	    return td.traverse(startNode);
	}
	
	
	public void dump() {
		Traverser t = null;
		try (
			Transaction tx = this.graphDb.beginTx();
		) {
			t = this.traverse(this.bosNode, true, EnumSet.of(RelType.NATURAL, RelType.RULE, RelType.RULES));
			Node node0 = null, node1 = null;
			String label0 = null, label1 = null;
			for (Path path : t) {
				node0 = path.endNode();
				label0 = node0.getProperty(Const.POS, "error_pos").toString();
				for (Relationship rel : node0.getRelationships(Direction.OUTGOING)) {
					node1 = rel.getEndNode();
					label1 = node1.getProperty(Const.POS, "error_pos").toString();
					System.out.print("\"" + label0 + "\"");
					System.out.print(" -> ");
					System.out.print("\"" + label1 + "\"");
					System.out.print(" [label = ");
					System.out.print("\"" + rel.getType().name() + ":");
					if (rel.isType(RelType.NATURAL)) {
						System.out.print(unEscapeString(rel.getProperty(Const.SYMBOL).toString()) + "\""); 
					}
					else if (rel.isType(RelType.RULE) || rel.isType(RelType.RULES)) {
						System.out.print(rel.getProperty(Const.RULE) + "\"");
					}
					else {
						System.out.print("error : unknown rel type");
					}
					System.out.print("]; //");
					System.out.print(Const.SYMBOL + ":" + unEscapeString(rel.getProperty(Const.SYMBOL).toString()) + ", ");
					System.out.print(Const.SIZE + ":" + rel.getProperty(Const.SIZE));
					System.out.print("\n");
				}
			}
		}
	}
	
	public void dumpToGraphviz() {
		this.dumpToGraphviz(Const.dotPath, this.bosNode);
	}
	public void dumpToGraphviz(String fileName) {
		this.dumpToGraphviz(fileName, this.bosNode);
	}
	public void dumpToGraphviz(String fileName, Node startNode) {
		Traverser t = null;
		try (
			Transaction tx = this.graphDb.beginTx();
			FileWriter writer = new FileWriter(fileName);
		) {
			writer.write("digraph {\n");
			t = this.traverse(this.bosNode, true, EnumSet.of(RelType.NATURAL, RelType.RULE, RelType.RULES));
			Node node0 = null, node1 = null;
			String label0 = null, label1 = null;
			for (Path path : t) {
				node0 = path.endNode();
				label0 = node0.getProperty(Const.POS, "error_pos").toString();
				for (Relationship rel : node0.getRelationships(Direction.OUTGOING)) {
					node1 = rel.getEndNode();
					label1 = node1.getProperty(Const.POS, "error_pos").toString();
					writer.write("\"" + label0 + "\"");
					writer.write(" -> ");
					writer.write("\"" + label1 + "\"");
					writer.write(" [label = ");
					writer.write("\"" + rel.getType().name() + ":");
					if (rel.isType(RelType.NATURAL)) {
						writer.write(unEscapeString(rel.getProperty(Const.SYMBOL).toString()) + "\""); 
					}
					else if (rel.isType(RelType.RULE)) {
						writer.write(rel.getProperty(Const.RULE) + "\"");
					}
					else if (rel.isType(RelType.RULES)) {
						writer.write(Arrays.toString((String[])rel.getProperty(Const.RULE)) + "\"");
					}
					else {
						System.out.println("error : unknown rel type");
					}
					writer.write("]; //");
					writer.write(Const.SYMBOL + ":" + unEscapeString(rel.getProperty(Const.SYMBOL).toString()) + ", ");
					writer.write(Const.SIZE + ":" + rel.getProperty(Const.SIZE));
					writer.write("\n");
				}
			}
			writer.write("}");
		} catch (IOException e) {
			System.out.println("error : dump log.dot failed");
		}
	}
	
	public void compactRules() {
		Traverser t = null;
		try (
			Transaction tx = this.graphDb.beginTx();
		) {
			t = this.traverse(this.bosNode, true, EnumSet.of(RelType.NATURAL, RelType.RULE, RelType.RULES));
			Node startNode = null, endNode = null;
			ArrayList<String> ruleList = null;
			HashMap<Node, ArrayList<String>> relMap = null;
			for (Path path : t) {
				startNode = path.endNode();
				relMap = new HashMap<>();
				for (Relationship rel : startNode.getRelationships(Direction.OUTGOING, RelType.RULE)) {
					endNode = rel.getEndNode();
					if ((ruleList = relMap.get(endNode)) != null) {
						ruleList.add(rel.getProperty(Const.RULE).toString());
					}
					else {
						ruleList = new ArrayList<>();
						ruleList.add(rel.getProperty(Const.RULE).toString());
						relMap.put(endNode, ruleList);
					}
				}
				for (Entry<Node, ArrayList<String>> kv : relMap.entrySet()) {
					relFactory.createRulesRel(startNode, kv.getKey(), kv.getValue().toArray(new String[0]));
				}
			}
			tx.success();
		}
	}

	private static String unEscapeString(String s) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	    	switch (s.charAt(i)) {
			case '\n': sb.append("\\n"); break;
			case '\t': sb.append("\\t"); break;
			case '"': sb.append("\\\""); break;
			default: sb.append(s.charAt(i));
	        }
	    }
	    return sb.toString();
	}	
	
	protected void finalize() {
		this.graphDb.shutdown();
	}
}

enum RelType implements RelationshipType
{
	NATURAL,
	RULE,
	RULES //compacted rules
}

class Const {
	final static public String dbPath = "/usr/local/Cellar/neo4j/2.1.5/libexec/data/graph.db";
	final static public String dotPath = "log.dot";
	
	//for relation property
	final static public String SYMBOL = "symbol";
	final static public String SIZE = "size";
	final static public String POS = "pos";
	final static public String STARTPOS = "startPos";
	final static public String ENDPOS = "endPos";
	
	//for RelTypes
	final static public String NATURAL = "natural";
	final static public String RULE = "rule";
}

class RelationshipFactory {
	final private ParsingSource source;
	
	RelationshipFactory(ParsingSource source) {
		this.source = source;
	}

	private Relationship createRelCommon(Node startNode, Node endNode, RelType type) {
		Relationship ret = startNode.createRelationshipTo(endNode, type);
		Object tmp = null;
		long startPos, endPos;
		if ((tmp = startNode.getProperty(Const.POS)) instanceof Long) {
			startPos = (Long)tmp;
		}
		else {
			throw new RuntimeException("invalid value of pos : " + tmp.toString() + " isn't long");
		}
		if ((tmp = endNode.getProperty(Const.POS)) instanceof Long) {
			endPos = (Long)tmp;
		}
		else {
			throw new RuntimeException("invalid value of pos : " + tmp.toString() + " isn't long");
		}
		ret.setProperty(Const.STARTPOS, startPos); 
		ret.setProperty(Const.ENDPOS, endPos); 
		ret.setProperty(Const.SYMBOL, this.source.substring(startPos, endPos));
		switch (type) {
		case NATURAL:
			ret.setProperty(Const.SIZE, endPos - startPos);
			break;
		case RULE:
		case RULES:
			ret.setProperty(Const.SIZE, 1);
			break;
		default:
			throw new RuntimeException("unknown RelType : " + type.toString());
		}
		return ret;
	}
	
	Relationship createNaturalRel(Node startNode, Node endNode) {
		return this.createRelCommon(startNode, endNode, RelType.NATURAL);
	}
	Relationship createRuleRel(Node startNode, Node endNode, String ruleName) {
		Relationship ret = this.createRelCommon(startNode, endNode, RelType.RULE);
		ret.setProperty(Const.RULE, ruleName);
		return ret;
	}
	Relationship createRulesRel(Node startNode, Node endNode, String[] rules) {
		Relationship ret = this.createRelCommon(startNode, endNode, RelType.RULES);
		ret.setProperty(Const.RULE, rules);
		return ret;
	}
}
