package de.unisaarland.cs.st.cut.refinement;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.junit.runner.RemoteJUnitCore;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import au.com.bytecode.opencsv.CSVReader;
import de.unisaarland.cs.st.cut.refinement.edgeselection.SimpleRandom;
import de.unisaarland.cs.st.cut.refinement.edgeselection.SourceFirst;
import de.unisaarland.cs.st.cut.refinement.scheduling.ComplexScheduler;
import de.unisaarland.cs.st.cut.refinement.scheduling.InvalidScheduleException;
import de.unisaarland.cs.st.cut.refinement.scheduling.OriginalOrderScheduler;
import de.unisaarland.cs.st.cut.refinement.scheduling.RefinementScheduler;

/**
 * The Double Check is a best effort, in fact it assumes that only 1 test set
 * the preconditions, which might be not ok. It a test requies two distinct sets
 * of test we shall find that out. This requires either a more fine grained
 * analysis (which deps are involved) or more executions. For the latter, one
 * shall try all the possible permutations of the common ancestors.
 * 
 * Another problem is that if at the same time two tests share the same
 * ancestor, and one depend one on another, we might have spurious MD.
 * 
 * 
 * Mixed schedules: this are not possible unless, the original test suite was
 * already parallelized.
 * 
 * EdgeSelection: to be sound-er we need to start from the back of a WCC and go
 * toward the source. Otherwise, write-after-write (which are not accounted for
 * in the DepList) might falsify tests preconditions. Dependency testing: in
 * case a test has multiple preconditions we must check (almost-exhaustively)
 * the combination of its' preconditions. We start with ISOLATE, then we move to
 * 1 dep at the time, then 2 etc. TODO: Not sure we can purge this when we
 * discover a "single" (or subset) of failing precondition... Maybe is not 100%
 * precise (we can not tell exactly which combination of preconditions cause the
 * test to fail, but at least we can expose the problem)
 * 
 * TODO: Why do we need the execution order to begin with? Once we have the
 * dependencies any order the fulfills all of them is fine, and why it shouldn't
 * be the case ?
 * 
 * @author gambi
 *
 */
public class DependencyRefiner {

	// TODO Promote as self standing class ? Check TestListener
	public enum TestResult {
		PASS, FAIL;
	}

	public static class DependencyEdge extends DefaultEdge {

		private boolean manifest;
		private boolean introducesCycle;
		private boolean ignored;

		public boolean underTest;

		public DependencyEdge() {
			super();
			manifest = false;
			introducesCycle = false;
		}

		public boolean isManifest() {
			return manifest;
		}

		public boolean isIgnored() {
			return ignored;
		}

		public void setIgnored(boolean ignored) {
			this.ignored = ignored;
		}

		public void setManifest(boolean manifest) {
			this.manifest = manifest;
		}

		public boolean isIntroducesCycle() {
			return introducesCycle;
		}

		public void setIntroducesCycle(boolean introducesCycle) {
			this.introducesCycle = introducesCycle;
		}

	}

	public static class TestNode {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestNode other = (TestNode) obj;
			if (id != other.id)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		public int id;
		public String name;

		@Override
		public String toString() {
			return name + "-" + id;
		}
	}

	private String applicationClasspath;
	private DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph;
	private Map<String, TestResult> reference;

	// Only for backward compatibility
	private List<TestNode> referenceOrder;

	// TODO Make this customizable
	private RefinementScheduler scheduler = null;

	private boolean allowParallelismAtMethodLevel = false;

	@Deprecated
	public DependencyRefiner() {

	}

	public DependencyRefiner(String applicationClasspath,
			DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph, Map<String, TestResult> expectedResults,
			List<TestNode> referenceOrder, String selectionStrategy, boolean showProgress, boolean runAll) {
		this.applicationClasspath = applicationClasspath;
		this.dependencyGraph = dependencyGraph;
		this.reference = expectedResults;
		this.referenceOrder = referenceOrder;
		scheduler = new OriginalOrderScheduler(this.referenceOrder);
		//
		setSelectionStrategy(selectionStrategy);
		//
		//
		this.showProgress = showProgress;
		if (showProgress) {
			System.out.println(
					"DependencyRefiner.DependencyRefiner() Write output to " + new File(".").getAbsolutePath());
		}
		this.RUNALL = runAll;

		allowParallelismAtMethodLevel = isMethodLevelParallel(referenceOrder);

	}

	public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
		try { // Move Parsing to external code
			ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);

			String refinementStrategy = cli.getRefinementStrategy();
			if (refinementStrategy == null) {
				refinementStrategy = System.getProperty("refinement.strategy", "source-first");
			}

			// ASSUMPTION: RUN ORDER and DEPS must AGREE ! You can build an
			// order using the execute script if that is missing...
			List<TestNode> referenceOrder = readTestsFromFile(cli.getRunOrder());

			DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph = buildGraph(referenceOrder,
					cli.getDependencies(), cli.isStrictMode());

			DependencyRefiner dr = new DependencyRefiner(//
					cli.getApplicationClasspath(), dependencyGraph, buildExpectedResults(cli.getReferenceOutput()),
					referenceOrder, refinementStrategy, cli.isShowProgress(), cli.isRunAll());

