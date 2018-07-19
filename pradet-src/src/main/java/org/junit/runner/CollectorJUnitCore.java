package org.junit.runner;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.InitializationError;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import de.unisaarland.cs.st.cut.junit.PrintOutCurrentTestRunListener;
import edu.gmu.swe.datadep.HeapWalker;
import edu.gmu.swe.datadep.StaticFieldDependency;
import edu.gmu.swe.datadep.inst.DependencyTrackingClassVisitor;

/**
 * This class runs the tests in the specific order using an instrumented JVM. It
 * also uses HeapWalker to capture the datadeps among test executions.
 */
public class CollectorJUnitCore {

	final static boolean debug = Boolean.getBoolean("debug");

	static {
		if (debug) {
			System.out.println("CollectorJUnitCore Enabling DEBUG ");
			// DependencyTrackingClassVisitor.fieldsLogged.add(Pattern.compile(".*"));
			// DependencyTrackingClassVisitor.fieldsLogged.add(Pattern.compile("crystal.*"));
			// DependencyTrackingClassVisitor.fieldsLogged.add(Pattern.compile("crystal.model.DataSourceTest.data"));
			// This load the whitelist from -Dwhitelist=...

			if (System.getProperty("debug-file") != null) {
				try {
					for (String pattern : Files.readAllLines(new File(System.getProperty("debug-file")).toPath())) {
						DependencyTrackingClassVisitor.fieldsLogged.add(Pattern.compile(pattern));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//
		HeapWalker.loadWhitelist();
	}

	private interface ParsingInterface {

		@Option(longName = { "port" })
		public Integer getPort();

		@Option(longName = { "test-list" })
		public List<String> getTestList();

	}

	// This is ok up to the point tests do not do black magic and such...
	public static Iterator<Entry<Request, String>> buildParallelRequest(final List<String> testList)
			throws InitializationError {

		Iterator<Entry<Request, String>> lazyIterator = new Iterator<Entry<Request, String>>() {

			private int index = 0;

			@Override
			public Entry<Request, String> next() {
				String test = testList.get(index);
				//
				String className = test.substring(0, test.lastIndexOf("."));
				String methodName = test.substring(test.lastIndexOf(".") + 1);
				// TODO This might be problematic because we repeateably load
				// the class?
				Entry<Request, String> entry;
				try {
					entry = new AbstractMap.SimpleEntry(Request.method(Class.forName(className), methodName), test);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				index++;
				return entry;
			}

			@Override
			public boolean hasNext() {
				return index < testList.size();
			}

		};

		return lazyIterator;

	}

	// In order to use this one we might need to adopt a different strategy
	// since we need to capture the moment tests start (the request for a test
	// start!) and tests end...

	// /*
	// * TODO. Do not use this for moment. Problem is with statically failing
	// * tests... Use a Lazy iterator but limit the number of requests to the
	// * minimum.
	// */
	// public static Iterator<Request> buildPerClassRequest(final List<String>
	// testList) throws InitializationError {
	//
	// Iterator<Request> lazyIterator = new Iterator<Request>() {
	//
	// private int index = 0;
	//
	// @Override
	// public Request next() {
	//
	// System.out.println("RemoteJUnitCore.buildPerClassRequest() - " + index);
	// String className = testList.get(index).substring(0,
	// testList.get(index).lastIndexOf("."));
	// Class testClass;
	// try {
	// System.out.println("Loading class: " + className);
	// // // This create problems ?!
	// testClass = Class.forName(className);
	// // if (!testClasses.contains(testClass))
	// // testClasses.add(testClass);
	// } catch (ClassNotFoundException e) {
	// throw new RuntimeException(e);
	//
	// }
	//
	// final List<String> filteredTestList = new ArrayList<String>();
	// // Add the first test to the list of the to run
	// filteredTestList.add(testList.get(index));
	//
	// // We need to rebuild the filter for each of them !
	// // Increment the index up to the very last test of this class
	// index++;
	// while (index < testList.size()
	// && testList.get(index).substring(0,
	// testList.get(index).lastIndexOf(".")).equals(className)) {
	// // Add the any test in this class to the list of the to run
	// filteredTestList.add(testList.get(index));
	// index++;
	// }
	// System.out.println("next() index is " + index + " test to run " +
	// filteredTestList.size());
	// // The only test which can run in this request are between the
	// // index
	//
	// // Why this does not call @BeforeClass at every run ?!
	// Request r = Request.aClass(testClass)//
	// .filterWith(new Filter() {
	// @Override
	// public boolean shouldRun(Description description) {
	// if (description.getMethodName() == null || filteredTestList
	// .contains(description.getClassName() + "." +
	// description.getMethodName())) {
	// return true;
	// }
	// return false;
	// }
	//
	// @Override
	// public String describe() {
	// return null;
	// }
	// });
	//
	// return r;
	// }
	//
	// @Override
	// public boolean hasNext() {
	// return index < testList.size();
	// }
	//
	// };
	//
	// return lazyIterator;
	//
	// }

	// By default we exit with error if input is wrong !
	public static void main(String... args) {

		ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);

		try {

			try (Socket socket = new Socket("localhost", cli.getPort());
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(
							new BufferedOutputStream(socket.getOutputStream()))) {

				JUnitCore core = new JUnitCore();

				Iterator<Entry<Request, String>> jUnitRequests = buildParallelRequest(cli.getTestList());

				// C'e' solo se settato e non sempre
				List<String> expectedTests = new ArrayList<String>(cli.getTestList());

				// Add Statically Failing Test listener
				final List<Integer> staticallyFailedTests = new ArrayList<Integer>();
				final List<Failure> assumptionFailedTests = new ArrayList<Failure>();
				final Map<Integer, String> staticallyFailedTestTraces = new HashMap<Integer, String>();

				RunListener staticallyFailingTestsListener = new RunListener() {

					Set<Description> runOrder = new HashSet<Description>();

					private int testID = 0;

					@Override
					public void testStarted(Description description) throws Exception {
						// Descriptions are unique per test execution
						runOrder.add(description);
						testID++;
					}

					// Somehow this does not triggered in standard JUnit Core a
					// testFailuer ?!
					@Override
					public void testAssumptionFailure(Failure failure) {
						if (runOrder.contains(failure.getDescription())) {
							assumptionFailedTests.add(failure);
						}
						super.testAssumptionFailure(failure);
					}

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

				/*
				 * https://stackoverflow.com/questions/41261889/is-there-a- way-
				 * to-disable-org-junit-runner-junitcores-stdout-output Result
				 * result = new JUnitCore().runMain(new NoopPrintStreamSystem(),
				 * args); System.exit(result.wasSuccessful() ? 0 : 1);
				 */
				long startTime = System.currentTimeMillis();

				// We need this anyway to generate run-order and
				// reference-output.csv
				PrintOutCurrentTestRunListener printToFilelistener = new PrintOutCurrentTestRunListener(false);
				core.addListener(printToFilelistener);

				// RunListener hw = new RunListener() {
				// @Override
				// public void testFinished(Description description) throws
				// Exception {
				// super.testFinished(description);
				// HeapWalker.walkAndFindDependencies(description.getClassName(),
				// description.getMethodName());
				// }
				// };
				//
				// core.addListener(hw);

				List<Result> results = new ArrayList<Result>();
				Map<String, List<StaticFieldDependency>> deps = new HashMap<>();

				while (jUnitRequests.hasNext()) {
					// Use the lazy iterator - If we can assume there's a single
					// request per test
					// we can run HeapWalker directly at this stage, no need for
					// an additional
					Entry<Request, String> testBundle = jUnitRequests.next();
					//
					System.out.println("\n\n==================================\n" + "Starting " + testBundle.getValue()
							+ "\n" + "==================================\n");

					Result r = core.run(testBundle.getKey());

					System.out.println("\n\n==================================\n" + "Finished " + testBundle.getValue()
							+ "\n" + "==================================\n");

					results.add(r);
					//
					String test = testBundle.getValue();
					String className = test.substring(0, test.lastIndexOf("."));
					String methodName = test.substring(test.lastIndexOf(".") + 1);

					// FIXME: Since probably here we get back tons of XML it is
					// better to extract the relevant information right away,
					// and store that !

					// TODO Move the logic to extract deps here !!
					//
					List<StaticFieldDependency> datadeps = HeapWalker.walkAndFindDependencies(className, methodName);
					// Register the end of this test execution
					deps.put(test, datadeps);
				}
				long executionTime = System.currentTimeMillis() - startTime;

				System.out.println("CollectorJUnitCore.main() End of test execution ");
				System.out.println("CollectorJUnitCore.main() Test execution took: " + executionTime);
				////// Write to file the results

				// This writes run-order and reference-output.csv
				printToFilelistener.storeResultsToFile();

				// This writes deps.csv

				// Refactor this part;

				List<String> depsData = new ArrayList<String>();

				String path = System.getProperty("dep-output", "deps.csv");
				System.out.println(">>> SAVING TO FILE dependencies to " + path + "...");
				BufferedWriter bw;

				try {
					bw = new BufferedWriter(new FileWriter(new File(path)));
					// Force an ascending orders on the keys
					List<String> orderedKeys = new ArrayList<String>(deps.keySet());
					Collections.sort(orderedKeys);
					//
					for (String s : orderedKeys) {
						Set<String> cur = new HashSet<>();
						for (StaticFieldDependency sfd : deps.get(s)) {
							// TODO check sfd.depGen
							String depon = HeapWalker.testNumToTestClass.get(sfd.depGen) + "."
									+ HeapWalker.testNumToMethod.get(sfd.depGen);

							// Should this be already insider the SFD object via
							// the
							// dependsOn parameter ?

							if (HeapWalker.testNumToMethod.get(sfd.depGen) == null) {
								System.out.println("FOUND NULL.NULL " + sfd);
							}

							if (!depon.equals("null.null")) {
								cur.add(depon);
							} else {
								System.out.println("FOUND NULL.NULL " + sfd.depGen);
							}

							// TODO Here we use the value... to extract what
							// kind of dep
							// ?
							// Extract from value
							String val = sfd.value;

							// Remove from the deps whatever belongs to an ENUM
							// ($VALUES
							// for example)

							if (val != null) { // Now val is optional...
								Pattern p = Pattern.compile("dependsOn=\"(.*?)\"");
								Matcher m = p.matcher(val);
								while (m.find()) {
									String ss = m.group(1);
									if (ss.contains("...")) {
										continue;
									}

									// System.out.println("Matched "+ss);
									cur.add(ss);
								}
							}
						}

						// Order in ascending order again
						List<String> orderedDeps = new ArrayList<String>(cur);
						Collections.sort(orderedDeps);
						//
						for (String c : orderedDeps) {
							if (/* !c.trim().equals("null.null") && */!c.trim().equals("INIT.INIT")
									&& !s.equals(c.trim())) {
								bw.write(s + "," + c.trim() + "\n");
								//
								depsData.add(s + "," + c.trim());
							}
						}

					}
					bw.close();

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
				}

				// // Merge Results and send them back to DependencyCollector
				// // TODO Including Deps informations
				// //
				// int runCount = 0;
				// for (Result result : results) {
				// runCount = runCount + result.getRunCount();
				// }
				//
				// System.out.println("RemoteJUnitCore.main() Run " + runCount +
				// " + " + staticallyFailedTests.size());
				// // Patch to include statically failing tests
				// objectOutputStream.writeObject(new Integer(runCount +
				// staticallyFailedTests.size()));

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

				if (debug) {
					System.out.println("RemoteJUnitCore.main() Run " + runCount + " + " + staticallyFailedTests.size());
					System.out.println(
							"RemoteJUnitCore.main() Failed " + failureCount + " + " + assumptionFailedTests.size());
					System.out.println("RemoteJUnitCore.main() Ignored " + ignoreCount);
					//
					System.out.println("RemoteJUnitCore.main() Data deps " + depsData.size());
				}

				/// Update counters

				System.out.println("CollectorJUnitCore.main() Checking the results ");

				runCount = runCount + staticallyFailedTests.size();
				failureCount = failureCount + assumptionFailedTests.size();

				// Double check results
				if (runCount != printToFilelistener.getRunCount()) {
					System.out.println("WARNING : RUN count does not match  !");
				}

				if (failureCount != printToFilelistener.getFailedCount()) {
					System.out.println("WARNING : FAIL count does not match  !");

					// Print out the differences !
					List<Description> failedTest = printToFilelistener.getFailedTestNames();
					for (Failure f : failures) {
						System.out.println("CollectorJUnitCore.main() Checking " + f.getDescription());
						if (!failedTest.contains(f.getDescription())) {
							System.out.println("CollectorJUnitCore.main() FOUND A PROBLEM with " + f.getDescription());
						}
					}

					//
					for (Description d : failedTest) {
						boolean found = false;
						for (Failure f : failures) {
							if (f.getDescription().equals(d)) {
								found = true;
								break;
							}
						}
						if (!found) {
							System.out.println(
									"CollectorJUnitCore.main() FOUND A PROBLEM with " + d + " from PrintOutListener");
						}
					}

				}

				// We need only to pass the list of failed for the moment
				// objectOutputStream.writeObject(result);
				objectOutputStream.writeObject(new Long(runTime));
				// Patch to include statically failing tests
				objectOutputStream.writeObject(new Integer(runCount));
				//
				objectOutputStream.writeObject(new Integer(ignoreCount));
				// Failure count includes already staticallyFailingTests but not
				// assumption failed tests
				objectOutputStream.writeObject(new Integer(failureCount));
				// To deserialize Description we need the actual class that we
				// do not have on the otherside... so we
				// pass the string version of it !

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
				// Pad missing data ?!
				if (failureCount > (failures.size() + assumptionFailedTests.size())) {
					for (int i = 0; i < (failureCount - failures.size() + assumptionFailedTests.size()); i++) {
						objectOutputStream.writeObject("MISSING FAILED TEST !");
						// Write also WHY the test failed ?
						objectOutputStream.writeObject("No Stack trace");
					}
				}

				// // Public the Others
				// for (Integer i : staticallyFailedTests) {
				// objectOutputStream.writeObject(expectedTests.get(i));
				// // Write also WHY the test failed ?
				// objectOutputStream.writeObject(staticallyFailedTestTraces.get(i));
				// }

				objectOutputStream.writeObject(new Integer(depsData.size()));
				//
				for (String dep : depsData) {
					objectOutputStream.writeObject(dep);
				}

				objectOutputStream.flush();

			} // This will close stream and socke

		} catch (

		InstantiationError e) {
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
