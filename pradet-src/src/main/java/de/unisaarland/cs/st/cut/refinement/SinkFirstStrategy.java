package de.unisaarland.cs.st.cut.refinement;

import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;
import de.unisaarland.cs.st.cut.refinement.edgeselection.SimpleRandom;

public class SinkFirstStrategy implements SelectionStrategy {

	private static final int MAX_IN_DEGREE = 100;

	@Override
	public DependencyEdge selectEdge(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {

		for (int i = 1; i <= MAX_IN_DEGREE; i++) {
			// <<<<<<< HEAD
			// for (String v : dependencyGraph.vertexSet()) {
			// if (dependencyGraph.outDegreeOf(v) == 0/* &&
			// dependencyGraph.inDegreeOf(v) == i*/) {
			// // we have a sink with only one incoming edge
			// Set<DependencyEdge> e = dependencyGraph.edgesOf(v);
			//
			//// if (e.size() != i)
			//// throw new RuntimeException("SinkStrategy found " + e.size() + "
			// edges instead of" +i);
			//
			// //TODO maybe clever heuristic here?
			// for(DependencyEdge edge : e){
			// if(!edge.isManifest()){
			// // Check if by reverting the edge we introduce a cycle
			// DirectedAcyclicGraph<String, DependencyEdge> copy =
			// (DirectedAcyclicGraph<String, DependencyEdge>) dependencyGraph
			// .clone();
			// String src = copy.getEdgeSource(edge);
			// String tgt = copy.getEdgeTarget(edge);
			//
			// copy.removeEdge(edge);
			// try {
			// copy.addDagEdge(tgt, src);
			// edge.setIntroducesCycle(false);
			//
			// System.out.println("Found sink " + edge + " in degree of " + i);
			//
			// return edge;
			//
			// } catch (CycleFoundException ex) {
			// System.out.println("skip problematic edge " + e);
			// edge.setIntroducesCycle(true);
			// // found = false;
			// }
			// }
			// }
			// =======
			for (TestNode v : dependencyGraph.vertexSet()) {
				if (dependencyGraph.outDegreeOf(v) == 0 && dependencyGraph.inDegreeOf(v) == i) {
					// we have a sink with only one incoming edge
					Set<DependencyEdge> e = dependencyGraph.edgesOf(v);

					if (e.size() != i)
						throw new RuntimeException("SinkStrategy found " + e.size() + " edges instead of" + i);

					// TODO maybe clever heuristic here?
					DependencyEdge ret = e.iterator().next();
					System.out.println("Found sink " + ret + " in degree of " + i);
					return ret;
					// >>>>>>> fix-force-order
				}
			}
		}

		// No suitable sink found. Fallback: Random
		System.out.println("Didnt find a sink. Using RandomStrategy...");
		return new SimpleRandom().selectEdge(dependencyGraph);
	}

}
