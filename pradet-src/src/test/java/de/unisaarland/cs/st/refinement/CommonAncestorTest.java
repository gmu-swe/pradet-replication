package de.unisaarland.cs.st.refinement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.DependencyEdge;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;

/**
 * TODO/FIXME: This test is broken: it contains hardcoded path to crystal. This
 * this of tests must be places into configurable integration-test folders
 * 
 * @author gambi
 *
 */
@Ignore // See above
public class CommonAncestorTest {

	private void writeStringArrayToFile(String[] input, File output) throws FileNotFoundException {
		try (PrintStream ps = new PrintStream(new FileOutputStream(output))) {
			for (String s : input)
				ps.println(s);

		}
	}

	@Test
	public void verifyResults() throws IOException, InterruptedException, ExecutionException {
		String applicationClasspath = "/Users/gambi/projects/crystalvc/target/classes:/Users/gambi/projects/crystalvc/target/test-classes:/Users/gambi/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:/Users/gambi/.m2/repository/org/jdom/jdom/1.1/jdom-1.1.jar:/Users/gambi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Klapper/poldet/scripts_bash/projects/crystalvc/lib/hgKit-279.jar";
		List<String> schedule = new ArrayList<>();
		String test1 = "crystal.model.DataSourceTest.testToString";
		String test2 = "crystal.client.PreferencesGUIEditorFrameTest.testGetPreferencesGUIEditorFrameClientPreferences";
		// This order introduce the MD
		schedule.add(test1);
		schedule.add(test2);
		//
		/////////
		Entry<Integer, List<String>> testResult = DependencyRefiner.remoteExecutionWithJUnitCore(schedule,
				applicationClasspath, new ArrayList<String>(), /* true, */ false);
		// Both Fail - test 1 is different -so test 1 is manifest
		Assert.assertTrue(testResult.getValue().contains(test1));
		Assert.assertTrue(testResult.getValue().contains(test2));
		// Double check
		schedule.clear();
		// This is the original order, if it is manifest test1 will change
		// 'again'
		schedule.add(test2);
		schedule.add(test1);
		//
		testResult = DependencyRefiner.remoteExecutionWithJUnitCore(schedule, applicationClasspath,
				new ArrayList<String>(), /* true, */ false);
		// Both Fail - test 1 is different -so test 1 is manifest
		Assert.assertTrue(testResult.getValue().contains(test1));
		Assert.assertTrue(testResult.getValue().contains(test2));

	}

	/*
	 * Problem: Missing preconditions of tests introduce false manifest dep. A
	 * test fails because a precondition is missing, we need to find out which
	 * one is that. At the moment, we test one by one the ancestors
	 * (pre-condition) but this is not always ok, so we need to double check
	 * what we find out be repeating the same execution in the same order "in
	 * isolation"
	 */

	@Test
	public void testSpuriousDep1() throws IOException, InterruptedException, ExecutionException {
		// Crystal CP - Note that this is hardcoded to my machine !

		boolean showProgress = true;

		String applicationClasspath = "/Users/gambi/projects/crystalvc/target/classes:/Users/gambi/projects/crystalvc/target/test-classes:/Users/gambi/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:/Users/gambi/.m2/repository/org/jdom/jdom/1.1/jdom-1.1.jar:/Users/gambi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Klapper/poldet/scripts_bash/projects/crystalvc/lib/hgKit-279.jar";

		String[] depsList = new String[] {
				"crystal.model.DataSourceTest.testToString,crystal.model.DataSourceTest.testSetCloneString",
				"crystal.model.DataSourceTest.testToString,crystal.client.PreferencesGUIEditorFrameTest.testGetPreferencesGUIEditorFrameClientPreferences",
				"crystal.model.DataSourceTest.testToString,crystal.model.DataSourceTest.testSetField",
				"crystal.model.DataSourceTest.testSetCloneString,crystal.model.DataSourceTest.testSetField" };

		File deps = File.createTempFile("deps", ".csv");
		deps.deleteOnExit();
		writeStringArrayToFile(depsList, deps);

		String[] runOrder = new String[] {
				"crystal.client.PreferencesGUIEditorFrameTest.testGetPreferencesGUIEditorFrameClientPreferences",
				"crystal.model.DataSourceTest.testSetField", "crystal.model.DataSourceTest.testSetCloneString",
				"crystal.model.DataSourceTest.testToString" };
		File runOrderFile = File.createTempFile("run-order", ".csv");
		runOrderFile.deleteOnExit();
		writeStringArrayToFile(runOrder, runOrderFile);

		String[] expectedResults = new String[] { "crystal.model.DataSourceTest.testSetField,PASS",
				"crystal.model.DataSourceTest.testSetCloneString,PASS",
				"crystal.client.PreferencesGUIEditorFrameTest.testGetPreferencesGUIEditorFrameClientPreferences,FAIL",
				"crystal.model.DataSourceTest.testToString,PASS" };
		File referenceOutput = File.createTempFile("reference-output", ".csv");
		referenceOutput.deleteOnExit();
		writeStringArrayToFile(expectedResults, referenceOutput);
		// Do the refinement

		DirectedAcyclicGraph<TestNode, DependencyEdge> graph = DependencyRefiner.buildGraph(referenceOutput, deps,
				// Strict mode
				true);

		DependencyRefiner dependencyRefiner = new DependencyRefiner(//
				applicationClasspath, //
				graph, DependencyRefiner.buildExpectedResults(referenceOutput), //
				DependencyRefiner.buildReferenceOrder(graph), //
				"random", //
				showProgress, false);

		// TODO Which assertion can we make ?!
		Set<DependencyEdge> manifestDeps = dependencyRefiner.refine();
		Assert.assertEquals(3, manifestDeps.size());
	}

