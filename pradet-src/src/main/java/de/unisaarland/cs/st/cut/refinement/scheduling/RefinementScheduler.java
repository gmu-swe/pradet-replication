package de.unisaarland.cs.st.cut.refinement.scheduling;

import java.util.List;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

public interface RefinementScheduler {

	/**
	 * Given the dependency graph and a target dependency to test in it, returns
	 * a schedule for the next test execution.
	 * 
	 * 
	 * Shall this consider also the test which set the preconditions ? Why ?!
	 * TODO Make this configurable ?
	 * 
	 * @param graph
	 * @param curEdge
	 * @param doubleCheck
	 * @return
	 * @throws InvalidScheduleException
	 */
	List<TestNode> computeSchedule(DirectedAcyclicGraph<TestNode, DependencyEdge> graph, DependencyEdge curEdge) throws InvalidScheduleException;

}