			dr.refine();
		} finally {
			// Somehow it hangs
			System.exit(0);
		}
	}

	public void processClasspath(String cp) {
		for (String cpEntry : cp.split(File.pathSeparator)) {
			try {
				URL url = (new File(cpEntry)).toURI().toURL();
				System.out.println("DependencyRefiner.main() Additional JAR " + url);
				URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				method.setAccessible(true);
				method.invoke(classLoader, url);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void setSelectionStrategy(final String selectionStrat) {

		switch (selectionStrat) {
		case "simple-random":
			System.out.println("Using Simple Random selection strategy ");
			selectionStrategy = new SimpleRandom();
			break;
		case "random":
			System.out.println("Using Random selection strategy ");
			selectionStrategy = new RandomSelectionStrategy2();
			System.out.print("DependencyRefiner.setSelectionStrategy() Overriding scheduler " + scheduler + " with ");
			scheduler = new ComplexScheduler();
			System.out.println(scheduler);
			break;
		case "sink-first":
			System.out.println("Using SinkFirstStrategy");
			selectionStrategy = new SinkFirstStrategy();
			break;
		case "source-first":
			System.out.println("Using SourceFirstStrategy");
			selectionStrategy = new SourceFirst();
			break;

		default:
			System.out.println("Unkown refinement strategy " + selectionStrat);
			// System.out.println("Using Simple Random selection strategy ");
			// selectionStrategy = new SimpleRandom();
			// This is the sound one
			System.out.println("Using SourceFirstStrategy");
			selectionStrategy = new SourceFirst();
			break;
		}
	}

	// TODO Add here an option to show the version of this class !!!

	public interface ParsingInterface {

		@Option(longName = { "application-classpath" })
		public String getApplicationClasspath();

		@Option(longName = { "refinement-strategy" }, defaultToNull = true)
		public String getRefinementStrategy();

		@Option(longName = { "run-all" })
		public boolean isRunAll();

		@Option(longName = { "show-progress" })
		public boolean isShowProgress();

		@Option(longName = { "run-order" }) // Mandatory
		public File getRunOrder();

		// Why is this optional ?
		@Option(longName = { "dependencies" }, defaultToNull = true)
		public File getDependencies();

		@Option(longName = { "reference-output" }, defaultToNull = true)
		public File getReferenceOutput();

		// Boolean values have no default values, either they are there or not
		@Option(longName = { "strict" })
		public boolean isStrictMode();

	}

	private static boolean debug = Boolean.getBoolean("debug");

	private SelectionStrategy selectionStrategy;
	public static boolean RUNALL = false;
	private File depFile;
	private File referenceOutput;
	private File runOrder;

	// TODO Convert this using command line arguments instead of system
	// properties
	@Deprecated
	private void setup(String... args) {

		ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);

		if (cli.getRefinementStrategy() != null) {
			setSelectionStrategy(cli.getRefinementStrategy());
		} else {
			setSelectionStrategy(System.getProperty("refinement.strategy", "simple-random"));
		}

		showProgress = cli.isShowProgress();

		// Missing or not specified
		RUNALL = cli.isRunAll();
		if (!RUNALL) {
			if (System.getProperties().containsKey("run-all")) {
				if (System.getProperty("run-all") == null) {
					RUNALL = true;
				} else {
					RUNALL = Boolean.getBoolean("run-all");
				}
			} else {
				RUNALL = false;
			}
		}
		System.out.println("Running all tests " + RUNALL);

		System.out.println("Loading graph...");

		depFile = cli.getDependencies();
		if (depFile == null) {
			depFile = new File(System.getProperty("dependencies", ""));
		}
		// TODO Rename the properties to reference.output
		referenceOutput = cli.getReferenceOutput();
		if (referenceOutput == null) {
			referenceOutput = new File(System.getProperty("expectedResults", ""));
		}

		runOrder = cli.getRunOrder();
		if (runOrder == null) {
			runOrder = new File(System.getProperty("run-order", ""));
		}

		if (runOrder == null) {
			System.out.println("DependencyRefiner.setup() Default run-order to reference-output");
			runOrder = referenceOutput;
		}

		if (!depFile.exists() || !referenceOutput.exists()) {
			throw new IllegalArgumentException("Can't read files " + depFile + " " + referenceOutput);
		}

		try {
			// This is wrong as it misses some test cases .... they might not
			// have dependencies but shall appear in the graph
			// referenceOrder = buildReferenceOrder(runOrder);
			dependencyGraph = buildGraph(runOrder, depFile, //
					cli.isStrictMode());

			reference = new HashMap<>();
			BufferedReader br = new BufferedReader(new FileReader(referenceOutput));
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				reference.put(split[0], split[1].equals("PASS") ? TestResult.PASS : TestResult.FAIL);
			}

			// System.out.println("Generating INITIAL dot file...");
			// writeDot("starting-graph.dot", dependencyGraph);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Make this TestNode instead of String
	public static Map<String, TestResult> buildExpectedResults(File referenceOutputFile) throws IOException {
		Map<String, TestResult> expectedOutput = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(referenceOutputFile));
		String line;
		while ((line = br.readLine()) != null) {
			String[] split = line.split(",");
			expectedOutput.put(split[0], split[1].equals("PASS") ? TestResult.PASS : TestResult.FAIL);
		}
		return expectedOutput;

	}

	// public static List<TestNode> buildReferenceOrder(File runOrderFile) {
	// // Adding nodes
	// List<TestNode> executionOrder = new ArrayList<TestNode>();
	// int index = 0;
	// try (CSVReader reader = new CSVReader(new FileReader(runOrderFile));) {
	// List<String[]> all = reader.readAll();
	// for (String[] values : all) {
	// //
	// if (debug) {
	// System.out.println("DependencyRefiner.buildReferenceOrder() add " +
	// values[0] + " as " + index);
	// }
	// TestNode tn = new TestNode();
	// tn.id = index;
	// tn.name = values[0];
	// //
	// executionOrder.add(tn);
	// //
	// index++;
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// return null;
	// }
	// return executionOrder;
	// }

	// This is only for backward compatibility
	public static List<TestNode> buildReferenceOrder(List<String> testList,
			DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {
		// Adding nodes
		List<TestNode> executionOrder = new ArrayList<TestNode>();
		TopologicalOrderIterator<TestNode, DependencyEdge> sortedNodes = new TopologicalOrderIterator<>(
				dependencyGraph);
		int id = 0;
		while (sortedNodes.hasNext()) {
			TestNode test = sortedNodes.next();
			test.id = id;
			if (debug) {
				System.out.println("DependencyRefiner.buildReferenceOrder() add " + test);
			}
			executionOrder.add(test);
			id++;
		}
		// Probably we need to reverse the topological order. The topologica
		// order goes from the source (the one needing the deps) to the target
		// (the one setting the deps)
		Collections.reverse(executionOrder);
		return executionOrder;
	}

	// This is only for backward compatibility
	public static List<TestNode> buildReferenceOrder(DirectedAcyclicGraph<TestNode, DependencyEdge> dependencyGraph) {
		// Adding nodes
		List<TestNode> executionOrder = new ArrayList<TestNode>();
		TopologicalOrderIterator<TestNode, DependencyEdge> sortedNodes = new TopologicalOrderIterator<>(
				dependencyGraph);
		while (sortedNodes.hasNext()) {
			TestNode test = sortedNodes.next();

			executionOrder.add(test);
		}
		// Probably we need to reverse the topological order. The topologica
		// order goes from the source (the one needing the deps) to the target
		// (the one setting the deps)
		Collections.reverse(executionOrder);
		// Now we need to update the id !
		int id = 0;
		for (TestNode test : executionOrder) {
			test.id = id;
			if (debug) {
				System.out.println("DependencyRefiner.buildReferenceOrder() Adding test " + test);
			}

			id++;
		}

		return executionOrder;
	}

	/*
	 * Check actual results against expected results and discover if the
	 * dependency is manifest, that is: changing the preconditions set by A test
	 * (target) to another (source) will cause the failure of the latter.
	 * 
	 * NOTE: Accumulating evidences that other data deps might be there is
	 * out-of-scope of this method.
	 * 
	 * TODO Check that the original Data Dep is NOT inverted !!!
	 */
	private boolean isManifestDependency(DependencyEdge dataDependencyUnderTest, Map<String, TestResult> results) {

		boolean isManifest = false;

		// Consider only the tests involved in the dependency under analysis
		// System.out.println("Checking if " +
		// dependencyGraph.getEdgeSource(dataDependencyUnderTest) + " --> "
		// + dependencyGraph.getEdgeTarget(dataDependencyUnderTest));

		System.out.println("Checking if " + dataDependencyUnderTest + " is Manifest");

		//
		String testWithViolatedPreconditions = dependencyGraph.getEdgeSource(dataDependencyUnderTest).name;
		String testThatSetThePreconditions = dependencyGraph.getEdgeTarget(dataDependencyUnderTest).name;

		// TODO Accumulate the result also for the other tests in the schedule,
		// as they might suggest
		// missed data dependencies ! Why this should be the case if we respect
		// all the preconditions ?!
		for (String s : results.keySet()) {
			if (testWithViolatedPreconditions.equals(s)) { // Source is the test
															// out of order !
				if (!results.get(s).equals(reference.get(s))) {
					System.out.println("\t * " + s + " Different (SOURCE )");
					isManifest = true;
				}
			} else if (testThatSetThePreconditions.equals(s)) {
				if (!results.get(s).equals(reference.get(s))) {
					System.out.println("\t * " + s + " Different (TARGET - SKIP)");
				}
			} else {
				if (!results.get(s).equals(reference.get(s))) {
					System.out.println("\t * " + s + " Different (OTHER - SKIP)");
				}
			}

		}

		// TODO Move the double check/additional check later !
		// boolean allSameDoubleCheck = true;
		// // TODO Extract to variable or parameter
		// if (performDoubleCheck) {
		// // // There might be spurious cases in which a test not related
		// // // to the dependency under test fails. This makes the whole
		// // // thing unstable, since dependencies are marked as manifest
		// // // when they are not.
		// // // See: write after write in crystal. So as safety net we
		// // // rule them out during refinement.
		//
		// if (!allSame) {
		// // TODO: THIS MIGHT TAKE LONG TIME ! What shall we double
		// // check?
		// System.out.println("DependencyRefiner.refine() Found potential
		// manifest dependency. Double check it");
		// // We need to perform another execution, this time wit
		// // original
		// // order, but isolated. If the result is again !allSame,
		// // then we
		// // keep the manifest dep, otherwise we mark this as spurious
		// // !
		// Map<String, TestResult> doubleCheckResults = new HashMap<String,
		// TestResult>();
		// doubleCheckResults.putAll(
		// doubleCheckManifestDependencyWithRemoteJUnitCore(dependencyGraph,
		// dataDependencyUnderTest));
		//
		// // TODO not sure how to include this in the stats
		// // System.out.println("Stopping at: " +
		// // (System.currentTimeMillis() - start));
		// // executionTime.add(System.currentTimeMillis() - start);
		// // The additional executions must be included in the actual
		// // one
		// // !
		// testCounts.add(results.size());
		//
		// System.out
		// .println("Double checking results of " +
		// dependencyGraph.getEdgeSource(dataDependencyUnderTest)
		// + " --> " + dependencyGraph.getEdgeTarget(dataDependencyUnderTest));
		//
		// String sourceDoubleCheck =
		// dependencyGraph.getEdgeSource(dataDependencyUnderTest).name;
		// String targetDoubleCheck =
		// dependencyGraph.getEdgeTarget(dataDependencyUnderTest).name;
		//
		// for (String s : doubleCheckResults.keySet()) {
		// if (sourceDoubleCheck.equals(s)) { // Source is the test
		// // out
		// // of order !
		// if (!doubleCheckResults.get(s).equals(reference.get(s))) {
		// allSameDoubleCheck = false;
		// System.out.println("\t * " + s + " Different (SOURCE) - DoubleCheck
		// ");
		// }
		// } else if (targetDoubleCheck.equals(s)) {
		// // TODO Shall we check explicitly for ANTI-DEPS or
		// // this
		// // is
		// // ok-ish?
		// if (!doubleCheckResults.get(s).equals(reference.get(s))) {
		// allSameDoubleCheck = false;
		// System.out.println("\t * " + s + " Different (TARGET) -
		// DoubleCheck");
		// }
		// } else {
		// if (!doubleCheckResults.get(s).equals(reference.get(s))) {
		// System.out.println("\t * " + s + " Different (SKIP) - DoubleCheck");
		// }
		// }
		//
		// }
		// return allSameDoubleCheck;
		// }
		// } else {
		return isManifest;
		// }
	}

	// Global Counters and Stats
	private static int iterationID = 0;
	private List<Integer> testCounts = new LinkedList<>();
	private List<Long> executionTime = new LinkedList<>();
	//
	boolean performDoubleCheck = false;
	private static int skipped = 0;

	public Set<DependencyEdge> refine() throws IOException, InterruptedException, ExecutionException {
		// This might be over-approximate because some dependencies cannot
		// be
		// tested with out approach - they will be reported as Untestable.
		System.out.println("DependencyRefiner.refine() TOTAL ITERATION COUNT: " + dataDependenciesCount);

		System.out.println("\n\n\n Iteration " + iterationID);
		// Select a data dependency to test - this must not introduce cycle

		DependencyEdge dataDependencyUnderTest = selectionStrategy.selectEdge(dependencyGraph);
		//
		System.out.println("DependencyRefiner.refine() Dependency Under Test " + dataDependencyUnderTest);
		// DependencyEdge (B -> A) means B requires A, so A must run before
		// B
		// such that B's preconditions are fulfilled.
		//
		// if (showProgress) {
		// writeDot(String.format("iteration_%03d.dot", iterationID),
		// dependencyGraph);
		// }

		while (dataDependencyUnderTest != null) {

			dataDependencyUnderTest.underTest = true;

			if (showProgress) {
				writeDot(String.format("iteration_%03d.dot", iterationID), dependencyGraph);
			}

			// Compute a schedule to validate/test the data dependency. This
			// raises an exception if the schedule cannot be found. For example,
			// random policy violates explicitly some conditions and uses ignore
			// to plan the schedule... however, OriginalOrderSchedule prevents
			// that by design.
			// So we simply SKIP this specific edge
			List<TestNode> schedule = null;

			try {
				schedule = scheduler.computeSchedule(dependencyGraph, dataDependencyUnderTest);

				if (Boolean.getBoolean("trace")) {
					System.out.println("Schedule is " + schedule);
				}

				// 4. Execute the tests
				long start = System.currentTimeMillis();
				System.out.println("Start test execution at: " + start);
				Map<String, TestResult> results = new HashMap<String, TestResult>();
				//
				results.putAll(executeTestsRemoteJUnitCore(schedule));
				System.out.println("Stop test execution at: " + (System.currentTimeMillis() - start));
				//
				executionTime.add(System.currentTimeMillis() - start);
				testCounts.add(results.size());

				// 5. Check if there is any difference compared with reference
				// output
				if (isManifestDependency(dataDependencyUnderTest, results)) {
					dataDependencyUnderTest.setManifest(true);
					// manifestDependenciesCount++;
					System.out.println("\n Found Manifest Dependency: " + dataDependencyUnderTest + " \n");
					// TODO Double check for Flakyness ?
					// Print schedule to file
					printScheduleToFile(String.format("manifest_dependency_at_iteration_%03d.schedule", iterationID),
							schedule);

				} else {
					/*
					 * FIXME THis is where we need to include additional
					 * constraints. FOR THE MOMENT WE DO NOT, and evaluate the
					 * basic strategy.
					 */

					// TODO This is an important point. If we decide to remove
					// this
					// edge, we
					// might get problems later:
					// Example: we have a -> b -> c -> d.
					// we remove b->c because it ac(b) did not show any manifest
					// dep
					// then we have a->b, c->d. At this point running c (or d)
					// might
					// fail since a was setting a precondition on c and d that
					// was
					// masked by aggregating things away.
					// so ideally one should remove
					// Removing from the DAG should not prevent tests to run !
					// Remove
					dataDependencyUnderTest.setManifest(false);
					if (!dependencyGraph.removeEdge(dataDependencyUnderTest)) {
						System.out.println(
								"DependencyRefiner.refine() WARNING Edged " + dataDependencyUnderTest + " NOT REMOVED");
					}

				}

				//
				dataDependencyUnderTest.underTest = false;
			} catch (InvalidScheduleException ise) {
				//
				System.out.println("DependencyRefiner.refine() Iteration SKIPPED ! " + dataDependencyUnderTest
						+ " creates a cycle !");
				// Not sure this is needed. If the schedule was not valid was
				// becayse of random.
				dataDependencyUnderTest.introducesCycle = true;
				// TODO How to handle progress !?
				skipped++;
			} catch (Throwable e) {
				System.out.println("DependencyRefiner.refine() Unhandled exception ! " + e);
				e.printStackTrace();
				throw e;

			}
			// Re-sample the "Updated" graph
			dataDependencyUnderTest = selectionStrategy.selectEdge(dependencyGraph);

			System.out.println("DependencyRefiner.refine() Dependency Under Test " + dataDependencyUnderTest);

			if (iterationID - skipped > dataDependenciesCount) {
				System.out.println("DependencyRefiner.refine() ERROR: refinement did not terminate !");
				System.out.println(
						"Iteration ID " + iterationID + " skipped " + skipped + " dataDeps " + dataDependenciesCount);

				throw new RuntimeException("Refinement did not terminate !");
			}

			iterationID++;

			System.out.println("\n\n\n Iteration " + iterationID);
		}

		///
		//
		//

		if (showProgress) {
			writeDot(String.format("iteration_%03d.dot", iterationID), dependencyGraph);
		}

		// FIXME: Why there should be redundant edges ?!
		// Set<DependencyEdge> toRemove = new HashSet<>();
		// for (DependencyEdge ee : dependencyGraph.edgeSet()) {
		// if (ee.isIntroducesCycle() && !ee.isManifest()) {
		// // dependencyGraph.removeEdge(ee);
		// // to prevent Concurrent modification exception
		// toRemove.add(ee);
		// System.out.println("Removing redundant edge " + ee);
		// }
		// }
		// dependencyGraph.removeAllEdges(toRemove);

		// System.out.println("Generating FINAL dot file...");
		// writeDot("final-graph.dot", dependencyGraph);

		exportGraph(dependencyGraph, new File("refined-deps.csv"));
		writeDot("refined-graph.dot", dependencyGraph);

		int totalTests = 0;
		for (int n : testCounts) {
			totalTests += n;
		}
		long totalExecTime = 0;
		for (long l : executionTime) {
			totalExecTime += l;
		}
		float totalFloat = totalExecTime / 1000;

		System.out.println("\n\n\nFinished after " + iterationID + " iterations");
		if (iterationID > 0) {
			System.out.println("Executed " + totalTests + " tests in total, avg.: " + (totalTests / iterationID));
			System.out.println("Spent " + totalFloat + " seconds executing tests, avg.: " + (totalFloat / iterationID)
					+ " per refinement step");
		}
		// EASE THE PARSING
		System.out.println("==================");
		System.out.println("TESTS:" + reference.size());
		System.out.println("DD:" + dataDependenciesCount);
		System.out.println("MD:" + manifestDependenciesCount);
		System.out.println("UD:" + untestableDependenciesCount);
		System.out.println("EXECUTIONS:" + totalTests);
		if (iterationID != 0) {
			System.out.println("AVG EXECUTIONS:" + (totalTests / iterationID));
		}
		System.out.println("MAX EXECUTIONS:" + reference.size() * dataDependenciesCount);
		System.out.println("TIME:" + totalFloat);
		if (iterationID != 0) {
			System.out.println("AVG TIME:" + (totalFloat / iterationID));
		}
		System.out.println("==================");

		// Extract the set of manifest deps !
		return dependencyGraph.edgeSet();

	}

	private boolean isMethodLevelParallel(List<TestNode> testList) {

		DirectedAcyclicGraph<String, DefaultEdge> DAG = new DirectedAcyclicGraph<String, DefaultEdge>(
				DefaultEdge.class);
		for (TestNode test : testList) {
			// Extract class name - and add the vertex
			String className = test.name.substring(0, test.name.lastIndexOf("."));
			DAG.addVertex(className);
		}

		// Try to add all the edges
		try {
			for (int i = 0; i < testList.size() - 1; i++) {
				String classNameSource = testList.get(i).name.substring(0, testList.get(i).name.lastIndexOf("."));
				String classNameTarget = testList.get(i + 1).name.substring(0,
						testList.get(i + 1).name.lastIndexOf("."));
				// Skip trivial self-loops
				if (classNameSource.equals(classNameTarget))
					continue;
				DAG.addDagEdge(classNameSource, classNameTarget);
			}
		} catch (CycleFoundException e) {
			System.out.println("RemoteJUnitCore.checkParallelism() Found cycle ! ");
			return true;
		}
		return false;
	}

	// Really in this experiments 1 is already OK
	// Before the initialization of the pool was INSIDE
	// remoteExecutionWithJUnitCore... BAD !
	private final static ExecutorService testExecutorThreadsPool = Executors.newFixedThreadPool(5);

	// TODO Shouldn't be the "server" always running and waiting for the
	// RemoteJUnitCore thingy to connect to the socket and send the results
	// back?

	// Refactor. Pool executor service and port and sockets !
	public static Entry<Integer, List<String>> remoteExecutionWithJUnitCore(//
			List<String> schedule, //
			String applicationClasspath, //
			List<String> additionalArgs,
			// Deprecated
			// boolean allowsParallelismAtMethodLevel, //
			boolean printToFile) throws IOException, InterruptedException, ExecutionException {

		try (ServerSocket server = new ServerSocket(0)) {
			final int port = server.getLocalPort();
			// server.accept() is Blocking So we need to use threads !
			// TODO Refactor to use Callable and return a Future<Result>
			// instead.
			// Use executor service !
			// https://blogs.oracle.com/CoreJavaTechTips/entry/get_netbeans_6

			// PROBABLY OPENING THE SOCKET SHALL BE DONE ONLY ONCE, THEN WE
			// SHOULD USE just accept
			Future<Entry<Integer, List<String>>> future = testExecutorThreadsPool
					.submit(new Callable<Entry<Integer, List<String>>>() {

						@Override
						public Entry<Integer, List<String>> call() throws Exception {

							try (Socket clientSocket = server.accept()) {
								if (debug) {
									System.out.println("RemoteJUnitCore.startServer() Server listening to " + port);
									System.out.println(
											"RemoteJUnitCore.startServer() RemoteJUnitCore join the execution. Waiting for the results ");
								}
								// This blocks
								PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
								ObjectInputStream in = new ObjectInputStream(
										new BufferedInputStream(clientSocket.getInputStream()));

								// To read the Result Object we need to resolve
								// the test
								// classes here as well...
								// Result testResult = (Result) in.readObject();
								// System.out.println("JUnitCore executed " +
								// testResult.getRunCount() + " ");
								// System.out.println("JUnitCore failed " +
								// testResult.getFailureCount() + " ");
								// System.out.println("JUnitCore ignored " +
								// testResult.getIgnoreCount() + " ");
								final Long runTime = (Long) in.readObject();
								final Integer runCount = (Integer) in.readObject();
								final Integer ignored = (Integer) in.readObject();
								final Integer failureCount = (Integer) in.readObject();
								// TODO Description requires the test class we
								// need only
								// the string of

								final List<String> failed = new ArrayList<>();

								for (int i = 0; i < failureCount; i++) {
									String failedTest = (String) in.readObject();
									String stackTrace = (String) in.readObject();
									if (debug) {
										System.out.println("DependencyRefiner.remoteExecutionWithJUnitCore() Test "
												+ failedTest + " Failed with stack trace:\n " + stackTrace);
									}
									failed.add(failedTest);
								}

								return new AbstractMap.SimpleEntry<Integer, List<String>>(runCount, failed);
							} catch (IOException | ClassNotFoundException e) {
								e.printStackTrace();
								return null;
							}
						}
					});

			// Probably we can check here if the port is already open and wait
			// otherwise... but without triggering the accept on the server side

			// TODO: Let's hope that between now and pb.start the server started
			// Prepare the invocation of the remote JUnitCore !
			String jvm_location;
			if (System.getProperty("os.name").startsWith("Win")) {
				jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
						+ "java.exe";
			} else {
				jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
						+ "java";
			}

			// Extract the JUNIT VERSION of the patched junit currently in use
			// String[] cpEntries =
			// System.getProperty("java.class.path").split(File.pathSeparator);
			// String patchedJUnitCpEntry = null;
			// for (String cpEntry : cpEntries) {
			// if (cpEntry.contains("PRADET") && cpEntry.contains("junit")) {
			// patchedJUnitCpEntry = cpEntry;
			// break;
			// }
			// }
			//
			// if (patchedJUnitCpEntry == null) {
			// throw new RuntimeException("Cannot find JUnit-PRADET on the
			// classpath");
			// }
			// Put THIS classpath in front. Note that we shade all the
			// dependencies but JUnit
			String classpath = System.getProperty("java.class.path") + File.pathSeparator + applicationClasspath;

			// File.pathSeparator;
			// String applicationClasspath =
			// System.getProperty("java.class.path");

			// Include additional propertis
			// -Djava.net.preferIPv4Stack=true

			// System.out.println("DependencyRefiner.remoteExecutionWithJUnitCore()
			// CLASSPATH " + classpath);
			List<String> args = new ArrayList<>();
			args.add(jvm_location);

			// TODO Not sure how this shall be included...
			args.add("-enableassertions");

			// We need to explicitly include method level ordering to force the
			// patched JUnit
			// StringBuilder referenceOrder = new StringBuilder();
			// for (String test : schedule) {
			// referenceOrder.append(test).append(",");
			// }
			//
			// // NOTE THIS ONE !!! Might be too long as well ?
			// // This one is for the JUnitCore sorting of test methods...
			// if (referenceOrder.length() > 0) {
			// referenceOrder.reverse().delete(0, 1).reverse();
			//
			// args.add("-Dreference-order=" + referenceOrder.toString());
			// }
			// MOVED DIRECTLY INSIDE RemoteJUnitCore !

			args.add("-cp");
			args.add(classpath);

			for (String additionalArg : additionalArgs) {
				if (additionalArg.trim().length() > 0)
					args.add(additionalArg);
			}

			// Enable Debug in Slave process if Debug is active in master
			// Process
			if (debug) {
				args.add("-Ddebug=true");
			}

			args.add(RemoteJUnitCore.class.getName());
			//
			if (printToFile) {
				args.add("--print-to-file");
			}

			// TODO Enable this one if you want to print out the execution data
			// or remote tests
			// if (showProress) {
			// args.add("--show-progress");
			// }
			//
			args.add("--port");
			args.add("" + port);
			// This is optional
			args.add("--iteration-id");
			args.add("" + iterationID);

			if (schedule.size() <= 100) {
				args.add("--test-list");
				args.addAll(schedule);
			} else {

				// We need to create a tmp file otherwise the JVM process will
				// not start, i.e., joda-time has 2K tests
				Path p = File.createTempFile("schedule", "" + iterationID).toPath();
				Files.write(p, schedule);
				args.add("--test-list-file");
				args.add(p.toAbsolutePath().toString());

			}
			//
			// if (allowsParallelismAtMethodLevel) {
			// args.add("--parallel");
			// }

			if (debug) {
				System.out.println("RemoteJUnitCore.testRemoteJUnitCore() Executing : " + args);
			}

			/// TODO Not sure why but empty arguments break the thing...

			// Note that we need probably to split the test string if more than
			// one
			// test is there
			ProcessBuilder processBuilder = new ProcessBuilder(args);
			// Probably remove this later ... This is OK, now we redirect test
			// execution output to file instead
			// if (debug ||
			// Boolean.getBoolean("show-output")) {
			// System.out
			// .println("DependencyRefiner.remoteExecutionWithJUnitCore()
			// Starting Remote Execution: " + args);
			// // For whatever strange reason if we disable this one the
			// // execution of LocalJUnitCore hangs ...
			// processBuilder.inheritIO();
			// }
			// For some strange reasong this breaks the code...
			processBuilder.inheritIO();
			// System.out.println("DependencyRefiner.remoteExecutionWithJUnitCore()"
			// + processBuilder.command());

			Process slaveJVM = processBuilder.start();

			// Wait for everything to finish ... ?
			int exitCode = slaveJVM.waitFor();
			if (exitCode != 0) {
				System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() ERROR !!");
				throw new RuntimeException("Remote test execution FAILED !!!");
			}

			return future.get();
		}
	}

	/*
	 * Spin off a new JVM with JUnitCore inside, this is faster than CUT but
	 * works only in the local VM ATM.
	 */
	private Map<String, TestResult> executeTestsRemoteJUnitCore(List<TestNode> theSchedule
	// DirectedAcyclicGraph<TestNode, DependencyEdge> graph,
	// DependencyEdge curEdge
	) throws IOException, InterruptedException, ExecutionException {

		////////
		List<String> schedule = new ArrayList<>();
		for (TestNode test : theSchedule) {
			schedule.add(test.name);
		}

		/////////
		List<String> additionalArgs = new ArrayList<String>();
		///
		File additionalArgsFile = new File(".additional-java-options");
		if (additionalArgsFile.exists()) {
			// Cannot handle args split by new lines
			for (String line : Files.readAllLines(Paths.get(additionalArgsFile.getPath()))) {
				for (String token : line.split(" "))
					if (token.trim().length() > 0)
						additionalArgs.add(token);
			}
		}
		/////////
		if (System.getenv().containsKey("EXTRA_JAVA_OPTS")) {
			additionalArgs.addAll(Arrays.asList(System.getenv().get("EXTRA_JAVA_OPTS").split(" ")));
		}

		Entry<Integer, List<String>> testResult = remoteExecutionWithJUnitCore(schedule, applicationClasspath,
				additionalArgs, /* allowParallelismAtMethodLevel, */false);

		if (testResult.getKey() != schedule.size()) {
			System.out.println("DependencyRefiner.executeTestsRemoteJUnitCore() ERROR TEST COUNT DOES NOT RUN !!! ");
			System.out.println(testResult.getKey() + " != " + schedule.size());

			//
			printScheduleToFile("error.schedule", theSchedule);

			throw new RuntimeException("Some tests did not run ! ");
		}

		Map<String, TestResult> ret = new HashMap<>();
		for (String test : schedule) {
			ret.put(test, TestResult.PASS);
		}

		for (String failed : testResult.getValue()) {
			ret.put(failed, TestResult.FAIL);

		}

		return ret;
	}

	// Variables for the execution
	private boolean showProgress = false;
	private static int dataDependenciesCount = 0;
	private int manifestDependenciesCount = 0;
	private int untestableDependenciesCount = 0;

	private void exportGraph(DirectedAcyclicGraph<TestNode, DependencyEdge> graph, File out) {

		try {
			PrintWriter pw = new PrintWriter(out);
			for (DependencyEdge e : graph.edgeSet()) {
				if (e.isManifest()) {
					pw.println(graph.getEdgeSource(e).name + "," + graph.getEdgeTarget(e).name);
					manifestDependenciesCount++;
				} else {
					pw.println(graph.getEdgeSource(e).name + "," + graph.getEdgeTarget(e).name + ",UNTESTED");
					untestableDependenciesCount++;
				}
			}
			pw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static List<TestNode> readTestsFromFile(File testListFile) throws FileNotFoundException, IOException {
		// Adding nodes
		// TODO Those must be sorted according to the run-order

		int i = 0;
		List<TestNode> testList = new ArrayList<>();

		// try (CSVReader reader = new CSVReader(new FileReader(testListFile));)
		// {
		//
		// List<String[]> all = reader.readAll();
		// // reader.close();
		// for (String[] values : all) {
		//
		// TestNode tn = new TestNode();
		// // We override this if needed
		// tn.id = i;
		// // FIXME If we encournte those we need to raise an exception,
		// // since we do not support parametrized
		// // Validate ALL input ! Use a matcher
		// tn.name = values[0].replaceAll("\\[.*\\]$", "");
		//
		// i++;
		//
		// testList.add(tn);
		// if (debug) {
		// System.out.println("DependencyRefiner.readTestsFromFile() Loaded Test
		// " + tn);
		// }
		// }
		// }
		List<String> lines = Files.readAllLines(testListFile.toPath());
		for (String value : lines) {
			TestNode tn = new TestNode();
			// // We override this if needed
			tn.id = i;
			tn.name = value;
			i++;
			//
			testList.add(tn);
			if (debug) {
				System.out.println("DependencyRefiner.readTestsFromFile() Loaded Test " + tn);
			}
		}

		return testList;
	}

	public static DirectedAcyclicGraph<TestNode, DependencyEdge> buildGraph(
			// Run Order is mostly the list of tests involved in the analysis
			// for the case deps contains more tests than the actual ones !
			File testListFile, //
			File deps, boolean strictMode) throws IOException {

		return buildGraph(readTestsFromFile(testListFile), deps, strictMode);

	}

	/**
	 * Build the data dependency graph. If B->A then B requires A, meaning A set
	 * preconditions to B and must run before it.
	 * 
	 * @param testList
	 * @param deps
	 * @param strictMode
	 * @return
	 * @throws IOException
	 */
	public static DirectedAcyclicGraph<TestNode, DependencyEdge> buildGraph(List<TestNode> testList, //
			File deps, boolean strictMode) throws IOException {

		DirectedAcyclicGraph<TestNode, DependencyEdge> ret = new DirectedAcyclicGraph<TestNode, DependencyEdge>(
				DependencyEdge.class);

		for (TestNode tn : testList) {
			if (ret.addVertex(tn)) {
				if (debug) {
					System.out.println("\t Adding test node " + tn);
				}
			} else {
				System.out.println("\t WARN Cannot add test node " + tn);
			}
		}

		// Adding edges -
		try (CSVReader reader = new CSVReader(new FileReader(deps));) {
			List<String[]> all = reader.readAll();
			// reader.close();
			for (String[] values : all) {
				TestNode source = null;
				TestNode target = null;
				try {

					// Clean and validate input... e.g. eliminate trailing [n]
					// entries
					// TODO Note that WE DO NOT STORE the input values
					for (TestNode t : ret.vertexSet()) {
						if (t.name.equals(values[0].//
								replaceAll("\\[.*\\]$", ""))) {
							source = t;
							continue;
						}
						if (t.name.equals(values[1].//
								replaceAll("\\[.*\\]$", ""))) {
							target = t;
							continue;
						}

						if (source != null && target != null) {
							break;
						}

					}
					//
					if (!strictMode && (source == null || target == null)) {
						System.out.println("DependencyRefiner.buildGraph(): WARNING: Problem with dependencies edge  "
								+ source + " --> " + target);
						// Skip this for the moment, log the warning
						break;
					}

					DependencyEdge e = ret.addEdge(source, target); // right
					if (debug) {
						System.out.println("\t Adding dep edge " + e);
					}

					// Source is the one which requires deps from target !
					if (source.id < target.id) {
						System.out.println(
								"DependencyRefiner.buildGraph(): WARNING: Dependencies data does not match run order data for "
										+ e);
					}

					dataDependenciesCount++;
				} catch (Throwable t) {
					boolean sourceMissing = false;
					boolean targetMissing = false;

					if (!ret.containsVertex(source)) {
						if (Boolean.getBoolean("trace")) {
							System.out
									.println("\t DependencyRefiner.buildGraph() Graph does not contains " + values[0]);
						}
						sourceMissing = true;
					}
					if (!ret.containsVertex(target)) {
						if (Boolean.getBoolean("trace")) {
							System.out
									.println("\t DependencyRefiner.buildGraph() Graph does not contains " + values[1]);
						}
						targetMissing = true;
					}

					// No matter what, if only one of the two deps is missing we
					// STOP with an error
					if (sourceMissing != targetMissing) {
						System.out.println("DependencyRefiner.buildGraph() Exit with error while adding: " + values[0]
								+ " --> " + values[1]);
						throw t;
					}

					// Unless strict mode, we skip entirely missing edges
					if (strictMode) {
						System.out.println("DependencyRefiner.buildGraph() Exit with error while adding: " + values[0]
								+ " --> " + values[1]);
						throw t;
					}
				}
			}
		} catch (IOException e) {
			throw e;
		}
		return ret;
	}

	// Label with parameter ?
	private String toDot(DirectedAcyclicGraph<TestNode, DependencyEdge> graph) {
		StringBuilder sb = new StringBuilder();

		sb.append("digraph {\n");
		for (DependencyEdge e : graph.edgeSet()) {
			sb.append("\t\"");
			sb.append(graph.getEdgeSource(e));
			sb.append("\" -> \"");
			sb.append(graph.getEdgeTarget(e));
			if (e.isManifest()) {
				sb.append("\" [color=red]\n");
			} else if (e.underTest) {
				sb.append("\" [color=green]\n");
			} else if (e.introducesCycle) {
				sb.append("\" [color=blue]\n");
			} else {
				sb.append("\";\n");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	public int DOT_MAX_SIZE = 100;

	private void writeDot(String path, DirectedAcyclicGraph<TestNode, DependencyEdge> graph) {
		try {
			// If the graph contains more than 100 nodes it is useless to
			// visualize !
			if (graph.vertexSet().size() > DOT_MAX_SIZE) {
				System.out.println(
						"DependencyRefiner.writeDot() Skip graph generation since number of nodes is larger than "
								+ DOT_MAX_SIZE);
			} else {

				PrintWriter pw = new PrintWriter(path);
				pw.print(toDot(graph));
				pw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void printScheduleToFile(String path, List<TestNode> schedule) {
		try {
			// If the graph contains more than 100 nodes it is useless to
			// visualize !
			PrintWriter pw = new PrintWriter(path);
			for (TestNode t : schedule)
				pw.println(t.name);
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
