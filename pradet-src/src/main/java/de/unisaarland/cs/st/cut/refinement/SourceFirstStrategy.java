package de.unisaarland.cs.st.cut.refinement;

import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;
import de.unisaarland.cs.st.cut.refinement.edgeselection.SimpleRandom;

@Deprecated
public class SourceFirstStrategy implements SelectionStrategy {

	private static final int MAX_OUT_DEGREE = 100;

	@Override
	public DependencyEdge selectEdge(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {

		for (int i = 1; i <= MAX_OUT_DEGREE; i++) {
			for (TestNode v : dependencyGraph.vertexSet()) {
				if (dependencyGraph.inDegreeOf(
						v) == 0/* && dependencyGraph.inDegreeOf(v) == i */) {
					// we have a sink with only one incoming edge
					Set<DependencyEdge> e = dependencyGraph.edgesOf(v);

					// if (e.size() != i)
					// throw new RuntimeException("SinkStrategy found " +
					// e.size() + " edges instead of" +i);

					// TODO maybe clever heuristic here?
					for (DependencyEdge edge : e) {
						if (!edge.isManifest()) {
							// Check if by reverting the edge we introduce a
							// cycle\

							// FIXME Pay attention that the .clone() does not
							// perform a deep copy. Check the
							// RandomSelectionStrategy2 for an example !
							DirectedAcyclicGraph<TestNode, DependencyEdge> copy = (DirectedAcyclicGraph<TestNode, DependencyEdge>) dependencyGraph
									.clone();
							TestNode src = copy.getEdgeSource(edge);
							TestNode tgt = copy.getEdgeTarget(edge);

							copy.removeEdge(edge);
							try {
								copy.addDagEdge(tgt, src);
								edge.setIntroducesCycle(false);

								System.out.println("Found spurce " + edge + " out degree of " + i);

								return edge;

							} catch (CycleFoundException ex) {
								System.out.println("skip problematic edge " + e);
								edge.setIntroducesCycle(true);
								// found = false;
							}
						}
					}
				}
			}
		}

		// No suitable sink found. Fallback: Random
		System.out.println("Didnt find a source. Using RandomStrategy...");
		return new SimpleRandom().selectEdge(dependencyGraph);
	}

}
