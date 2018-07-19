package de.unisaarland.cs.st.cut.graph;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import au.com.bytecode.opencsv.CSVReader;

public class GraphProvider {

    private static GraphProvider instance;
    public DirectedAcyclicGraph<Node, DefaultEdge> graph;
    private List<List<Node>> dependencies;
    private DirectedAcyclicGraph<String, DefaultEdge> classGraph;

    private GraphProvider() {
	graph = new DirectedAcyclicGraph<Node, DefaultEdge>(DefaultEdge.class);
    }

    public static GraphProvider getInstance() {
	if (instance == null) {
	    instance = new GraphProvider();
	}
	return instance;
    }

    // There is an edge from $m_2$ to $m_1$ if $m_2$ depends on $m_1$, i.e.
    // $m_1$ always has to be executed before $m_2$.
    public void buildGraph(String path) throws IOException {
	CSVReader reader = new CSVReader(new FileReader(path));
	List<String[]> all = reader.readAll();
	reader.close();

	HashMap<String, Node> cache = new HashMap<String, Node>();

	for (String[] values : all) {
	    Node n1 = new Node(values[0], values[1]);
	    Node n2 = new Node(values[2], values[3]);

	    if (cache.containsKey(n1.toString())) {
		n1 = cache.get(n1.toString());
	    } else {
		cache.put(n1.toString(), n1);
	    }

	    if (cache.containsKey(n2.toString())) {
		n2 = cache.get(n2.toString());
	    } else {
		cache.put(n2.toString(), n2);
	    }

	    graph.addVertex(n1);
	    graph.addVertex(n2);

	    // graph.addEdge(n1, n2);
	    graph.addEdge(n2, n1); // right
	}
	classGraph = null;
	dependencies = null;
    }

    public void buildExamlpleGraph() {
	String pac = "de.unisaarland.cs.st.parallel_sandbox.";
	Node test1 = new Node(pac + "ExampleTest", "test1");
	Node test2 = new Node(pac + "ExampleTest", "test2");
	Node test3 = new Node(pac + "Example2Test", "test3");
	Node test4 = new Node(pac + "Example2Test", "test4");

	graph.addVertex(test1);
	graph.addVertex(test2);
	graph.addVertex(test3);
	graph.addVertex(test4);

	// graph.addEdge(test1, test3);
    }

    /**
     * 
     * @return A List of schedules that give the right execution order according
     *         to the dependencies
     */
    public List<List<Node>> getDependencies() {
	if (dependencies == null) {
	    dependencies = new LinkedList<List<Node>>();
	    ConnectivityInspector<Node, DefaultEdge> condect = new ConnectivityInspector<Node, DefaultEdge>(graph);
	    List<Set<Node>> subgraphs = condect.connectedSets();

	    for (Set<Node> subnodes : subgraphs) {

		// we need to find all edges in this subgraph too...
		Set<DefaultEdge> edges = new HashSet<DefaultEdge>();
		for (Node n : subnodes) {
		    edges.addAll(graph.edgesOf(n));
		}

		DirectedSubgraph<Node, DefaultEdge> ds = new DirectedSubgraph<Node, DefaultEdge>(graph, subnodes,
			edges);
		TopologicalOrderIterator<Node, DefaultEdge> toi = new TopologicalOrderIterator<Node, DefaultEdge>(ds);
		LinkedList<Node> nodes = new LinkedList<Node>();
		while (toi.hasNext()) {
		    nodes.add(toi.next());
		}
		Collections.reverse(nodes);
		dependencies.add(nodes);
	    }
	}
	return dependencies;
    }

    /**
     * 
     * @return same structure as getDependencies but with method(class)
     *         representation
     */
    public List<List<String>> getDescriptionDependencies() {
	List<List<Node>> deps = getDependencies();

	List<List<String>> ret = new LinkedList<List<String>>();

	for (List<Node> l : deps) {
	    List<String> s = new LinkedList<String>();
	    for (Node n : l) {
		s.add(n.getMethodName() + "(" + n.getClassName() + ")");
	    }
	    ret.add(s);
	}

	return ret;
    }

    public DirectedAcyclicGraph<String, DefaultEdge> getClassDependencies() {

	if (classGraph == null) {
	    classGraph = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);

	    HashMap<String, Set<Node>> npc = new HashMap<String, Set<Node>>();

	    for (Node n : graph.vertexSet()) {
		if (npc.get(n.getClassName()) == null) {
		    npc.put(n.getClassName(), new HashSet<Node>());
		    classGraph.addVertex(n.getClassName());
		}
		npc.get(n.getClassName()).add(n);
	    }

	    for (Entry<String, Set<Node>> entry : npc.entrySet()) {
		for (Node n : entry.getValue()) {
		    for (DefaultEdge e : graph.outgoingEdgesOf(n)) {
			Node target = graph.getEdgeTarget(e);
			if (!target.getClassName().equals(entry.getKey())) {
			    classGraph.addEdge(entry.getKey(), target.getClassName());
			}
		    }
		}
	    }

	}

	return classGraph;
    }

    public static void main(String... args) {
	GraphProvider gp = GraphProvider.getInstance();
	try {
	    gp.buildGraph("/home/skappler/Programming/master/experiments/CRYSTAL-ORIG-DT_LIST.txt.csv");
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	List<List<Node>> dep = gp.getDependencies();
	for (List<Node> ln : dep) {
	    System.out.print("{ ");
	    for (Node n : ln) {
		System.out.print(n + " ");
	    }
	    System.out.println("}");
	}

	System.out.println(gp.getClassDependencies());
    }
}
