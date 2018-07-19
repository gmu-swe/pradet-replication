package org.junit.runner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.PradetSuite;
import org.junit.runners.model.InitializationError;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import de.unisaarland.cs.st.cut.junit.PrintOutCurrentTestRunListener;

/**
 * This class wraps JUnitCore to execute the provided tests in order and publish
 * results using sockets using a listener.
 * 
 * The execution goes as follows: - we check if the schedule is the result of an
 * already parallelized execution by checking that at the level of TestClasses
 * we have a dependency cycle. Or in other words, that we have situations like:
 * T1.m1, T(!=1).m*, T1.m(!=1). In this case we parallelize at method level.
 * 
 * Otherwise, we use a conservative soltion and parallelize at class leve.
 * 
 * 
 * TODO: Can we redirect test output to file ? TODO: Can we return the Failure
 * message ?
 */
public class RemoteJUnitCore {

	final static boolean debug = Boolean.getBoolean("debug");

	private interface ParsingInterface {

		@Option(longName = { "port" })
		public Integer getPort();

		@Option(longName = { "test-list" }, defaultToNull = true)
		public List<String> getTestList();

		@Option(longName = { "test-list-file" }, defaultToNull = true)
		public File getTestListFile();

		// Iteration ID - Optional
		@Option(longName = { "iteration-id" }, defaultToNull = true)
		public Integer getIterationID();

		@Option(longName = { "parallel" })
		public boolean isMethodLevelParallel();

		@Option(longName = { "print-to-file" })
		public boolean isPrintToFileDuringExecution();

		// Print out run-order and reference-output for the run
		@Option(longName = { "show-progress" })
		public boolean isShowProgress();

	}

	/**
	 * Build a lazy iterator which generates Request objects on the fly.
	 * Generating all of them before does not work because the methods annotated
	 * with @BeforeClass trigger for each request. This usually breaks the tests
	 * since for the same class the method is supposed to be called only once.
	 * 
	 * Additionally, we merge consecutive tests which belong to the same test
	 * class together. Note that this work only if coupled with a JUnitCore that
	 * calls run for each new request.
	 * 
	 * @param testList
	 * @return
	 * @throws InitializationError
	 */

	// Use the lazy iterator here = This is subsumed by the other, with the
	// difference that core.run is called multiple time
	public static Iterator<Request> buildParallelRequest(List<String> testList) throws InitializationError {
		System.out.println("RemoteJUnitCore.buildParallelRequest()");
		List<Runner> runners = new ArrayList<>();
		//
		// final List<Integer> staticallyFailedTests = new ArrayList<Integer>();
		// final Map<Integer, String> staticallyFailedTestTraces = new
		// HashMap<Integer, String>();
		//
		// List<String> expectedTests = new ArrayList<String>();
		for (String test : testList) {
			String className = test.substring(0, test.lastIndexOf("."));
			String methodName = test.substring(test.lastIndexOf(".") + 1);

			if (debug) {
				System.out.println("CUTJUnitCore.main() Adding runner for " + className + " " + methodName);
			}
			try {
				runners.add(Request.method(Class.forName(className), methodName).getRunner());
				// Register the name
				// expectedTests.add(className + "." + methodName);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Test class " + className + " does not exist ?!", e);
			}
		}

		System.out.println("RemoteJUnitCore.buildParallelRequest() TOTAL TESTS " + runners.size());
		// Not sure this is safe !
		List<Request> r = new ArrayList<Request>();
		r.add(Request.runner(new PradetSuite((Class<?>) null, runners)));
		return r.iterator();

	}

