package de.unisaarland.cs.st.cut.refinement;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

public interface SelectionStrategy {

	/**
	 * This must ensure that the returned edge will not create a cycle when
	 * inverted !
	 * 
	 * Returns either the selected edge or null when no more edges can be
	 * selected
	 * 
	 * @param dependencyGraph
	 * @return
	 */
	public DependencyEdge selectEdge(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph);
}
