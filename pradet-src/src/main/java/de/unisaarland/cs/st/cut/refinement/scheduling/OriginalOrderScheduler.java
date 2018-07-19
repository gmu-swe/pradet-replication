package de.unisaarland.cs.st.cut.refinement.scheduling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.traverse.BreadthFirstIterator;

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
 * Note that different options exist: return all the tests (this might be
 * beneficial to expose hidden/missing DD but costs a lot) return all the tests
 * up to the source,target of the target DD return all the tests up to the
 * target DD return only the target DD (isolation) etc..
 * 
 * TODO NOTE THAT WE DO NOT CARE ABOUT IGNORED DEPS AT THE MOMENT.
 * 
 * @author gambi
 *
 */
public class OriginalOrderScheduler implements RefinementScheduler {

	// TODO Immutable ?
	private List<TestNode> originalOrder = new ArrayList<TestNode>();

	public OriginalOrderScheduler(List<TestNode> originalOrder) {
		this.originalOrder.addAll(originalOrder);
	}

	private boolean doubleCheck = false;

	private DirectedAcyclicGraph<TestNode, DependencyEdge> findWccContainingTargetDataDependency(
			DirectedAcyclicGraph<TestNode, DependencyEdge> graph, DependencyEdge targetDataDependency)
			throws InvalidScheduleException {
		// Compute Weakly connected component(s) wccs
		ConnectivityInspector<TestNode, DependencyEdge> condect = new ConnectivityInspector<TestNode, DependencyEdge>(
				graph);

		List<Set<TestNode>> wccs = condect.connectedSets();

		// DEBUG Mostly
		int count = 0;
		for (Set<TestNode> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(graph.getEdgeSource(targetDataDependency))
					&& !nodesInWcc.contains(graph.getEdgeTarget(targetDataDependency))) {
				continue;
			}
			count++;
		}
		if (count != 1) {
			throw new RuntimeException(
					"ERROR: Wrong number of WCC that contain " + targetDataDependency + ". Count is " + count);
		}

		// END OF DEBUG

		for (Set<TestNode> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(graph.getEdgeSource(targetDataDependency))
					&& !nodesInWcc.contains(graph.getEdgeTarget(targetDataDependency))) {
				continue;
			}

			System.out.println("OriginalOrderScheduler.computeSchedule() Found WCC that contains "
					+ targetDataDependency + ":\n" + nodesInWcc);

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

					if (e.equals(targetDataDependency)) {
						/*
						 * We invert the edge only if we are not double-checking
						 * the result !
						 */

						// Add this only if the edge is not
						if (Boolean.getBoolean("debug")) {
							System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() Inverting "
									+ targetDataDependency);
						}
						wccEdge = wcc.addDagEdge(graph.getEdgeTarget(e), graph.getEdgeSource(e));

						// wccEdge.setIgnored(false);
						// wccEdge.setIntroducesCycle(false);
						// wccEdge.setManifest(false);
					} else {
						wccEdge = wcc.addDagEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
					}
					// Maintain all the attributes of the original graph !
					// WHAT IF TARGET DEP IS IGNORED ?!!
					wccEdge.setIgnored(e.isIgnored());
					wccEdge.setIntroducesCycle(e.isIntroducesCycle());
					wccEdge.setManifest(e.isManifest());

				} catch (CycleFoundException e1) {
					// This should never happen ...
					throw new InvalidScheduleException("An Edge creates cycle ! " + targetDataDependency);
				}
			}
			return wcc;
		}
		throw new RuntimeException("Cannot find WCC containing " + targetDataDependency);

	}

	@Override
	public List<TestNode> computeSchedule(DirectedAcyclicGraph<TestNode, DependencyEdge> dataDependencyGraph,
			DependencyEdge targetDataDependency) throws InvalidScheduleException {

		// 1. Find the wcc that contains targetDataDependency, since only the
		// WCCs make sense here (basically this scheduler does not support the
		// RUNALL option). By definition only 1 WCC shall contain the
		// targetDataDependency. Node that we IGNORE, i.e., do not report
		// ignored dependencies in the

		TestNode dependentTest = dataDependencyGraph.getEdgeSource(targetDataDependency);
		TestNode preconditionSetterTest = dataDependencyGraph.getEdgeTarget(targetDataDependency);

		// Nodes should be the same objects, edges are different...
		DirectedAcyclicGraph<TestNode, DependencyEdge> wcc = findWccContainingTargetDataDependency(dataDependencyGraph,
				targetDataDependency);

		// 2. Compute the precondition of dependentTest minus the
		// preconditionSetterTest
		// wcc);
		BreadthFirstIterator<TestNode, DependencyEdge> it = new BreadthFirstIterator<TestNode, DependencyEdge>(
				dataDependencyGraph, preconditionSetterTest);
		// Fast forward to preconditionSetterTest node
		List<TestNode> preconditions = new ArrayList<TestNode>();
		while (it.hasNext()) {
			preconditions.add(it.next());
		}
		preconditions.remove(preconditionSetterTest);

		if (Boolean.getBoolean("debug")) {
			System.out.println("OriginalOrderScheduler.computeSchedule() Preconditions  are :" + preconditions);
		}
		// Build the schedule for the execution: it contains only the nodes in
		// WCC that are preconditions or dependentTest AND nodes are ordered
		// according
		// to originalOrder
		//
		// We obtain this by cloning originalOrder and removing all the nodes
		// that are not in the preconditions
		List<TestNode> schedule = new ArrayList<TestNode>();
		if (Boolean.getBoolean("debug")) {
			System.out.println("OriginalOrderScheduler.computeSchedule() Original Order " + originalOrder);
		}
		for (TestNode t : originalOrder) {
			if (preconditions.contains(t)) {
				schedule.add(t);
			}
		}
		// Add dependentTest
		schedule.add(dependentTest);

		// boolean atRequired = false;
		// while (it.hasNext()) {
		// TestNode s = it.next();
		//
		// if (atRequired) { // || DependencyRefiner.RUNALL
		// //
		// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
		// // Take : " + s);
		// schedule.add(s);
		// } else if (s.equals(graph.getEdgeTarget(targetDataDependency)) &&
		// !doubleCheck) {
		// //
		// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
		// // Take : " + s);
		// schedule.add(s);
		// atRequired = true;
		// } else if (s.equals(graph.getEdgeSource(targetDataDependency)) &&
		// doubleCheck) {
		// // else {
		// //
		// System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore()
		// // Skip : " + s);
		// // }
		// schedule.add(s);
		// atRequired = true;
		// }
		// }
		// Since we follow already the original order we do not need to keep
		// this
		// Collections.reverse(schedule);

		// TODO Here we can predict Execution time based on the schedule

		if (schedule.isEmpty())
			throw new RuntimeException("Empty schedule!");
		else {
			return schedule;
		}

	}
}
