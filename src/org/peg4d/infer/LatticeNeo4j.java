package org.peg4d.infer;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
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
		this(source, true);
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
			Node current = this.bosNode, next = null;
			try (Transaction tx = this.graphDb.beginTx()) {
				for (long i = 0; i < source.length(); i++) {
					next = this.getOrCreateNode(i + 1);
//					this.relFactory.createNaturalRel(current, next);
					current = next;
				}
				this.eosNode = next;
				tx.success();
			}
		}
	}

	public void appendMatchedRule(String ruleName, long startPos, long endPos) {
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
//			if ((tmp = this.nodeMap.higherEntry(pos)) != null) {
//				this.relFactory.createNaturalRel(ret, tmp.getValue());
//			}
			this.nodeMap.put(pos, ret);
		}
		return ret;
	}

	public ArrayList<ArrayList<String>> generateShortestPath() {
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		try (Transaction tx = this.graphDb.beginTx()) {
			PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(PathExpanders.forDirection(Direction.OUTGOING), Const.SIZE);
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
						format.add(rel.getProperty(Const.RULE).toString());
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
	
	private Traverser traverseNatural(final Node startNode, boolean includeStartNode)
	{
	    TraversalDescription td = graphDb.traversalDescription()
	            .breadthFirst()
	            .relationships(RelTypes.NATURAL, Direction.OUTGOING);
	    if (!includeStartNode) td = td.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelTypes.NATURAL));
	    return td.traverse(startNode);
	}
	private Traverser traverseRule(final Node startNode, boolean includeStartNode)
	{
	    TraversalDescription td = graphDb.traversalDescription()
	            .breadthFirst()
	            .relationships(RelTypes.RULE, Direction.OUTGOING);
	    if (!includeStartNode) td = td.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelTypes.RULE));
	    return td.traverse(startNode);
	}
	private Traverser traverseAll(final Node startNode, boolean includeStartNode)
	{
	    TraversalDescription td = graphDb.traversalDescription()
	            .breadthFirst()
	            .relationships(RelTypes.RULE, Direction.OUTGOING)
	    		.relationships(RelTypes.NATURAL, Direction.OUTGOING);
	    if (!includeStartNode) td = td.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelTypes.NATURAL, RelTypes.RULE));
	    return td.traverse(startNode);
	}
	
	public void dumpToGraphviz() {
		this.dumpToGraphviz(Const.dotPath, this.bosNode);
	}
	public void dumpToGraphviz(String fileName) {
		this.dumpToGraphviz(fileName, this.bosNode);
	}
	public void dumpToGraphviz(String fileName, Node startNode) {
		Traverser t = null;
		FileWriter writer = null;
		try (Transaction tx = this.graphDb.beginTx()) {
			writer = new FileWriter(fileName);
			writer.write("digraph {\n");
			t = this.traverseAll(this.bosNode, true);
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
					if (rel.isType(RelTypes.NATURAL)) {
						writer.write(unEscapeString(rel.getProperty(Const.SYMBOL).toString()) + "\""); 
					}
					else if (rel.isType(RelTypes.RULE)) {
						writer.write(rel.getProperty(Const.RULE) + "\"");
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
			writer.close();
		} catch (IOException e) {
			System.out.println("error : cannot dump log.dot");
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

enum RelTypes implements RelationshipType
{
	NATURAL,
	RULE
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

	private Relationship createRelCommon(Node startNode, Node endNode, RelTypes type) {
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
			ret.setProperty(Const.SIZE, 1);
			break;
		default:
			throw new RuntimeException("unknown RelType : " + type.toString());
		}
		
		return ret;
	}
	
	Relationship createNaturalRel(Node startNode, Node endNode) {
		return this.createRelCommon(startNode, endNode, RelTypes.NATURAL);
	}
	Relationship createRuleRel(Node startNode, Node endNode, String ruleName) {
		Relationship ret = this.createRelCommon(startNode, endNode, RelTypes.RULE);
		ret.setProperty(Const.RULE, ruleName);
		return ret;
	}
}