	@Test
	public void testSpuriousDep2() throws IOException, InterruptedException, ExecutionException {
		// Crystal CP - Note that this is hardcoded to my machine !

		boolean showProgress = true;

		String applicationClasspath = "/Users/gambi/projects/crystalvc/target/classes:/Users/gambi/projects/crystalvc/target/test-classes:/Users/gambi/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:/Users/gambi/.m2/repository/org/jdom/jdom/1.1/jdom-1.1.jar:/Users/gambi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Klapper/poldet/scripts_bash/projects/crystalvc/lib/hgKit-279.jar";

		String[] depsList = new String[] {
				"crystal.model.DataSourceTest.testToString,crystal.model.DataSourceTest.testSetCloneString",
				"crystal.model.DataSourceTest.testToString,crystal.model.DataSourceTest.testSetKind",
				"crystal.model.DataSourceTest.testToString,crystal.model.DataSourceTest.testSetField",
				"crystal.model.DataSourceTest.testSetKind,crystal.model.DataSourceTest.testSetField",
				"crystal.model.DataSourceTest.testSetCloneString,crystal.model.DataSourceTest.testSetField" };

		File deps = File.createTempFile("deps", ".csv");
		deps.deleteOnExit();
		writeStringArrayToFile(depsList, deps);

		String[] runOrder = new String[] { "crystal.model.DataSourceTest.testSetField", //
				"crystal.model.DataSourceTest.testSetKind", //
				"crystal.model.DataSourceTest.testSetCloneString", //
				"crystal.model.DataSourceTest.testToString"//
		};
		File runOrderFile = File.createTempFile("run-order", ".csv");
		runOrderFile.deleteOnExit();
		writeStringArrayToFile(runOrder, runOrderFile);

		String[] expectedResults = new String[] { "crystal.model.DataSourceTest.testSetField,PASS", //
				"crystal.model.DataSourceTest.testSetKind,PASS", //
				"crystal.model.DataSourceTest.testSetCloneString,PASS", //
				"crystal.model.DataSourceTest.testToString,PASS" //
		};
		File referenceOutput = File.createTempFile("reference-output", ".csv");
		referenceOutput.deleteOnExit();
		writeStringArrayToFile(expectedResults, referenceOutput);

		DirectedAcyclicGraph<TestNode, DependencyEdge> graph = DependencyRefiner.buildGraph(referenceOutput, deps,
				// Strict mode
				true);

		DependencyRefiner dependencyRefiner = new DependencyRefiner(//
				applicationClasspath, //
				graph, DependencyRefiner.buildExpectedResults(referenceOutput), //
				DependencyRefiner.buildReferenceOrder(graph), //
				"random", //
				showProgress, false);

		Set<DependencyEdge> manifestDeps = dependencyRefiner.refine();
		Assert.assertEquals(4, manifestDeps.size());
	}

}
