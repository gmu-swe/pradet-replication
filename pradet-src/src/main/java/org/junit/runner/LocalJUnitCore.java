package org.junit.runner;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
 * This class wraps JUnitCore to execute the provided tests in order and publish
 * results using sockets using a listener.
 * 
 * 
 * TODO How to deal with org.junit.runner.manipulation.Filter ? If a test fail
 * in static constructor !
 */
public class LocalJUnitCore {

	final static boolean debug = Boolean.getBoolean("debug");

	private interface ParsingInterface {

		@Option(longName = { "test-list" }, defaultToNull = true)
		public List<String> getTestList();

		@Option(longName = { "test-list-file" }, defaultToNull = true)
		public File getTestListFile();

		@Option(longName = { "expected-results-file" }, defaultToNull = true)
		public File getExpectedResultsFile();

		@Option(longName = { "dependencies" }, defaultToNull = true)
		public File getDependenciesFile();

		@Option(longName = { "application-classpath" })
		public String getApplicationClassPath();

		// @Option(longName = { "parallel" })
		// public boolean isMethodLevelParallelization();

		@Option(longName = { "dry-run" })
		public boolean isDryRun();

		@Option(longName = { "output-suffix" }, defaultToNull = true)
		public String getOutputSuffix();

	}

	private static void addSoftwareLibrary(File file) throws Exception {
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
		method.setAccessible(true);
		method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { file.toURI().toURL() });
	}

	private static List<String> convertToStringList(List<TestNode> nodeList) {
		List<String> stringList = new ArrayList<String>();
		for (TestNode tn : nodeList) {
			stringList.add(tn.name);

		}
		return stringList;
	}

	// By default we exit with error if input is wrong !
	public static void main(String... args) {

		ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);

		try {

			List<TestNode> defaultRunOrder = new ArrayList<TestNode>();
			List<String> runOrder = new ArrayList<String>();
			List<TestNode> runOrderTest = new ArrayList<TestNode>();

			List<String> testNames = new ArrayList<>();
			if (cli.getTestList() != null) {
				testNames.addAll(cli.getTestList());
			} else if (cli.getTestListFile() != null) {
				testNames.addAll(Files.readAllLines(cli.getTestListFile().toPath()));
			} else {
				throw new RuntimeException("Provide either test-list or test-list-file!");
			}

			// Add all the nodes no matter what
			int i = 0;
			for (String testName : testNames) {

				if (testName.trim().length() == 0) {
					System.out.println("LocalJUnitCore.main() WARN: Empty test name !");
					continue;
				}

				TestNode tn = new TestNode();
				// We override this if needed
				tn.id = i;
				// FIXME If we encournte those we need to raise an
				// exception,
				// since we do not support parametrized
				// Validate ALL input ! Use a matcher
				tn.name = testName;

				i++;

				defaultRunOrder.add(tn);
			}

			if (cli.getDependenciesFile() != null) {
				// IDEALLY IF THERE IS NO NEED FOR THIS WE CAN SKIP IT. E.G. No
				// warnings about inconstitent data deps
				System.out.println("LocalJUnitCore.main() Rebuild run order using dependencies information");

				runOrderTest.addAll(DependencyRefiner.buildReferenceOrder(
						DependencyRefiner.buildGraph(defaultRunOrder, cli.getDependenciesFile(), false)));

				runOrder.addAll(convertToStringList(runOrderTest));

			} else {
				runOrder.addAll(convertToStringList(defaultRunOrder));
			}

			if (debug || cli.isDryRun()) {
				System.out.println("LocalJUnitCore.main() GENERATED RUN ORDER: ");
				for (String test : runOrder) {
					System.out.println("LocalJUnitCore.main() " + test);
				}
			}

			if (cli.isDryRun()) {
				System.out.println("LocalJUnitCore.main() Exit with DRY run ");
				// Check if the order is correct by rebuilding the grapth with
				// the new order
				DependencyRefiner.buildGraph(runOrderTest, cli.getDependenciesFile(), false);

				System.exit(0);
			}

			/////////

			/////////
			List<String> additionalArgs = new ArrayList<String>();
			///
			// File additionalArgsFile = new File(".additional-java-options");
			// if (additionalArgsFile.exists()) {
			// // Cannot handle args split by new lines
			// for (String line :
			// Files.readAllLines(Paths.get(additionalArgsFile.getPath()))) {
			// for (String token : line.split(" "))
			// if (token.trim().length() > 0)
			// additionalArgs.add(token);
			// }
			// }

			// Force to output to file
			if (cli.getOutputSuffix() != null) {
				// Sanitize the name ... Might be a little overconservative
				String sane = cli.getOutputSuffix().replaceAll("[^a-zA-Z0-9\\._]+", "_");
				additionalArgs.add("-Drun-order.file=run-order." + sane);
				additionalArgs.add("-Dreference-output.file=reference-output.csv." + sane);
			}

			// additionalArgs.add("-Drun-order.file=run-order." +
			// cli.getOutputSuffix());

			///////// Do not output run-order files during the execution
			boolean printToFileDuringExecution = false;
			if (debug) {
				System.out.println("LocalJUnitCore.main() DEBUG: Force print to File");
				printToFileDuringExecution = true;
			}

			if (System.getenv().containsKey("EXTRA_JAVA_OPTS")) {
				for (String a : Arrays.asList(System.getenv().get("EXTRA_JAVA_OPTS").trim().split(" "))) {
					if (a.trim().length() > 0)
						additionalArgs.add(a.trim());
				}
			}

			//
			Entry<Integer, List<String>> testResult = DependencyRefiner.remoteExecutionWithJUnitCore(runOrder,
					cli.getApplicationClassPath(), additionalArgs,
					//
					// false,// Deprecate
					printToFileDuringExecution);

			if (testResult.getKey() != runOrder.size()) {
				System.out
						.println("DependencyRefiner.executeTestsRemoteJUnitCore() ERROR TEST COUNT DOES NOT RUN !!! ");
				System.out.println(testResult.getKey() + " != " + runOrder.size());

				//
				DependencyRefiner.printScheduleToFile("error.schedule", defaultRunOrder);

				throw new RuntimeException("Some tests did not run ! ");
			}

			Map<String, TestResult> ret = new HashMap<>();
			for (String test : runOrder) {
				ret.put(test, TestResult.PASS);
			}

			for (String failed : testResult.getValue()) {
				ret.put(failed, TestResult.FAIL);

			}

			/// VERIFY
			boolean failed = false;
			if (cli.getExpectedResultsFile() != null) {
				for (String line : Files.readAllLines(cli.getExpectedResultsFile().toPath())) {
					String testN = line.split(",")[0];
					String testR = line.split(",")[1];

					// System.out.println("LocalJUnitCore.main() " + testN + "
					// -- " + testR);

					if (ret.containsKey(testN) && !ret.get(testN).equals(TestResult.valueOf(testR))) {
						System.out.println("LocalJUnitCore.main() DIFFERENT RESULTS FOR " + testN + " expected " + testR
								+ " but got " + ret.get(testN));
						failed = true;
					}
				}
			}

			if (failed) {
				System.exit(1);
			} else {
				System.exit(0);
			}
		} catch (Exception e) {
			System.out.println("CUTJUnitCore.main() Error");
			e.printStackTrace();
			System.exit(1);
		}
	}

}
