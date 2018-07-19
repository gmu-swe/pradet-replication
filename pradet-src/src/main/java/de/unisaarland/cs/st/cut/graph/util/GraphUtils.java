package de.unisaarland.cs.st.cut.graph.util;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

public class GraphUtils {

	public static DirectedAcyclicGraph<TestNode, DependencyEdge> duplicate(
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
				DependencyEdge e = copy.addDagEdge(fromVertex, toVertex);
				// Update the attributes as well
				e.setIgnored(de.isIgnored());
				e.setIntroducesCycle(de.isIntroducesCycle());
				e.setManifest(de.isManifest());

			} catch (CycleFoundException e1) {
				throw new RuntimeException(e1);
			}
		}
		return copy;
	}
}
