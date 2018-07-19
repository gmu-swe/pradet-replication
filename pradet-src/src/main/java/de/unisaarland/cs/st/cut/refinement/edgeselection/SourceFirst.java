package de.unisaarland.cs.st.cut.refinement.edgeselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import de.unisaarland.cs.st.cut.graph.util.GraphUtils;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;
import de.unisaarland.cs.st.cut.refinement.SelectionStrategy;

/**
 * 
 * 
 * @author gambi
 *
 */
public class SourceFirst implements SelectionStrategy {

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

	private List<DependencyEdge> dependenciesToTest = new ArrayList<DependencyEdge>();
	private Iterator<DependencyEdge> iterator = null;

	/**
	 * Build an iterator over the dependencies at first use and then use that
	 * iterator after that. This works since the approach is deterministic.
	 */
	@Override
	public DependencyEdge selectEdge(final DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {

		boolean debug = false;
		if (iterator == null || !(iterator.hasNext())) {
			try {
				if (iterator == null) {
					System.out.println("SourceFirst.selectEdge() Build the dependency iterator on first use");
				} else {
					System.out.println("SourceFirst.selectEdge() Re-Build the dependency iterator !!");
					debug = true;
					dependenciesToTest = new ArrayList<DependencyEdge>();
				}

				ConnectivityInspector<TestNode, DependencyEdge> condect = new ConnectivityInspector<TestNode, DependencyEdge>(
						dependencyGraph);

				List<Set<TestNode>> wccs = condect.connectedSets();

				System.out.println("SourceFirst.selectEdge() Found " + wccs.size() + " WCCs ");
				// Sort by MAX ID in wccs. Bigger ID first
				Collections.sort(wccs, new Comparator<Set<TestNode>>() {

					private int findMax(Set<TestNode> set) {
						int max = -1;
						for (TestNode t : set)
							if (t.id >= max)
								max = t.id;
						return max;
					}

					@Override
					public int compare(Set<TestNode> o1, Set<TestNode> o2) {
						int max1 = findMax(o1);
						int max2 = findMax(o2);
						// Note 2 before 1 !
						return max2 - max1;
					}
				});

				for (Set<TestNode> wccNodes : wccs) {

					if (wccNodes.size() == 1) {
						System.out.println("SourceFirst.selectEdge() Skipping WCC " + wccs.indexOf(wccNodes) + " with "
								+ wccNodes.size() + " nodes");
						continue;
					}

					System.out.println("SourceFirst.selectEdge() Processing WCC " + wccs.indexOf(wccNodes) + " with "
							+ wccNodes.size() + " nodes");

					if (debug) {
						System.out.println("SourceFirst.selectEdge() WCC Contains: " + wccNodes);
					}

					List<TestNode> orderedNodes = new ArrayList<TestNode>(wccNodes);
					Collections.sort(orderedNodes, new Comparator<TestNode>() {

						@Override
						public int compare(TestNode o1, TestNode o2) {
							return o2.id - o1.id;
						}
					});

					for (TestNode test : orderedNodes) {
						if (debug) {
							System.out.println("SourceFirst.selectEdge() Processing Test node: " + test);
						}
						// Get all the deps that include this node as source and
						// sort them from the bigger to the smaller test node id
						List<DependencyEdge> outgoing = new ArrayList<DependencyEdge>(
								dependencyGraph.outgoingEdgesOf(test));

						if (debug) {
							System.out.println(
									"SourceFirst.selectEdge() Test " + test + " has outgoind edges " + outgoing);
						}
						//
						if (outgoing.isEmpty()) {
							if (debug) {
								System.out.println(
										"SourceFirst.selectEdge() Test " + test + " has no  outgoind edges. Skip it");
							}
							continue;
						}
						//
						Collections.sort(outgoing, new Comparator<DependencyEdge>() {
							@Override
							public int compare(DependencyEdge o1, DependencyEdge o2) {
								return dependencyGraph.getEdgeTarget(o2).id - dependencyGraph.getEdgeTarget(o1).id;
							}
						});

						// Add them to the list of deps to test
						dependenciesToTest.addAll(outgoing);
					}
				}

				iterator = dependenciesToTest.iterator();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		while (iterator.hasNext()) {
			DependencyEdge e = iterator.next();

			if (debug) {
				System.out.println("SourceFirst.selectEdge() Selecting Edge " + e);
			}

			// We must check all the time because Manifest Deps might introduce
			// problems !
			if (isTestable(dependencyGraph, dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e))
					&& !introduceCycle(dependencyGraph, dependencyGraph.getEdgeSource(e),
							dependencyGraph.getEdgeTarget(e))) {
				return e;
			}
		}

		// Here we did not find anything
		System.out.println("SelectEdge() No more edges available. Stopping refinement");
		return null;
	}

	/**
	 * Source is the test WHICH requires Target to be executes
	 * 
	 * Target not in PRE( Source - Target ) - not a formal definition
	 * 
	 * Basically if we compute all the possible paths from Source to Target, the
	 * count should be 1 at the time we compute it !
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @param dependencyGraph
	 * @param sourceTestNode
	 * @param targetTestNode
	 * @return
	 */
	private boolean isTestable(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph, //
			TestNode sourceTestNode, TestNode targetTestNode) {

		System.out.println("SourceFirst.isTestable() " + sourceTestNode + " -- " + targetTestNode + " Edge is "
				+ dependencyGraph.getEdge(sourceTestNode, targetTestNode));

		if (dependencyGraph.getEdge(sourceTestNode, targetTestNode).isManifest()) {
			System.out.println("SourceFirst.isTestable() Dep is already manifest.");
			return false;
		}

		// Find the WCC that contains source and target
		// Compute Weakly connected component(s) wccs
		ConnectivityInspector<TestNode, DependencyEdge> condect = new ConnectivityInspector<TestNode, DependencyEdge>(
				dependencyGraph);

		List<Set<TestNode>> wccs = condect.connectedSets();

		// DEBUG Mostly
		int count = 0;
		for (Set<TestNode> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(sourceTestNode) && !nodesInWcc.contains(targetTestNode)) {
				continue;
			}
			count++;
		}
		if (count != 1) {
			System.out.println("SourceFirst.isTestable() ERROR: Wrong number of WCC that contain "
					+ dependencyGraph.getEdge(sourceTestNode, targetTestNode) + ". Count is " + count);
			throw new RuntimeException("ERROR: Wrong number of WCC that contain "
					+ dependencyGraph.getEdge(sourceTestNode, targetTestNode) + ". Count is " + count);
		}

		// END OF DEBUG

		for (Set<TestNode> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(sourceTestNode) && !nodesInWcc.contains(targetTestNode)) {
				continue;
			}

			// Build a WCC graph
			DirectedAcyclicGraph<TestNode, DependencyEdge> wcc = new DirectedAcyclicGraph<TestNode, DependencyEdge>(
					DependencyEdge.class);
			Set<DependencyEdge> relEdegs = new HashSet<>();
			for (TestNode node : nodesInWcc) {
				wcc.addVertex(node);
				relEdegs.addAll(dependencyGraph.edgesOf(node));
			}
			for (DependencyEdge e : relEdegs) {
				try {
					DependencyEdge wccEdge = null;
					wccEdge = wcc.addDagEdge(dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e));
					wccEdge.setIgnored(e.isIgnored());
					wccEdge.setIntroducesCycle(e.isIntroducesCycle());
					wccEdge.setManifest(e.isManifest());
				} catch (Exception ex) {
					// This should never happen
					System.out.println("SourceFirst.isTestable() ERROR !!");
					throw new RuntimeException(ex);
				}
			}

			System.out.println("SourceFirst.isTestable() all Paths start for " + wcc.vertexSet().size() + " -- "
					+ wcc.edgeSet().size());

			long start = System.currentTimeMillis();

			AllDirectedPaths<TestNode, DependencyEdge> allPaths = new AllDirectedPaths<TestNode, DependencyEdge>(wcc);

			int paths = allPaths.getAllPaths(sourceTestNode, targetTestNode, true, wcc.edgeSet().size()).size();

			long end = System.currentTimeMillis();
			System.out.println("SourceFirst.isTestable() all Paths took " + ((end - start) / 1000));
			//
			if (paths > 1) {
				System.out.println("SourceFirst.isTestable() Multiple paths between " + sourceTestNode + " and "
						+ targetTestNode + " dependency is not testable");
				return false;
			} else {
				System.out.println("SourceFirst.isTestable() Single paths between " + sourceTestNode + " and "
						+ targetTestNode + " dependency is testable");
				return true;
			}
		}
		//
		System.out.println("SourceFirst.isTestable() No more WCC to test, return null");

		// Default
		//
		return false;
	}

}