	// This must create 1 request for each class, note that this might result in
	// multiple tests run not just one !
	// Ideally we can merge them into a single request as long as there are no
	// two - non consecutive - appeareance of the same class. TODO Check if the
	// classes provided to Request are processed in the same order, and if
	// getRunner is called right away or only at "the right" time.
	public static Iterator<Request> buildPerClassRequest(final List<String> testList) throws InitializationError {

		Iterator<Request> lazyIterator = new Iterator<Request>() {

			private int index = 0;

			@Override
			public Request next() {

				// System.out.println("RemoteJUnitCore.buildPerClassRequest() -
				// " + index);
				String className = testList.get(index).substring(0, testList.get(index).lastIndexOf("."));
				Class testClass;
				try {
					// System.out.println("Loading class: " + className);
					// // This create problems ?!
					testClass = Class.forName(className);
					// if (!testClasses.contains(testClass))
					// testClasses.add(testClass);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);

				}

				final List<String> filteredTestList = new ArrayList<String>();
				// Add the first test to the list of the to run
				filteredTestList.add(testList.get(index));

				// We need to rebuild the filter for each of them !
				// Increment the index up to the very last test of this class
				index++;
				while (index < testList.size()
						&& testList.get(index).substring(0, testList.get(index).lastIndexOf(".")).equals(className)) {
					// Add the any test in this class to the list of the to run
					filteredTestList.add(testList.get(index));
					index++;
				}

				// System.out.println("next() index is " + index + " test to run
				// " + filteredTestList.size());
				// The only test which can run in this request are between the
				// index

				// Why this does not call @BeforeClass at every run ?!
				Request r = Request.aClass(testClass)//
						.filterWith(new Filter() {
							// Run the test only if belongs to
							// referenceOrder or its a
							// suite method (getMethodName == null )
							@Override
							public boolean shouldRun(Description description) {
								if (description.getMethodName() == null || filteredTestList
										.contains(description.getClassName() + "." + description.getMethodName())) {
									return true;
								}
								return false;
							}

							@Override
							public String describe() {
								return null;
							}
						});

				return r;
			}

			@Override
			public boolean hasNext() {
				return index < testList.size();
			}

		};

		return lazyIterator;

	}

