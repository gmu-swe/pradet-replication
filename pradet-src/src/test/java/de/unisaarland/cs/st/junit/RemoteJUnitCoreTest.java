package de.unisaarland.cs.st.junit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RemoteJUnitCore;
import org.junit.runner.notification.Failure;

public class RemoteJUnitCoreTest {

	private int port = -1;

	@Before
	public void startServer() throws IOException {
		final ServerSocket server = new ServerSocket(0);
		port = server.getLocalPort();
		System.out.println("RemoteJUnitCore.startServer() Server listening to " + port);
		// This is blocking
		Thread serverThread = new Thread(new Runnable() {

			@Override
			public void run() {
				Socket clientSocket;
				try {
					clientSocket = server.accept();
					// This blocks
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					ObjectInputStream in = new ObjectInputStream(
							new BufferedInputStream(clientSocket.getInputStream()));

					Long runTime = (Long) in.readObject();
					Integer runCount = (Integer) in.readObject();
					Integer ignoreCount = (Integer) in.readObject();
					Integer failureCount = (Integer) in.readObject();

					for (int i = 0; i < failureCount; i++) {
						String failedTest = (String) in.readObject();
						String failedTestTrace = (String) in.readObject();
					}

					// System.out.println("Run( ) Got " + testResult);
					// System.out.println("\t " + testResult.getRunCount());
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}

			}
		});
		serverThread.start();
	}

	// BAD you should implement some runnable for server and client
	@Test
	public void testRemoteJUnitCore() throws IOException, InterruptedException {
		// TODO Check how infinitest or JCS do this !
		String jvm_location;
		if (System.getProperty("os.name").startsWith("Win")) {
			jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
					+ "java.exe";
		} else {
			jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
					+ "java";
		}

		String classpath = System.getProperty("java.class.path");

		String[] args = new String[] { jvm_location, "-cp", classpath, //
				RemoteJUnitCore.class.getName(), //
				"--port", "" + port, "--test-list", //
				"de.unisaarland.cs.st.cut.DefaultRunnerPassFail.defaultRunnerPassFail1" };

		System.out.println("RemoteJUnitCore.testRemoteJUnitCore() Executing : " + Arrays.toString(args));

		// Note that we need probably to split the test string if more than one
		// test is there
		ProcessBuilder processBuilder = new ProcessBuilder(args);
		processBuilder.inheritIO();
		Process slaveJVM = processBuilder.start();

		// Wait for everything to finish ... ?
		int exitCode = slaveJVM.waitFor();
		Assert.assertEquals(0, exitCode);
	}
}
