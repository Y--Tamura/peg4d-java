package org.peg4d.infer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

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
import org.peg4d.ParsingSource;

public class LatticeNeo4j {
	static enum RelTypes implements RelationshipType
	{
		DEFAULT,
		RULE
	}
	
	private static final String dbPath = "/usr/local/Cellar/neo4j/2.1.5/libexec/data/graph.db";
	private GraphDatabaseService graphDb;
	private Node bosNode;
	private Node eosNode;
	private HashMap<Long, Node> nodeMap;

	LatticeNeo4j(ParsingSource source) {
		this(source, true);
	}
	LatticeNeo4j(ParsingSource source, boolean initializeAllSource) {
		this.nodeMap = new HashMap<>();
		this.graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(LatticeNeo4j.dbPath);
		try (Transaction tx = this.graphDb.beginTx()) {
			this.bosNode = this.graphDb.createNode();
			this.bosNode.setProperty("creation time", ZonedDateTime.now().toString());
			this.bosNode.setProperty("symbol", "%BOS%");
			this.bosNode.setProperty("pos", 0);
			this.nodeMap.put(0L, this.bosNode);
			tx.success();
		}
		if (initializeAllSource) {
			Node current = this.bosNode, next = null;
			Relationship rel = null;
			try (Transaction tx = this.graphDb.beginTx()) {
				for (long i = 0; i < source.length(); i++) {
					next = this.graphDb.createNode();
					next.setProperty("pos", i + 1);
					rel = current.createRelationshipTo(next, RelTypes.DEFAULT);
					rel.setProperty("symbol", source.substring(i, i + 1));
					rel.setProperty("size", 1);
					rel.setProperty("startPos", i);
					rel.setProperty("endPos", i + 1);
					this.nodeMap.put(i + 1, next);
					current = next;
				}
				this.eosNode = next;
				this.eosNode.setProperty("symbol", "%EOS%");
				rel = current.createRelationshipTo(this.eosNode, RelTypes.DEFAULT);
				rel.setProperty("size", 1);
				tx.success();
			}
		}
	}

	public void appendMatchedRule(String ruleName, long startPos, long endPos) {
		try (Transaction tx = this.graphDb.beginTx()) {
			Node startNode = this.getOrCreateNode(startPos);
			Node endNode = this.getOrCreateNode(endPos);
			Relationship rel = startNode.createRelationshipTo(endNode, RelTypes.RULE);
			rel.setProperty("size", "1");
			rel.setProperty("startPos", startPos);
			rel.setProperty("endPos", endPos);
			rel.setProperty("rule", ruleName);
			tx.success();
		}
	}

	private Node getOrCreateNode(long pos) {
		return this.nodeMap.get(pos); //FIX ME
	}

	public ArrayList<ArrayList<String>> generateShortestPath() {
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		try (Transaction tx = this.graphDb.beginTx()) {
			PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(PathExpanders.forDirection(Direction.OUTGOING), "size");
			Iterable<WeightedPath> paths = finder.findAllPaths(this.bosNode, this.eosNode);
			ArrayList<String> format = null;
			String prev = null;
			for (WeightedPath path : paths) {
				format = new ArrayList<>();
				prev = "";
				for (Relationship rel : path.relationships()) {
					if (rel.hasProperty("rule")) {
						if (!prev.isEmpty()) {
							format.add("\"" + prev + "\"");
							prev = "";
						}
						format.add(rel.getProperty("rule").toString());
					}
					else if (rel.hasProperty("symbol")) {
						prev += rel.getProperty("symbol").toString().replace("\n", "\\n");
					}
					else {
						throw new RuntimeException("FIX ME!! unknown relation");
					}
				}
				if (!prev.isEmpty()) format.add("\"" + prev + "\""); 
				ret.add(format);
				System.out.println(format);
			}
			tx.success();
		}
		return ret;
	}
	
	protected void finalize() {
		this.graphDb.shutdown();
	}
}
