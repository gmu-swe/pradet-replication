package de.unisaarland.cs.st.cut.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * @author Paul Verest, taken from:
 *         https://stackoverflow.com/questions/30581851/maven-surefire-how-to-
 *         print-current-test-being-run
 * 
 *         Note that surefire can be configured to redirect console messages to
 *         files so on the main console we cannot see the messages of this
 *         listener, however, files are generated.
 * 
 *         We need to try/catch all the call because some projects, such as
 *         hazelcast, fail upon calling description.getDisplayName()
 * 
 *         Note that this won't work with mvn again
 */

// THIS CREATES Both run-order, reference-output, and execution time!
public class PrintOutCurrentTestRunListener extends RunListener {

	final static boolean debug = Boolean.getBoolean("debug");

	private final String DEFAULT_RUN_ORDER_FILE_NAME = "run-order";
	private File runOrderFile;

	private final String DEFAULT_REFERENCE_OUTPUT_NAME = "reference-output.csv";
	private File referenceOutputFile;

	private List<Description> runOrder = new ArrayList<Description>();
	private Map<Description, String> testOutput = new HashMap<Description, String>();
	private Map<Description, Long> testExecutionTimes = new HashMap<Description, Long>();
	// Avoid problems with statically failing tests
	private List<Description> expectedDescription = new ArrayList<Description>();
	// Are tests that either have the ignore annotation or are not actual test
	// cases (missing annotation or so)
	private List<Integer> invalidTests = new ArrayList<Integer>();
	private List<Integer> staticallyFailingTests = new ArrayList<Integer>();
	private int counter = 0;

	///
	private boolean printToFileDuringExecution = false;

	// Default constructor
	public PrintOutCurrentTestRunListener() {
		this(true);
	}

	public PrintOutCurrentTestRunListener(boolean printToFileDuringExecution) {
		this.printToFileDuringExecution = printToFileDuringExecution;
		if (debug) {
			System.out.println(
					"PrintOutCurrentTestRunListener.PrintOutCurrentTestRunListener() Print to file during execution ? "
							+ this.printToFileDuringExecution);
		}
	}

	// Only way to handle statically failing tests. This REQUIRES sequential
	// test execution !!
	public PrintOutCurrentTestRunListener(List<Description> expectedDescription) {
		this.expectedDescription.addAll(expectedDescription);
		this.printToFileDuringExecution = true;
		//
		if (debug) {
			System.out.println("PrintOutCurrentTestRunListener.PrintOutCurrentTestRunListener() Force print to file");
		}
	}

	// Accumulate tests
	public void testStarted(Description description) throws Exception {
		try {
			Long startTime = System.currentTimeMillis();

			/// We need to keep the actual description object here to match it
			/// later, however we will use the expectedDescription at the end to
			/// store results to file
			System.out.println("PrintOutCurrentTestRunListener.testStarted() " + counter + "/"
					+ expectedDescription.size() + ") " + description + " at " + startTime + " ");

			if (expectedDescription.size() > counter) {
				System.out.println("Expected " + expectedDescription.get(counter));
			}

			// This correspond to a method which does not correspond to an
			// actual test
			if (description.getDisplayName().contains("initializationError")
					&& description.getDisplayName().contains("org.junit.runner.manipulation.Filter")) {
				System.out.println(
						"\n\n PrintOutCurrentTestRunListener.testStarted() >>X>X>C>> TEST IS NOT VALID ! \n\n ");
				// Shouldn't this marked as FAILED test ?
				invalidTests.add(counter);
			} else if (description.getDisplayName().contains("initializationError")) {
				System.out.println(
						"\n\n PrintOutCurrentTestRunListener.testStarted() >>X>X>C>> TEST IS STATICALLY FAILING ! \n\n ");
				staticallyFailingTests.add(counter);
				// Override description
				description = expectedDescription.get(counter);
			}

			runOrder.add(description);
			// By default a test that does not fail or is not skipped passes
			testOutput.put(description, "PASS");
			testExecutionTimes.put(description, startTime);
		} catch (Exception e) {
			//
			System.out.println("PrintOutCurrentTestRunListener.testStarted() ERROR (IGNORED) ");
			e.printStackTrace();
		}
	}

	// This is invoked also in case of skipped tests ? Apparently for tests
	// marked with both @Test and @Ignore: testStart is NOT invoked but
	// testFinish it is

