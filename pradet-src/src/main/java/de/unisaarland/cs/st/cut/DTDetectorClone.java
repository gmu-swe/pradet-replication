package de.unisaarland.cs.st.cut;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestNode;
import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.TestResult;

/**
 * This class re-implements part of DTDetector's approach, as it apparently
 * cannot correcly run JUnit3 Tests wich comes inside TestSuites. It execute
 * much more tests than it should...
 * 
 * @author gambi
 *
 */
public class DTDetectorClone {

	/*
	 * --analysis=isolate|reverse| --tests=${TEST_ORDER} \
	 * --report=./${ANALYSIS}.txt \ some options are not implemented:
	 * --analysis=combination --k=2\ --minimize=true 2>&1 | tee ${ANALYSIS}.log
	 * We provide the expected results from file, as well as the application
	 * clappath
	 */
	private interface ParsingInterface {

		@Option(longName = { "analysis" })
		public String getAnalysisName();

		// @Option(longName = { "k" }, defaultValue = "0")
		// public Integer getK();

		@Option(longName = { "tests" })
		public File getTestFile();

		@Option(longName = { "expected-results" })
		public File getExpectedResults();

		@Option(longName = { "report" })
		public File getReportFile();

		@Option(longName = { "application-classpath" })
		public String getApplicationClassPath();
	}

	private static List<String> convertToStringList(List<TestNode> nodeList) {
		List<String> stringList = new ArrayList<String>();
		for (TestNode tn : nodeList) {
			stringList.add(tn.name);

		}
		return stringList;
	}

	// Create a new job for eact test
	private static List<List<String>> buildIsolateJobs(List<String> testList) {
		List<List<String>> jobList = new ArrayList<List<String>>();
		for (String test : testList) {
			jobList.add(Arrays.asList(test));
		}
		return jobList;
	}

	// Create a single job which contains all the tests in reverse order
	private static List<List<String>> buildReverseJobs(List<String> testList) {
		List<List<String>> jobList = new ArrayList<List<String>>();
		// Add all
		List<String> reveseList = new ArrayList<String>(testList);
		// Reverse
		Collections.reverse(reveseList);
		jobList.add(reveseList);
		//
		return jobList;
	}

	public static void main(String... args) {

		ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);

		try {

			// List<TestNode> defaultRunOrder = new ArrayList<TestNode>();
			// List<TestNode> runOrderTest = new ArrayList<TestNode>();

			List<String> testNames = new ArrayList<>();
			testNames.addAll(Files.readAllLines(cli.getTestFile().toPath()));

			//
			List<String> additionalArgs = new ArrayList<String>();

			List<List<String>> jobs = new ArrayList<List<String>>();

			switch (cli.getAnalysisName()) {
			case "isolate":
				jobs.addAll(buildIsolateJobs(testNames));
				break;
			case "reverse":
				jobs.addAll(buildReverseJobs(testNames));
				break;
			default:
				throw new RuntimeException("Analysis not implemented " + cli.getAnalysisName());
				// break;
			}

			// Execution - note that in the results we care only about 1 single
			// tests for the moment...

			// Accumulate the results - NOTE that a this point we assume we run
			// one test only one time !!!
			Map<String, TestResult> results = new HashMap<>();

			for (List<String> job : jobs) {
				//
				List<String> runOrder = job;

				// Execute the tests in a slave JVM
				Entry<Integer, List<String>> testResult = DependencyRefiner.remoteExecutionWithJUnitCore(runOrder,
						cli.getApplicationClassPath(), additionalArgs, false // printToFileDuringExecution
				);

				// Check results
				if (testResult.getKey() != runOrder.size()) {
					System.out.println(
							"DependencyRefiner.executeTestsRemoteJUnitCore() ERROR TEST COUNT DOES NOT RUN !!! ");
					System.out.println(testResult.getKey() + " != " + runOrder.size());
					throw new RuntimeException("Some tests did not run ! ");
				}

				for (String test : runOrder) {
					results.put(test, TestResult.PASS);
				}

				for (String failed : testResult.getValue()) {
					results.put(failed, TestResult.FAIL);

				}
			}
			// Load expected results
			Map<String, TestResult> expectedResults = new HashMap<>();
			try (BufferedReader br = new BufferedReader(new FileReader(cli.getExpectedResults()))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] split = line.split(",");
					expectedResults.put(split[0], split[1].equals("PASS") ? TestResult.PASS : TestResult.FAIL);
				}

			}

			try (PrintWriter pw = new PrintWriter(cli.getReportFile())) {
				// Now compare the results with the expected results
				for (String testName : results.keySet()) {
					if (expectedResults.containsKey(testName)) {
						if (!expectedResults.get(testName).equals(results.get(testName))) {
							pw.println("Test: " + testName + " has a different results. We expected "
									+ expectedResults.get(testName) + " but got " + results.get(testName));
						}
					} else {
						throw new RuntimeException("Missing expected result data for " + testName);
					}
				}
			}

			System.exit(0);
		} catch (Exception e) {
			System.out.println("CUTJUnitCore.main() Error");
			e.printStackTrace();
			System.exit(1);
		}
	}

}
