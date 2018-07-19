package de.unisaarland.cs.st.cut.refinement.edgeselection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import de.unisaarland.cs.st.cut.graph.util.GraphUtils;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;
import de.unisaarland.cs.st.cut.refinement.SelectionStrategy;

/**
 * Randomly select an edge which does not create a cycle when inverted.
 * 
 * @author gambi
 *
 */
public class SimpleRandom implements SelectionStrategy {

	private Random random;

	public SimpleRandom() {
		Long seed = Long.getLong("seed");
		if (seed == null) {
			seed = System.currentTimeMillis();
			System.out.println("No seed specified. Using randomly generated seed " + seed);
		} else {
			System.out.println("Using provided seed " + seed);
		}
		random = new Random(seed);
	}

	/**
	 * Check if the provided graph contains a cycle when edge is inverted. The
	 * input graph is duplicated - might take some time/memory - and the
	 * provided edge must belong to the input graph.
	 * 
	 * TODO This can be moved to UTILITY or ABSTRACT Class
	 * 
	 * @param copy
	 * @param edge
	 * @return
	 */
	private boolean introduceCycle(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph, //
			TestNode sourceTestNode, TestNode targetTestNode) {

		DirectedAcyclicGraph<TestNode, DependencyEdge> copy = GraphUtils.duplicate(dependencyGraph);

		TestNode src = null;
		TestNode tgt = null;
		for (TestNode t : copy.vertexSet()) {
			if (t.name.equals(sourceTestNode.name)) {
				src = t;
				continue;
			}
			if (t.name.equals(targetTestNode.name)) {
				tgt = t;
				continue;
			}

			if (src != null && tgt != null) {
				break;
			}
		}

		DependencyEdge eCopy = copy.getEdge(src, tgt);
		copy.removeEdge(eCopy);
		// A cycle will result in an exception
		try {
			copy.addDagEdge(tgt, src);
			return false;
		} catch (CycleFoundException ex) {
			// System.out.println("introduceCycle()");
			return true;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@Override
	public DependencyEdge selectEdge(final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {

		// Randomize the edges using the provided seed
		LinkedList<DependencyEdge> edges = new LinkedList<>();
		edges.addAll(dependencyGraph.edgeSet());
		Collections.shuffle(edges, random);

		for (DependencyEdge e : edges) {

			// Skip Dependency Edges that are already manifest
			if (e.isManifest()) {
				continue;
			}
			// Check
			if (introduceCycle(dependencyGraph, dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e))) {
				// System.out.println("selectEdge() " + e + " will introduce a
				// cycle. Skip !");

			} else {
				// System.out.println("selectEdge() Return " + e);
				return e;
			}
		}

		// Here we did not find anything
		System.out.println("SelectEdge() No more edges available. Stopping refinement");
		return null;
	}

}
