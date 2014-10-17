package org.peg4d.infer;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
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
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.peg4d.ParsingSource;

public class LatticeNeo4j implements Lattice, Dumper {
	private GraphDatabaseService graphDb;
	private RelationshipFactory relFactory;
	private TreeMap<Long, Node> nodeMap;
	private Node bosNode;
	private Node eosNode;
	
	LatticeNeo4j(ParsingSource source) {
		this(source, false);
	}
	LatticeNeo4j(ParsingSource source, boolean initializeAllSource) {
		this.graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(Const.DB_PATH);
		this.relFactory = new RelationshipFactory(source);
		this.nodeMap = new TreeMap<>();
		try (Transaction tx = this.graphDb.beginTx()) {
			this.bosNode = this.getOrCreateNode(0L);
			this.bosNode.setProperty("creation time", ZonedDateTime.now().toString());
			tx.success();
		}
		if (initializeAllSource) {
			try (Transaction tx = this.graphDb.beginTx()) {
				for (long i = 1; i < source.length(); i++) {
					this.getOrCreateNode(i);
				}
				tx.success();
			}
		}
	}

	public void appendMatchedRule(String ruleName, long startPos, long endPos) {
		if (Options.verbose) {
			System.out.printf("%s[%d, %d]%n", ruleName, startPos, endPos);
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
		if (pos == null) throw new IllegalArgumentException("invalid position is given");
		if ((ret = this.nodeMap.get(pos)) == null) {
			ret = this.graphDb.createNode();
			PropertyManipulator.setPos(ret, pos);
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
							), Const.S_SIZE);
			Iterable<WeightedPath> paths = finder.findAllPaths(this.bosNode, this.eosNode);
			ArrayList<String> format = null;
			String prev = null;
			for (WeightedPath path : paths) {
				format = new ArrayList<>();
				prev = "";
				for (Relationship rel : path.relationships()) {
					if (rel.hasProperty(Const.S_RULE)) {
						if (!prev.isEmpty()) {
							format.add("\"" + prev + "\"");
							prev = "";
						}
						format.add(PropertyManipulator.getRule(rel));
					}
					else if (rel.hasProperty(Const.S_TEXT)) {
						prev += unEscapeString(PropertyManipulator.getText(rel));
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

	private Traverser traverse(boolean includeStartNode, EnumSet<RelType> types) {
		return this.traverse(this.bosNode, includeStartNode, types);
	}
	private Traverser traverse(final Node startNode, boolean includeStartNode, EnumSet<RelType> types) {
	    TraversalDescription td = graphDb.traversalDescription().breadthFirst();
	    if (!includeStartNode) td = td.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelType.NATURAL, RelType.RULE, RelType.RULES));
	    for (RelType type : types) {
	    	td = td.relationships(type, Direction.OUTGOING);
	    }
	    return td.traverse(startNode);
	}
	
	public void dump(StringWriter writer) {
		//this.dump(writer, EnumSet.allOf(RelType.class), EnumSet.of(RelType.NATURAL, RelType.RULE, RelType.RULES));
		this.dump(writer, EnumSet.allOf(RelType.class), EnumSet.of(RelType.NATURAL, RelType.RULES));
	}
	
	public void dump(StringWriter writer, EnumSet<RelType> traverse, EnumSet<RelType> print) {
		try (Transaction tx = this.graphDb.beginTx()) {
			Traverser t = this.traverse(true, traverse);
			Node startNode = null, endNode = null;
			String startLabel = null, endLabel = null, text = null;
			long size = 0;
			for (Path path : t) {
				startNode = path.endNode();
				startLabel = Long.toString(PropertyManipulator.getPos(startNode));
				for (Relationship rel : startNode.getRelationships(Direction.OUTGOING)) {
					endNode = rel.getEndNode();
					endLabel = Long.toString(PropertyManipulator.getPos(endNode));
					size = PropertyManipulator.getSize(rel);
					if (rel.isType(RelType.NATURAL)) {
						text = unEscapeString(PropertyManipulator.getText(rel));
					}
					else if (rel.isType(RelType.RULE) || rel.isType(RelType.RULES)) {
						text = PropertyManipulator.getRule(rel);
					}
					else {
						text = "unknown";
					}
					for (RelType type : print) {
						if (rel.isType(type)) {
							writer.write(String.format("\"%s\" -> \"%s\" [label = \"%s:%s:%d\"]%n",
									startLabel,	endLabel,
									rel.getType().name(), text,	size));
							break;
						}
					}
				}
			}
		}
	}
	
	public void dumpToGraphviz() {
		this.dumpToGraphviz(Const.LOG_FILE, this.bosNode);
	}
	public void dumpToGraphviz(String fileName) {
		this.dumpToGraphviz(fileName, this.bosNode);
	}
	public void dumpToGraphviz(String fileName, Node startNode) {
		try (FileWriter writer = new FileWriter(fileName)) {
			writer.write("digraph {\n");
			this.dump(str -> {
				try {
					writer.write(str);
				}
				catch (IOException e) {
					System.err.println("failed to write Graphviz file : " + fileName);					
				}
			});
			writer.write("}");
		}
		catch (IOException e) {
			System.err.println("failed to dump Graphviz file : " + fileName);
		}
	}
	
	public void compactRules() {
		Traverser t = null;
		try (Transaction tx = this.graphDb.beginTx()) {
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
						ruleList.add(rel.getProperty(Const.S_RULE).toString());
					}
					else {
						ruleList = new ArrayList<>();
						ruleList.add(rel.getProperty(Const.S_RULE).toString());
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

class RelationshipFactory {
	final private ParsingSource source;
	
	RelationshipFactory(ParsingSource source) {
		this.source = source;
	}

	private Relationship createRelCommon(Node startNode, Node endNode, RelType type) {
		Relationship ret = startNode.createRelationshipTo(endNode, type);
		Object tmp = null;
		long startPos, endPos;
		if ((tmp = startNode.getProperty(Const.S_POS)) instanceof Long) {
			startPos = (Long)tmp;
		}
		else {
			throw new RuntimeException("invalid value of pos : " + tmp.toString() + " isn't long");
		}
		if ((tmp = endNode.getProperty(Const.S_POS)) instanceof Long) {
			endPos = (Long)tmp;
		}
		else {
			throw new RuntimeException("invalid value of pos : " + tmp.toString() + " isn't long");
		}
		PropertyManipulator.setStartPos(ret, startPos);
		PropertyManipulator.setEndPos(ret, endPos);
		PropertyManipulator.setText(ret, this.source.substring(startPos, endPos));
		switch (type) {
		case NATURAL:
			PropertyManipulator.setSize(ret, endPos - startPos);
			break;
		case RULE:
		case RULES:
			PropertyManipulator.setSize(ret, 1);
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
		PropertyManipulator.setRule(ret, ruleName);
		return ret;
	}
	Relationship createRulesRel(Node startNode, Node endNode, String[] rules) {
		Relationship ret = this.createRelCommon(startNode, endNode, RelType.RULES);
		PropertyManipulator.setRule(ret, Arrays.toString(rules));
		PropertyManipulator.setRules(ret, rules);
		return ret;
	}
}

class PropertyManipulator {
	static String getText(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_TEXT);
		if (obj instanceof String) {
			return (String)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setText(PropertyContainer container, String text) {
		container.setProperty(Const.S_TEXT, text);
	}

	static String getRule(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_RULE);
		if (obj instanceof String) {
			return (String)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setRule(PropertyContainer container, String text) {
		container.setProperty(Const.S_RULE, text);
	}

	static String[] getRules(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_RULES);
		if (obj instanceof String[]) {
			return (String[])obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setRules(PropertyContainer container, String[] text) {
		container.setProperty(Const.S_RULES, text);
	}

	static long getSize(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_SIZE);
		if (obj instanceof Long) {
			return (Long)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setSize(PropertyContainer container, long size) {
		container.setProperty(Const.S_SIZE, size);
	}

	static long getPos(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_POS);
		if (obj instanceof Long) {
			return (Long)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setPos(PropertyContainer container, long size) {
		container.setProperty(Const.S_POS, size);
	}

	static long getStartPos(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_STARTPOS);
		if (obj instanceof Long) {
			return (Long)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setStartPos(PropertyContainer container, long size) {
		container.setProperty(Const.S_STARTPOS, size);
	}
	
	static long getEndPos(PropertyContainer container) {
		Object obj = container.getProperty(Const.S_ENDPOS);
		if (obj instanceof Long) {
			return (Long)obj;
		}
		else {
			throw new RuntimeException("stored invalid Property : " + obj);
		}
	}
	static void setEndPos(PropertyContainer container, long size) {
		container.setProperty(Const.S_ENDPOS, size);
	}
}