	@Override
	public void testFinished(Description description) throws Exception {
		try {

			if (staticallyFailingTests.contains(counter)) {
				// Override description
				description = expectedDescription.get(counter);
			}
			if (!testOutput.containsKey(description)) {
				System.out.println("??? PrintOutCurrentTestRunListener.testFinished() cannot find " + description);

				// Some tests do not triggered start but trigger fail, and
				// they also whos
				System.out.println(
						"PrintOutCurrentTestRunListener.testFinished() STATICALLY FAILING TEST or IGNORED Test!");
				staticallyFailingTests.add(counter);
				//
				runOrder.add(description);
				// By default a test that does not fail or is not skipped
				// passes
				testOutput.put(description, "IGNORE");
				testExecutionTimes.put(description, 1L);
				//
				counter++;

			} else {

				Long endTime = System.currentTimeMillis();
				Long startTime = testExecutionTimes.get(description);
				//
				testExecutionTimes.put(description, //
						/* Default to 1 millisec */
						(endTime - startTime) == 0 ? 1 : (endTime - startTime));
			}

			System.out.println("PrintOutCurrentTestRunListener.testFinished() " + counter + "/"
					+ expectedDescription.size() + ") " + description + " status " + testOutput.get(description));

		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		counter++;
	}

	// Apparently this can be invoked even if testStart and testFinished are not
	// invoked !
	@Override
	public void testFailure(Failure failure) throws Exception {
		try {

			if (debug) {
				System.out.println("PrintOutCurrentTestRunListener.testFailure() Failure " + failure);
			}

			Description description = failure.getDescription();

			if (staticallyFailingTests.contains(counter)) {
				// Override description
				description = expectedDescription.get(counter);
			}

			System.out.println("PrintOutCurrentTestRunListener.testFailure() " + counter + "/"
					+ expectedDescription.size() + ") " + description);

			// For some tests, like crystal.server.TestHgStateChecker, the
			// failure during initialization
			// prevent testStart and testFinished to be called. However,
			// testFailure is actually called !

			// Start was not register. Fail in BeforeClass !
			if (!runOrder.contains(description)) {
				// System.out.println(failure.getTrace());
				System.out.println("PrintOutCurrentTestRunListener.testFailure() " + description.getClassName());
				System.out.println("PrintOutCurrentTestRunListener.testFailure() " + description.getMethodName());

				// This should be true for Fail in BeforeClass (not necessarily
				// for other cases)
				if (description.getMethodName() == null) {
					// Patch this !
					System.out.println("PrintOutCurrentTestRunListener.testFailure() STATICALLY FAILING TEST !");
					staticallyFailingTests.add(counter);
					//
					description = expectedDescription.get(counter);

					System.out.println(
							"PrintOutCurrentTestRunListener.testFailure() Change description to " + description);

					runOrder.add(description);
					// By default a test that does not fail or is not skipped
					// passes
					testOutput.put(description, "FAIL");
					testExecutionTimes.put(description, 1L);
					//
					counter++;
					//
				}

			} else {

				testOutput.put(description, "FAIL");
			}

			System.out.println("PrintOutCurrentTestRunListener.testFailure() SIZE " + testOutput.size());
		} catch (Exception e) {
			System.out.println("PrintOutCurrentTestRunListener.testFailure() ERROR ");
			e.printStackTrace();
		}
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			Description description = failure.getDescription();
			if (staticallyFailingTests.contains(counter)) {
				// Override description
				description = expectedDescription.get(counter);
			}
			System.out.println("PrintOutCurrentTestRunListener.testAssumptionFailure() " + counter + "/"
					+ expectedDescription.size() + ") " + description);

			if (expectedDescription.size() > counter) {
				System.out.println("Expected " + expectedDescription.get(counter));
			}

			if (!testOutput.containsKey(description)) {
				System.out.println(
						"??? PrintOutCurrentTestRunListener.testAssumptionFailure() cannot find " + description);
			}
			// This should not be ignored because it is a Failure
			testOutput.put(description, "FAIL");
		} catch (Exception e) {
		}
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		try {
			if (staticallyFailingTests.contains(counter)) {
				// Override description
				description = expectedDescription.get(counter);
			}
			System.out.println("PrintOutCurrentTestRunListener.testIgnored() " + counter + "/"
					+ expectedDescription.size() + ") " + description);

			if (expectedDescription.size() > counter) {
				System.out.println("Expected " + expectedDescription.get(counter));
			}
			// There's input values involved ! - This should update it
			if (!testOutput.containsKey(description)) {
				System.out.println("PrintOutCurrentTestRunListener.testIgnored() cannot find " + description);
			}
			testOutput.put(description, "IGNORED");
		} catch (Exception e) {
		}
	}

