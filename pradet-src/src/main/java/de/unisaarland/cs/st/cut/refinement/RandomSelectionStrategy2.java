package de.unisaarland.cs.st.cut.refinement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.traverse.DepthFirstIterator;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

/**
 * This implements the break cycles as described in the FSE paper.
 * 
 * @author gambi
 *
 */
public class RandomSelectionStrategy2 implements SelectionStrategy {

	private Random random;
	// This keep the list of edges to test when we come across a cycle.
	private Queue<DependencyEdge> localBuffer = new LinkedList<DependencyEdge>();
	private Set<DependencyEdge> commonPreconditions = new HashSet<DependencyEdge>();

	public RandomSelectionStrategy2() {
		Long seed = Long.getLong("seed");
		if (seed == null) {
			seed = System.currentTimeMillis();
			System.out.println("No seed specified. Using randomly generated seed " + seed);
		} else {
			System.out.println("Using provided seed " + seed);
		}
		random = new Random(seed);
	}

	// We need to ignore the ignored deps !
	private DirectedAcyclicGraph<TestNode, DependencyEdge> createCycle(
			final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph, DependencyEdge edge) {
		// Clone the graph,
		DirectedAcyclicGraph<TestNode, DependencyEdge> copy = new DirectedAcyclicGraph<TestNode, DependencyEdge>(
				DependencyEdge.class);

		for (TestNode tn : dependencyGraph.vertexSet()) {
			TestNode tnCopy = new TestNode();
			tnCopy.id = tn.id;
			tnCopy.name = tn.name;
			copy.addVertex(tnCopy);
		}
		for (DependencyEdge de : dependencyGraph.edgeSet()) {

			if (de.isIgnored()) {
				System.out.println("RandomSelectionStrategy2.createCycle() Ignoring " + de);
				continue;
			}

			TestNode toVertex = null;
			TestNode fromVertex = null;

			for (TestNode t : copy.vertexSet()) {
				if (t.name.equals(dependencyGraph.getEdgeSource(de).name)) {
					fromVertex = t;
					continue;
				}
				if (t.name.equals(dependencyGraph.getEdgeTarget(de).name)) {
					toVertex = t;
					continue;
				}

				if (fromVertex != null && toVertex != null) {
					break;
				}
			}

			try {
				copy.addDagEdge(fromVertex, toVertex);
			} catch (CycleFoundException e1) {
				throw new RuntimeException(e1);
			}
		}

		///
		TestNode src = null;
		TestNode tgt = null;
		for (TestNode t : copy.vertexSet()) {
			if (t.name.equals(dependencyGraph.getEdgeSource(edge).name)) {
				src = t;
				continue;
			}
			if (t.name.equals(dependencyGraph.getEdgeTarget(edge).name)) {
				tgt = t;
				continue;
			}

			if (src != null && tgt != null) {
				break;
			}
		}
		DependencyEdge eCopy = copy.getEdge(src, tgt);

		System.out.println("RandomSelectionStrategy2.selectEdge() Testing " + eCopy + " for cycle");

		copy.removeEdge(eCopy);
		try {
			copy.addDagEdge(tgt, src);
			return copy;
		} catch (CycleFoundException ex) {
			return null;
		} finally {
			// Clean up?
		}

	}

