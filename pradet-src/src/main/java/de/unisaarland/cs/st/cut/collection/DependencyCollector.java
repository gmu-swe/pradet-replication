package de.unisaarland.cs.st.cut.collection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.CollectorJUnitCore;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Uses JB datadep-detector framework, based on HeapWalking and instrumented
 * JVM, to collect a set of data dependencies among test cases.
 * 
 * This work by executing in a RemoteJUnitCore instance the tests in the given
 * order with the support of a HeapWalking test listener which runs at every
 * test invocation.
 * 
 * Note: Statically Failing tests and other problematic tests do not always
 * trigger testStart/testFinishe etc
 * 
 * 
 * @author gambi
 *
 */
public class DependencyCollector {

	// We need the list of test
	// We need the applicationCP
	// We need where's the instrumented JVM and the various libraries
	// Do we need package-filter?
	/*
	 * <jvm>${poldet.home}/jre-inst/bin/java</jvm>
	 * <argLine>-Xbootclasspath/p:${dependency.detector.jar}
	 * -agentpath:${poldet.home}/libdeptracker.so
	 * -javaagent:${dependency.detector.jar} -Dwhitelist=package-filter
	 * </argLine>
	 */

	final static boolean debug = Boolean.getBoolean("debug");
	final static boolean showFailed = Boolean.getBoolean("showFailed");
	

	/**
	 * TODO: Probably to extend with non-default configurations and such
	 * 
	 * @author gambi
	 *
	 */
	public interface ParsingInterface {

		@Option(longName = { "datadep-detector-home" })
		public String getDatadepDetectorHome();

		@Option(longName = { "run-order" })
		public File getRunOrder();

		@Option(longName = { "package-filter" })
		public File getPackageFilter();

		@Option(longName = { "application-classpath" })
		public String getApplicationClasspath();

		@Option(longName = { "enums-file" })
		public File getEnumerationsFile();

		@Option(longName = { "redirect-to-file" })
		public boolean isRedirectToFile();
	}

	private String applicationClasspath;
	private List<String> referenceOrder = new ArrayList<>();
	private String datadepDetectorHome;
	// private File packageFilter;
	//
	private List<String> additionalArgs = new ArrayList<>();
	private boolean isRedirectToFile;

	public DependencyCollector(String applicationClasspath, List<String> referenceOrder, String datadepDetectorHome,
			File packageFilter, File enumerationsFile, List<String> additionalArgs, boolean isRedirectToFile) {
		this.applicationClasspath = applicationClasspath;
		this.referenceOrder.addAll(referenceOrder);
		this.datadepDetectorHome = datadepDetectorHome;
		// Still not sure where this should go...
		// this.packageFilter = packageFilter;
		// Additional Arguments
		this.additionalArgs.add("-Dwhitelist=" + packageFilter);
		this.additionalArgs.add("-Denum-list=" + enumerationsFile);
		//

		if (System.getenv().containsKey("EXTRA_JAVA_OPTS")) {
			if (debug) {
				System.out.println("DependencyCollector.DependencyCollector() Getting EXTRA_JAVA_OPTS ");
			}

			for (String additionalArg : Arrays.asList(System.getenv().get("EXTRA_JAVA_OPTS").split(" "))) {
				if (additionalArg.trim().length() > 0) // Skip empty args
					this.additionalArgs.add(additionalArg);
			}
		}

		this.additionalArgs.addAll(additionalArgs);

		//
		this.isRedirectToFile = isRedirectToFile;

	}

	public List<String> collect() {
		List<String> dataDeps = new ArrayList<String>();
		try {
			dataDeps.addAll(remoteExecutionWithJUnitCore(this.referenceOrder, applicationClasspath, datadepDetectorHome,
					additionalArgs, isRedirectToFile));
			// Collect results?
		} catch (Throwable e) {
			e.printStackTrace();
		}
		//
		return dataDeps;
	}