	// This is needed for mvn
	@Override
	public void testRunFinished(Result result) throws Exception {
		if (printToFileDuringExecution)
			storeResultsToFileDuringRun();
		///
		super.testRunFinished(result);
	}

	// Print to file- Is this thread safe ?!
	private final Pattern extractMethodNameAndClassName = Pattern.compile("^(.*)\\((.*?)\\)");

	public void storeResultsToFileDuringRun() {
		long id = System.currentTimeMillis();

		System.out.println(
				">>> SAVING TO FILE (Remove Invalid files): PrintOutCurrentTestRunListener.storeResultsToFileDuringRun() ");
		//
		// Not sure when
		String fileName = System.getProperty("run-order.file", DEFAULT_RUN_ORDER_FILE_NAME);

		if (System.getProperty("run-order.file") == null) {
			fileName = DEFAULT_RUN_ORDER_FILE_NAME + "-" + id;
		} else if (printToFileDuringExecution) {
			fileName = fileName + "-" + id;
		}

		runOrderFile = new File(fileName);

		try {
			if (!runOrderFile.exists() && !runOrderFile.createNewFile()) {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() " + runOrderFile.getAbsolutePath()
						+ " cannot be created ");
			} else {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Output to "
						+ runOrderFile.getAbsolutePath() + " " + runOrder.size());

				try (PrintWriter pw = new PrintWriter(runOrderFile)) {
					for (Description d : runOrder) {

						if (invalidTests.contains(runOrder.indexOf(d))) {
							System.out.println(
									"PrintOutCurrentTestRunListener.testRunFinished() Test " + d + " is NOT valid !");
							// Do not output to file!
							// continue;
							testOutput.put(d, "IGNORED");
						}
						String t = d.getDisplayName();

						// Apply pretty print transformation on the default
						// JUnit
						// output - allows for customization later if needed
						// testGetLocalState(crystal.model.LocalStateResultTest)
						Matcher m = extractMethodNameAndClassName.matcher(t);
						//
						if (m.find()) {
							// Build FQN
							pw.println(m.group(2) + "." + m.group(1));
						} else {
							System.out.println(
									">>> PrintOutCurrentTestRunListener.testRunFinished() Problem parsing " + t);
						}
						//
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//
		fileName = System.getProperty("reference-output.file", DEFAULT_REFERENCE_OUTPUT_NAME);
		if (System.getProperty("reference-output.file") == null) {
			fileName = DEFAULT_REFERENCE_OUTPUT_NAME + "-" + id;
		} else if (printToFileDuringExecution) {
			fileName = fileName + "-" + id;
		}

		referenceOutputFile = new File(fileName);
		try {
			if (!referenceOutputFile.exists() && !referenceOutputFile.createNewFile()) {
				System.out.println("PrintOutCurrentTestRunListener.testRunStarted() "
						+ referenceOutputFile.getAbsolutePath() + " cannot be created ");
				referenceOutputFile = null;
			} else {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Output to "
						+ referenceOutputFile.getAbsolutePath());
				try (PrintWriter pw = new PrintWriter(referenceOutputFile)) {
					for (Entry<Description, String> e : testOutput.entrySet()) {

						Description description = e.getKey();

						if (invalidTests.contains(runOrder.indexOf(description))) {
							System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Test " + description
									+ " is NOT valid !");
							// continue;
						}

						String displayName = description.getDisplayName();

						Matcher m = extractMethodNameAndClassName.matcher(displayName);
						//
						if (m.find()) {
							// Build FQN
							pw.print(m.group(2) + "." + m.group(1));
						} else {
							System.out.println(">>> PrintOutCurrentTestRunListener.testRunFinished() Problem parsing "
									+ e.getKey());
							continue;
						}
						pw.print(",");
						pw.print(e.getValue());
						// Include the execution time as well
						pw.print(",");
						pw.println("" + ((testExecutionTimes.get(e.getKey()) != null)
								? testExecutionTimes.get(e.getKey()) : -1));

					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// This one called only at the end
	public void storeResultsToFile() {

		long id = System.currentTimeMillis();

		System.out.println(
				">>> SAVING TO FILE (Remove Invalid files): PrintOutCurrentTestRunListener.storeResultsToFile() ");
		//
		// Not sure when
		String fileName = System.getProperty("run-order.file", DEFAULT_RUN_ORDER_FILE_NAME);

		if (System.getProperty("run-order.file") == null) {
			fileName = DEFAULT_RUN_ORDER_FILE_NAME + "-" + id;
		}

		runOrderFile = new File(fileName);
		try {
			if (!runOrderFile.exists() && !runOrderFile.createNewFile()) {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() " + runOrderFile.getAbsolutePath()
						+ " cannot be created ");
			} else {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Output to "
						+ runOrderFile.getAbsolutePath() + " " + runOrder.size());

				try (PrintWriter pw = new PrintWriter(runOrderFile)) {
					for (Description d : runOrder) {

						if (invalidTests.contains(runOrder.indexOf(d))) {
							System.out.println(
									"PrintOutCurrentTestRunListener.testRunFinished() Test " + d + " is NOT valid !");
							// Do not output to file!
							// continue;
							testOutput.put(d, "IGNORED");
						}
						String t = d.getDisplayName();

						// Apply pretty print transformation on the default
						// JUnit
						// output - allows for customization later if needed
						// testGetLocalState(crystal.model.LocalStateResultTest)
						Matcher m = extractMethodNameAndClassName.matcher(t);
						//
						if (m.find()) {
							// Build FQN
							pw.println(m.group(2) + "." + m.group(1));
						} else {
							System.out.println(
									">>> PrintOutCurrentTestRunListener.testRunFinished() Problem parsing " + t);
						}
						//
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//
		fileName = System.getProperty("reference-output.file", DEFAULT_REFERENCE_OUTPUT_NAME);
		if (System.getProperty("reference-output.file") == null) {
			fileName = DEFAULT_REFERENCE_OUTPUT_NAME + "-" + id;
		}

		referenceOutputFile = new File(fileName);
		try {
			if (!referenceOutputFile.exists() && !referenceOutputFile.createNewFile()) {
				System.out.println("PrintOutCurrentTestRunListener.testRunStarted() "
						+ referenceOutputFile.getAbsolutePath() + " cannot be created ");
				referenceOutputFile = null;
			} else {
				System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Output to "
						+ referenceOutputFile.getAbsolutePath());
				try (PrintWriter pw = new PrintWriter(referenceOutputFile)) {
					for (Entry<Description, String> e : testOutput.entrySet()) {

						Description description = e.getKey();

						if (invalidTests.contains(runOrder.indexOf(description))) {
							System.out.println("PrintOutCurrentTestRunListener.testRunFinished() Test " + description
									+ " is NOT valid !");
							// continue;
						}

						String displayName = description.getDisplayName();

						Matcher m = extractMethodNameAndClassName.matcher(displayName);
						//
						if (m.find()) {
							// Build FQN
							pw.print(m.group(2) + "." + m.group(1));
						} else {
							System.out.println(">>> PrintOutCurrentTestRunListener.testRunFinished() Problem parsing "
									+ e.getKey());
							continue;
						}
						pw.print(",");
						pw.print(e.getValue());
						// Include the execution time as well
						pw.print(",");
						pw.println("" + ((testExecutionTimes.get(e.getKey()) != null)
								? testExecutionTimes.get(e.getKey()) : -1));

					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/// NOT SURE IF THOSE ARE IMPLEMENTED CORRECTLY !
	public int getRunCount() {
		return runOrder.size();
	}

	public int getFailedCount() {
		int fCount = 0;
		for (Entry<Description, String> testResult : testOutput.entrySet()) {
			if ("FAIL".equals(testResult.getValue())) {
				fCount++;
			}
		}
		return fCount;
	}

	public List<Description> getFailedTestNames() {
		List<Description> fList = new ArrayList<>();
		for (Entry<Description, String> testResult : testOutput.entrySet()) {
			if ("FAIL".equals(testResult.getValue())) {
				fList.add(testResult.getKey());
			}
		}
		return fList;
	}

}
