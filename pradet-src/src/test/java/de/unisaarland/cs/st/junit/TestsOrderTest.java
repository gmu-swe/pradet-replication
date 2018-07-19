package de.unisaarland.cs.st.junit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner;

public class TestsOrderTest {

	@Test
	public void testRunOrder1() {
		List<String> inputRunOrder = new ArrayList<String>();
		inputRunOrder.add("de.unisaarland.cs.st.cut.DefaultRunnerFailFail.defaultRunnerFailFail1");
		inputRunOrder.add("de.unisaarland.cs.st.cut.DefaultRunnerFailFail.defaultRunnerFailFail2");

		testRunOrdering(inputRunOrder);
	}

	//// THIS CANNOT BE ACHIEVEDED SO FAR !! We need to patch junit.4-12
	@Test
	public void testRunOrder2() {
		List<String> inputRunOrder = new ArrayList<String>();
		inputRunOrder.add("de.unisaarland.cs.st.cut.DefaultRunnerFailFail.defaultRunnerFailFail2");
		inputRunOrder.add("de.unisaarland.cs.st.cut.DefaultRunnerFailFail.defaultRunnerFailFail1");

		testRunOrdering(inputRunOrder);
	}

	@Test
	public void beforeAndAfterClassTest() {
		List<String> inputRunOrder = new ArrayList<String>();
		// This is encapsulated into 1st Request
		inputRunOrder.add("de.unisaarland.cs.st.junit.DummyTestWithBeforeClass.test_1");
		// This is encapsulated into 2nd Request
		inputRunOrder.add("de.unisaarland.cs.st.cut.DefaultRunnerFailFail.defaultRunnerFailFail1");
		// This is encapsulated into 3rd Request
		inputRunOrder.add("de.unisaarland.cs.st.junit.DummyTestWithBeforeClass.test_2");
		inputRunOrder.add("de.unisaarland.cs.st.junit.DummyTestWithBeforeClass.test_3");
		//
		testRunOrdering(inputRunOrder);

		// This is not working because the DummyTest class runs in a different
		// JVM... Assert.assertEquals(DummyTestWithBeforeClass.count, 2); But
		// from the logs I can see that before Class is invoked 2 times,
		// correctly !
	}

	private void testRunOrdering(List<String> inputRunOrder) {
		try {
			String applicationClasspath = "";
			boolean allowsParallelismAtMethodLevel = false;
			boolean printToFile = true;

			// Create a temp file Order
			File runOrderFile = File.createTempFile("test", "run-order");
			runOrderFile.deleteOnExit();
			System.out.println("TestsOrderTest.testRunOrdering() " + runOrderFile.getPath());
			//
			File referenceOutput = File.createTempFile("test", "reference-output.csv");
			referenceOutput.deleteOnExit();
			//
			List<String> additionalArgs = new ArrayList<String>();
			///
			additionalArgs.add("-Drun-order.file=" + runOrderFile.getPath());
			additionalArgs.add("-Dreference-output.file=" + referenceOutput.getPath());
			// The patched version of JUnit allows to reorder tests at method
			// level, but requires and additional System.Property to be set
			// reference-order. This is a comma-separated list of tests. Not
			// optimal.
			StringBuilder referenceOrder = new StringBuilder();
			for (String test : inputRunOrder) {
				referenceOrder.append(test).append(",");
			}
			if (referenceOrder.length() > 0) {
				referenceOrder.reverse().delete(0, 1).reverse();
				additionalArgs.add("-Dreference-order=" + referenceOrder.toString());
			}

			//
			System.setProperty("debug", "true");
			// This creates files

			Entry<Integer, List<String>> testResult = DependencyRefiner.remoteExecutionWithJUnitCore(//
					inputRunOrder, applicationClasspath, //
					additionalArgs, //
					/* allowsParallelismAtMethodLevel, */ printToFile);

			System.out.println("TestsOrderTest.testRunOrdering() TestResult " + testResult);

			// Read run-order and check that matches the one provided as input
			List<String> outputRunOrder = Files.readAllLines(runOrderFile.toPath());
			System.out.println("TestsOrderTest.testRunOrdering() " + outputRunOrder);

			for (int index = 0; index < inputRunOrder.size(); index++) {
				Assert.assertEquals(inputRunOrder.get(index), outputRunOrder.get(index));
			}

		} catch (IOException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
	}
}