	private DependencyEdge fromLocalBuffer(final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {
		while (!localBuffer.isEmpty()) {
			// Remove from local buffer
			DependencyEdge e = localBuffer.poll();

			System.out.println("\n\n\n Selecting Dependency EDGE from the Local Buffer: " + e);
			// Configure the ignore tags
			for (DependencyEdge cp : commonPreconditions) {
				if (cp.equals(e)) {
					cp.setIgnored(false);
				} else {
					System.out.println("RandomSelectionStrategy2.selectEdge() IGNORING  " + cp);
					// Ignoring the others
					cp.setIgnored(true);
				}
			}

			// Check the edge
			if (createCycle(dependencyGraph, e) != null) {
				return e;
			} else {
				System.out.println(
						"RandomSelectionStrategy2.selectEdge() Cycle inside cycle for " + e + " Local Buffer. SKIP");
				continue;
			}
		}
		return null;
	}

	private void fillLocalBuffer(final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph,
			DependencyEdge e) {
		/*
		 * test source(e) has multiple preconditions. Collect them and process
		 * them !
		 */
		final Comparator<DependencyEdge> c = new Comparator<DependencyEdge>() {

			@Override
			public int compare(DependencyEdge o1, DependencyEdge o2) {
				return dependencyGraph.getEdgeTarget(o1).id - dependencyGraph.getEdgeTarget(o2).id;
			}
		};

		List<DependencyEdge> orderedEdges = new ArrayList<>();
		for (DependencyEdge outgoing : dependencyGraph.outgoingEdgesOf(dependencyGraph.getEdgeSource(e))) {
			orderedEdges.add(outgoing);
		}
		Collections.sort(orderedEdges, c);
		// Enqueue Deps for the next round
		Collections.reverse(orderedEdges);
		// Not sure add all preserve the order
		for (DependencyEdge de : orderedEdges) {
			localBuffer.add(de);
			commonPreconditions.add(de);
			System.out.println("RandomSelectionStrategy2.selectEdge() Adding " + de + " to Local Buffer ");
		}
	}

	private DirectedAcyclicGraph<TestNode, DependencyEdge> duplicate(
			DirectedAcyclicGraph<TestNode, DependencyEdge> originalGraph) {
		DirectedAcyclicGraph<TestNode, DependencyEdge> copy = new DirectedAcyclicGraph<TestNode, DependencyEdge>(
				DependencyEdge.class);
		for (TestNode tn : originalGraph.vertexSet()) {
			TestNode tnCopy = new TestNode();
			tnCopy.id = tn.id;
			tnCopy.name = tn.name;
			copy.addVertex(tnCopy);
		}
		for (DependencyEdge de : originalGraph.edgeSet()) {
			TestNode toVertex = null;
			TestNode fromVertex = null;

			for (TestNode t : copy.vertexSet()) {
				if (t.name.equals(originalGraph.getEdgeSource(de).name)) {
					fromVertex = t;
					continue;
				}
				if (t.name.equals(originalGraph.getEdgeTarget(de).name)) {
					toVertex = t;
					continue;
				}

				if (fromVertex != null && toVertex != null) {
					break;
				}
			}

			try {
				copy.addDagEdge(fromVertex, toVertex);
			} catch (CycleFoundException e1) {
				throw new RuntimeException(e1);
			}
		}
		return copy;
	}

	/**
	 * Note that this is disruptive, later we call it using duplicate in front !
	 * 
	 * @param dependencyGraph
	 * @param sourceTestNode
	 * @param targetTestNode
	 * @return
	 */
	private boolean introduceCycle(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph, //
			TestNode sourceTestNode, TestNode targetTestNode) {
		TestNode src = null;
		TestNode tgt = null;
		for (TestNode t : dependencyGraph.vertexSet()) {
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

		DependencyEdge eCopy = dependencyGraph.getEdge(src, tgt);
		dependencyGraph.removeEdge(eCopy);
		// A cycle will result in an exception
		try {
			dependencyGraph.addDagEdge(tgt, src);
			return false;
		} catch (CycleFoundException ex) {
			System.out.println("introduceCycle()");
		}

		return true;
	}

	/**
	 * This must ensure that when we return an edge this will not create a
	 * cycle. This includes also the case with 'ignored' deps.
	 */
	@Override
	public DependencyEdge selectEdge(final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {

		DependencyEdge d = fromLocalBuffer(dependencyGraph);
		if (d != null)
			return d;

		// Reset ignore attribute
		if (!commonPreconditions.isEmpty()) {
			for (DependencyEdge cp : commonPreconditions) {
				cp.setIgnored(false);
			}
			commonPreconditions.clear();
		}

		// Randomize the remaining edges
		LinkedList<DependencyEdge> edges = new LinkedList<>();
		edges.addAll(dependencyGraph.edgeSet());
		Collections.shuffle(edges, random);

		System.out.println("RandomSelectionStrategy2.selectEdge() Remaining Edges " + edges.size());
		//
		for (DependencyEdge e : edges) {

			System.out.println("RandomSelectionStrategy2.selectEdge() Select " + e);
			// Skip Dependencies that are already manifest
			if (e.isManifest()) {
				System.out.println("RandomSelectionStrategy2.selectEdge() Dep is already manifest");
				continue;
			}

			// TODO Comment on what's the meaning of this one !
			if (dependencyGraph.outDegreeOf(dependencyGraph.getEdgeSource(e)) > 1) {
				fillLocalBuffer(dependencyGraph, e);
				// At this point we might return the first in line
				d = fromLocalBuffer(dependencyGraph);
				if (d != null) {
					System.out.println("RandomSelectionStrategy2.selectEdge() Returning " + d + " from local buffer");
					return d;
				}

			} else {

				// TODO Better compute the common preconditions here, even
				// before
				// checking for cycles

				// Check if by reverting the edge we introduce a cycle:
				// 1 - Create a full duplicate of the graph: if THIS already has
				// cycle, something was wrong !

				// 2 - Find in the duplicate the edge corresponding to the
				// targetDataDependency and remove it from the graph
				if (introduceCycle(duplicate(dependencyGraph), dependencyGraph.getEdgeSource(e),
						dependencyGraph.getEdgeTarget(e))) {
					System.out.println("RandomSelectionStrategy2.selectEdge() Found a cycle with " + e
							+ " select the best dep to break the cycle");

				} else {
					System.out.println("RandomSelectionStrategy2.selectEdge() Inverting Dependency EDGE " + e);

					if (dependencyGraph.outDegreeOf(dependencyGraph.getEdgeSource(e)) > 1) {
						fillLocalBuffer(dependencyGraph, e);

						d = fromLocalBuffer(dependencyGraph);
						if (d != null)
							return d;

					} else {
						return e;
					}

				}
			}
		}

		// Here we did not find anything
		System.out.println("DependencyRefiner.selectEdge() Stopping the search ");
		return null;
	}

}