	/*
	 * This returns the list of collected deps as string
	 */
	public static List<String> remoteExecutionWithJUnitCore(//
			List<String> referenceOrder, //
			String applicationClasspath, //
			String datadepDetectorHome, List<String> additionalArgs, //
			boolean redirectToFile) throws IOException, InterruptedException, ExecutionException {

		try (ServerSocket server = new ServerSocket(0)) {
			final int port = server.getLocalPort();
			// server.accept() is Blocking So we need to use threads !
			// TODO Refactor to use Callable and return a Future<Result>
			// instead.
			// Use executor service !
			// https://blogs.oracle.com/CoreJavaTechTips/entry/get_netbeans_6
			ExecutorService pool = Executors.newFixedThreadPool(1);

			// Probably we do not need any of this, just the accept
			Future<List<String>> future = pool.submit(new Callable<List<String>>() {

				@Override
				public List<String> call() throws Exception {

					// This is blocking. But we do not expect results
					try (Socket clientSocket = server.accept()) {
						if (debug) {
							System.out.println("RemoteJUnitCore.startServer() Server listening to " + port);
							System.out.println(
									"RemoteJUnitCore.startServer() RemoteJUnitCore join the execution. Waiting for the results ");
						}

						// This blocks ?
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						ObjectInputStream in = new ObjectInputStream(
								new BufferedInputStream(clientSocket.getInputStream()));

						/// Extract all the data here
						final Long runTime = (Long) in.readObject();

						final Integer runCount = (Integer) in.readObject();
						//
						final Integer ignoreCount = (Integer) in.readObject();
						final Integer failureCount = (Integer) in.readObject();

						for (int i = 0; i < failureCount; i++) {
							String failedTest = (String) in.readObject();
							String stackTrace = (String) in.readObject();
							if (debug || showFailed) {
								System.out.println("Test " + failedTest + " Failed with stack trace:\n " + stackTrace);
							}
						}

						final Integer depsCount = (Integer) in.readObject();

						// Probable the list is serializable...
						List<String> deps = new ArrayList<String>();
						for (int i = 0; i < depsCount; i++) {
							deps.add((String) in.readObject());
						}
						//
						return deps;
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
						return null;
					}
				}
			});

			// Most likely this will not work on Windows
			String jvm_location = datadepDetectorHome + "/jre-inst/bin/java";
			// FIXME Do we need to include anything more than application cp ?!
			String classpath = System.getProperty("java.class.path") + File.pathSeparator + applicationClasspath;

			List<String> args = new ArrayList<>();
			args.add(jvm_location);

			// Not sure how this should be inlcuded
			args.add("-enableassertions");

			String dependencyDetectorJar = null;
			for (String cpEntry : System.getProperty("java.class.path").split(File.pathSeparator)) {
				if (cpEntry.contains("DependencyDetector") && cpEntry.contains("0.0.1-SNAPSHOT")) {
					dependencyDetectorJar = cpEntry;
					break;
				}
			}

			if (dependencyDetectorJar == null)
				throw new RuntimeException("Cannot find dependency.detector.jar");
			// Avoid exceptions
			args.add("-noverify");
			//
			args.add("-Xbootclasspath/p:" + dependencyDetectorJar);
			args.add("-agentpath:" + datadepDetectorHome + "/libdeptracker.so");
			args.add("-javaagent:" + dependencyDetectorJar);
			// Double check what's this. NOTE that package-filter is the actual
			// file name !!!
			// Additional Args contains -Dwhitelist=...
			args.addAll(additionalArgs);
			//

			args.add("-cp");
			args.add(classpath);
			// Enable Debug in Slave process if Debug is active in master
			// Process
			if (Boolean.getBoolean("debug")) {
				args.add("-Ddebug=true");

				if (System.getProperty("debug-file") != null)
					args.add("-Ddebug-file=" + System.getProperty("debug-file"));

			}
			// The main class
			args.add(CollectorJUnitCore.class.getName());
			// Input arguments
			args.add("--port");
			args.add("" + port);
			//
			args.add("--test-list");
			args.addAll(referenceOrder);

			if (Boolean.getBoolean("debug")) {
				System.out
						.println("DependencyRefiner.remoteExecutionWithJUnitCore() Starting Remote Execution: " + args);
			}

			// Note that we need probably to split the test string if more than
			// one
			// test is there
			ProcessBuilder processBuilder = new ProcessBuilder(args);
			// Probably remove this later ... This is OK, now we redirect test
			// execution output to file instead
			// if ( cli.toFile()) {
			// System.out
			// .println("DependencyRefiner.remoteExecutionWithJUnitCore()
			// Starting Remote Execution: " + args);

			// NOTE THIS ONE !!
			if (redirectToFile) {
				processBuilder.redirectError(new File(".error.log"));
				processBuilder.redirectError(new File(".output.log"));
			} else {
				processBuilder.inheritIO();
			}
			// }
			// System.out.println("DependencyRefiner.remoteExecutionWithJUnitCore()"
			// + processBuilder.command());

			Process slaveJVM = processBuilder.start();

			// Wait for everything to finish ... ?
			int exitCode = slaveJVM.waitFor();
			if (exitCode != 0) {
				System.out.println("DependencyCollector.executeTestsRemoteJUnitCore() ERROR !!");
				throw new RuntimeException("Remote test execution FAILED !!!");
			}

			return future.get();
		}
	}

	// TODO We might enforce some check at this level already to see if tests
	// are read or not
	public static List<String> readTestsFromFile(File runOrderFile) throws IOException {
		return Files.readAllLines(runOrderFile.toPath());
	}

	public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
		try { // Move Parsing to external code
			ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);
			//
			DependencyCollector dc = new DependencyCollector(cli.getApplicationClasspath(),
					readTestsFromFile(cli.getRunOrder()), cli.getDatadepDetectorHome(), cli.getPackageFilter(),
					cli.getEnumerationsFile(),
					// TODO !
					new ArrayList<String>(),
					//
					cli.isRedirectToFile());
			///
			List<String> deps = dc.collect();
			//
			if (debug) {
				System.out.println("Collected deps: ");
				for (String dep : deps) {
					System.out.println(dep);
				}
			}

			//
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			// Somehow it hangs
			System.exit(0);
		}
	}
}