	// By default we exit with error if input is wrong !
	public static void main(String... args) {

		ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);
		try {

			try (Socket socket = new Socket("localhost", cli.getPort());
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(
							new BufferedOutputStream(socket.getOutputStream()))) {

				JUnitCore core = new JUnitCore();

				// String logFile = "test-output-iteration-" +
				// cli.getIterationID() + ".log";
				// final PrintStream outputStream = (cli.getIterationID() ==
				// null) ? System.out : new PrintStream(logFile);

				// if (debug) {
				// RunListener listener = new TextListener(new JUnitSystem() {
				//
				// @Override
				// public PrintStream out() {
				// return outputStream;
				// }
				//
				// // @Override
				// public void exit(int code) {
				// // Do not exit the JVM on exit !
				//
				// }
				// });
				// core.addListener(listener);
				// }

				List<String> testList = new ArrayList<>();
				if (cli.getTestList() != null) {
					testList.addAll(cli.getTestList());
				} else if (cli.getTestListFile() != null) {
					testList.addAll(Files.readAllLines(cli.getTestListFile().toPath()));

				} else {
					throw new RuntimeException("Please specify the test list");
				}

				Iterator<Request> jUnitRequests = null;
				if (cli.isMethodLevelParallel()) {
					jUnitRequests = buildParallelRequest(testList);
				} else {
					jUnitRequests = buildPerClassRequest(testList);
				}

				// We need to explicitly include method level ordering to force
				// the
				// patched JUnit
				StringBuilder referenceOrder = new StringBuilder();
				for (String test : testList) {
					referenceOrder.append(test).append(",");
				}
				// TODO Might be this too long as well ?
				if (referenceOrder.length() > 0) {
					referenceOrder.reverse().delete(0, 1).reverse();
					System.setProperty("reference-order", referenceOrder.toString());
				}

				// C'e' solo se settato e non sempre
				List<String> expectedTests = new ArrayList<String>(testList);

				// Add Statically Failing Test listener
				final List<Integer> staticallyFailedTests = new ArrayList<Integer>();
				final List<Failure> assumptionFailedTests = new ArrayList<Failure>();
				final Map<Integer, String> staticallyFailedTestTraces = new HashMap<Integer, String>();

				RunListener staticallyFailingTestsListener = new RunListener() {

					Set<Description> runOrder = new HashSet<Description>();

					private int testID = 0;

					@Test
					@Override
					public void testStarted(Description description) throws Exception {
						// Descriptions are unique per test execution
						runOrder.add(description);
						testID++;
					}

					// Somehow this does not triggered in standard JUnit Core a
					// testFailuer ?!
					@Test
					@Override
					public void testAssumptionFailure(Failure failure) {
						if (runOrder.contains(failure.getDescription())) {
							System.out.println("Patch testAssumptionFailure()");
							assumptionFailedTests.add(failure);
						}
						super.testAssumptionFailure(failure);
					}

					@Test
					@Override
					public void testFailure(Failure failure) throws Exception {
						if (!runOrder.contains(failure.getDescription())
								&& failure.getDescription().getMethodName() == null) {
							staticallyFailedTests.add(testID);
							staticallyFailedTestTraces.put(testID, failure.getTrace());
							testID++; // Start is not invoked, we need to force
										// this
						}
					}

				};

				core.addListener(staticallyFailingTestsListener);

				// Some useful message - Test count might be wrong if testList
				// contains non-runnable tests
				System.out.println("RemoteJUnitCore.main() Tests to execute " + testList.size() + " ");
				// if (cli.getIterationID() != null) {
				// System.out.println("RemoteJUnitCore.main() Output redirected
				// to " + logFile + " ");
				// } else {
				// System.out.println("RemoteJUnitCore.main() Output redirected
				// to StdOut ");
				// }

				/*
				 * https://stackoverflow.com/questions/41261889/is-there-a- way-
				 * to-disable-org-junit-runner-junitcores-stdout-output Result
				 * result = new JUnitCore().runMain(new NoopPrintStreamSystem(),
				 * args); System.exit(result.wasSuccessful() ? 0 : 1);
				 */
				System.out.println("CUTJUnitCore.main() Start of test execution ");

				long startTime = System.currentTimeMillis();

				PrintOutCurrentTestRunListener printToFilelistener = new PrintOutCurrentTestRunListener(
						cli.isPrintToFileDuringExecution());

				core.addListener(printToFilelistener);

				// TODO: Check if the problem is when the request is created,
				// maybe we need to defer the request creation till the very
				// last second (stream?)
				List<Result> results = new ArrayList<Result>();

				while (jUnitRequests.hasNext()) {
					// Use the lazy iterator
					Result r = core.run(jUnitRequests.next());
					results.add(r);
					// System.out.println("RemoteJUnitCore.main() " + r);
				}
				long executionTime = System.currentTimeMillis() - startTime;

				System.out.println("RemoteJUnitCore.main() End of test execution ");
				System.out.println("RemoteJUnitCore.main() Test execution took: " + executionTime);

				// Merge Results:
				int runCount = 0;
				int failureCount = 0;
				int ignoreCount = 0;
				long runTime = 0;
				List<Failure> failures = new ArrayList<Failure>();
				//
				for (Result result : results) {
					runCount = runCount + result.getRunCount();
					failureCount = failureCount + result.getFailureCount();
					ignoreCount = ignoreCount + result.getIgnoreCount();
					runTime = runTime + result.getRunTime();
					failures.addAll(result.getFailures());
				}

				System.out.println("RemoteJUnitCore.main() Run " + runCount + " + " + staticallyFailedTests.size());
				System.out.println(
						"RemoteJUnitCore.main() Failed " + failureCount + " + " + assumptionFailedTests.size());
				System.out.println("RemoteJUnitCore.main() Ignored " + ignoreCount);

				if (cli.isShowProgress()) {
					// we always store to file at the very end
					printToFilelistener.storeResultsToFile();
				}

				// We need to remove the reference to test classes here, just
				// names and test method shall remain

				// We need only to pass the list of failed for the moment
				// objectOutputStream.writeObject(result);
				objectOutputStream.writeObject(new Long(runTime));
				// Patch to include statically failing tests
				objectOutputStream.writeObject(new Integer(runCount + staticallyFailedTests.size()));
				//
				objectOutputStream.writeObject(new Integer(ignoreCount));
				// Failure count includes already staticallyFailingTests but not
				// assumption failed tests
				objectOutputStream.writeObject(new Integer(failureCount + assumptionFailedTests.size()));
				// To deserialize Description we need the actual class that we
				// do not have on the otherside... so we
				// pass the string version of it !

				// TODO THis might be tricky
				// NOTE That we do not have this data for statically failing
				// tests, so we need to patch that in...
				// However, we rely on the sequential nature of the execution
				for (Failure f : failures) {
					// Skip Statically Failing tests
					if (f.getDescription().getMethodName() == null)
						continue;
					objectOutputStream
							.writeObject(f.getDescription().getClassName() + "." + f.getDescription().getMethodName());
					// Write also WHY the test failed ?
					objectOutputStream.writeObject(f.getTrace());
				}
				for (Failure f : assumptionFailedTests) {
					// Skip Statically Failing tests
					if (f.getDescription().getMethodName() == null)
						continue;
					objectOutputStream
							.writeObject(f.getDescription().getClassName() + "." + f.getDescription().getMethodName());
					// Write also WHY the test failed ?
					objectOutputStream.writeObject(f.getTrace());
				}
				// Public the Others
				for (Integer i : staticallyFailedTests) {
					objectOutputStream.writeObject(expectedTests.get(i));
					// Write also WHY the test failed ?
					objectOutputStream.writeObject(staticallyFailedTestTraces.get(i));
				}

				objectOutputStream.flush();

			} // This will close stream and socke

		} catch (InstantiationError e) {
			System.out.println("CUTJUnitCore.main() Instantiation Error");
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

}
