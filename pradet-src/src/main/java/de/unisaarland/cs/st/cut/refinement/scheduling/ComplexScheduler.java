package de.unisaarland.cs.st.cut.refinement.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

/**
 * When we compute the schedule for the next test execution, which breaks a
 * target data dependency, we need to be sure to keep intact all the remaining
 * data dependencies. Otherwise, the target test (affected by the data
 * dependency unde test) might fail beacuse of other precondition wrongly set.
 * 
 * This is not necessary if the data dependency are precise (down to single
 * static field); however, in some configurations dependencies are aggregated
 * per test-case testB depends on testA.
 *
 * Note that different options exist:
 * return all the tests (this might be beneficial to expose hidden/missing DD but costs a lot)
 * return all the tests up to the source,target of the target DD
 * return all the tests up to the target DD
 * return only the target DD (isolation)
 * etc..
 * @author gambi
 *
 */
public class ComplexScheduler implements RefinementScheduler {

	private boolean doubleCheck = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.unisaarland.cs.st.cut.refinement.RefinementScheduler#computeSchedule(
	 * org.jgrapht.experimental.dag.DirectedAcyclicGraph,
	 * de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge)
	 */
	@Override
	public List<TestNode> computeSchedule(DirectedAcyclicGraph<TestNode, DependencyEdge> graph,
			DependencyEdge curEdge) {

		// 1. Find the wcc that containts curEdge. curEgde is NOT inverted yet ?
		// 1.1 if RUN_ALL take all the nodes - TODO this is not implemented
		// yet... Question, if this is active which wcc comes first, ideally we
		// should repeat the invocations as in REFERENCE_ORDER.

		// 2. Compute the Topological sort

		// 3. Create the schedule

		// Compute Weakly connected component(s) wccs
		ConnectivityInspector<TestNode, DependencyEdge> condect = new ConnectivityInspector<TestNode, DependencyEdge>(
				graph);
		List<DirectedAcyclicGraph<TestNode, DependencyEdge>> allSubgraphs = new LinkedList<DirectedAcyclicGraph<TestNode, DependencyEdge>>();

		List<Set<TestNode>> wccs = condect.connectedSets();

		for (Set<TestNode> nodesInWcc : wccs) {

			// Rebuild the actual wcc as self-standing graph, only if it
			// contains curEdge
			if (!nodesInWcc.contains(graph.getEdgeSource(curEdge))
					&& !nodesInWcc.contains(graph.getEdgeTarget(curEdge))) {
				// System.out.println(
				// "DependencyRefiner.executeTestsRemoteJUnitCore() WCC does not
				// contain curEdge " + curEdge);
				// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
				// " + nodesInWcc);
				continue;
			}

			DirectedAcyclicGraph<TestNode, DependencyEdge> wcc = new DirectedAcyclicGraph<TestNode, DependencyEdge>(
					DependencyEdge.class);
			// Bookkeeping edges from the original graph
			Set<DependencyEdge> relEdegs = new HashSet<>();

			for (TestNode node : nodesInWcc) {
				// Clone the nodes ? Not necessarily
				wcc.addVertex(node);
				relEdegs.addAll(graph.edgesOf(node));
			}
			for (DependencyEdge e : relEdegs) {
				try {
					DependencyEdge wccEdge = null;

					if (!e.isIgnored()) {
						if (e.equals(curEdge) && !doubleCheck) {
							/*
							 * We invert the edge only if we are not
							 * double-checking the result !
							 */

							// Add this only if the edge is not
							System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() Inverting " + curEdge);
							wccEdge = wcc.addDagEdge(graph.getEdgeTarget(e), graph.getEdgeSource(e));
							//
							wccEdge.setIgnored(false);
							wccEdge.setIntroducesCycle(false);
							wccEdge.setManifest(false);
						} else {
							wccEdge = wcc.addDagEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
							//
							wccEdge.setIgnored(e.isIgnored());
							wccEdge.setIntroducesCycle(e.isIntroducesCycle());
							wccEdge.setManifest(e.isManifest());
						}

					} else {
						System.out.println("DependencyRefiner.computeSchedule() Ignoring " + e);
					}
				} catch (CycleFoundException e1) {
					// This should never happen ...
					// e1.printStackTrace();
					throw new RuntimeException("Edge creates cycle ! " + curEdge);
				}
			}
			allSubgraphs.add(wcc);
		}

		// For the moment, since RUN_ALL is not implemented there should be one
		// and only one wcc

		if (allSubgraphs.size() != 1) {
			System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() ERROR wrong allSubgraph count ! "
					+ allSubgraphs.size());
		}

		// DirectedAcyclicGraph<TestNode, DependencyEdge> interesting = null;
		// Iterator<DirectedAcyclicGraph<TestNode, DependencyEdge>> itt =
		// allSubgraphs.iterator();
		// while (itt.hasNext()) {
		// DirectedAcyclicGraph<TestNode, DependencyEdge> c = itt.next();
		// if (c.containsVertex(graph.getEdgeSource(curEdge)) &&
		// c.containsVertex(graph.getEdgeTarget(curEdge))) {
		// interesting = c;
		// itt.remove();
		// }
		// }

		// MERGE ALL THE SUBGRAPH INTO A SINGLE OBJECT AND FILTER IGNORED DEPS
		DirectedAcyclicGraph<TestNode, DependencyEdge> filteredInteresting = new DirectedAcyclicGraph<>(
				DependencyEdge.class);
		for (DirectedAcyclicGraph<TestNode, DependencyEdge> subGraph : allSubgraphs) {
			for (TestNode tn : subGraph.vertexSet()) {
				filteredInteresting.addVertex(tn);
			}
		}
		for (DirectedAcyclicGraph<TestNode, DependencyEdge> subGraph : allSubgraphs) {
			for (DependencyEdge de : subGraph.edgeSet()) {
				if (de.isIgnored()) {
					System.out.println("DependencyRefinementStrategy.getSchedule() IGNORING DEP: " + de);
				} else {
					// System.out.println("DependencyRefinementStrategy.getSchedule()
					// DEP NOT IGNORED: " + de);
					// This might create a new dep
					filteredInteresting.addEdge(subGraph.getEdgeSource(de), subGraph.getEdgeTarget(de));
				}
			}
		}
		// Remove unconnected components - IS THIS SAFE ?!
		List<TestNode> toRemove = new ArrayList<TestNode>();
		for (TestNode tn : filteredInteresting.vertexSet()) {
			if (filteredInteresting.outDegreeOf(tn) == 0 && filteredInteresting.inDegreeOf(tn) == 0) {
				System.out.println("DependencyRefinementStrategy.getSchedule() TestNode " + tn + " is isolated");
				toRemove.add(tn);
			}
		}

		// Include only the nodes needed to run (source->target)' ! So basically
		// all the reachable nodes from source

		TestNode reachFrom = null;
		if (!doubleCheck) {
			reachFrom = filteredInteresting.getEdgeTarget(curEdge);
		} else {
			reachFrom = filteredInteresting.getEdgeSource(curEdge);
		}
		System.out.print("DependencyRefiner.executeTestsRemoteJUnitCore() Reacheability set from " + reachFrom);

		DepthFirstIterator<TestNode, DependencyEdge> reacheableFromSource = new DepthFirstIterator<>(
				filteredInteresting, reachFrom);

		Set<TestNode> reac = new HashSet<>();
		while (reacheableFromSource.hasNext()) {
			reac.add(reacheableFromSource.next());
		}

		System.out.println(" is " + reac);

		for (TestNode t : filteredInteresting.vertexSet()) {
			if (!reac.contains(t)) {
				// System.out.println(
				// "DependencyRefiner.executeTestsRemoteJUnitCore() TestNode " +
				// t + " is not reacheable");
				toRemove.add(t);
			}
		}

		for (TestNode del : toRemove) {
			// System.out.println("DependencyRefinementStrategy.getSchedule()
			// Remove TestNode" + del);
			filteredInteresting.removeVertex(del);
		}

		// In case of tie, we break it using test IDs.
		TopologicalOrderIterator<TestNode, DependencyEdge> it = new TopologicalOrderIterator<TestNode, DependencyEdge>(
				filteredInteresting,
				new PriorityQueue<TestNode>(filteredInteresting.vertexSet().size(), new Comparator<TestNode>() {

					@Override
					public int compare(TestNode o1, TestNode o2) {
						return o2.id - o1.id;
					}
				}));

		// Build the scheduling for the execution
		List<TestNode> schedule = new ArrayList<>();
		boolean atRequired = false;
		while (it.hasNext()) {
			TestNode s = it.next();

			if (atRequired) { // || DependencyRefiner.RUNALL
				// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
				// Take : " + s);
				schedule.add(s);
			} else if (s.equals(graph.getEdgeTarget(curEdge)) && !doubleCheck) {
				// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
				// Take : " + s);
				schedule.add(s);
				atRequired = true;
			} else if (s.equals(graph.getEdgeSource(curEdge)) && doubleCheck) {
				// else {
				// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
				// Skip : " + s);
				// }
				schedule.add(s);
				atRequired = true;
			}
		}
		Collections.reverse(schedule);

		System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() Schedule is " + schedule);

		// TODO Here we can predict Execution time based on the schedule

		if (schedule.isEmpty())
			throw new RuntimeException("empty schedule!");
		else {
			return schedule;
		}

	}
}
