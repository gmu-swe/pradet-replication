package de.unisaarland.cs.st.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.junit.runners.PradetSuite;
import org.junit.runners.model.InitializationError;

import de.unisaarland.cs.st.cut.DefaultRunnerPassFail;
import de.unisaarland.cs.st.cut.JUnit3StylePassFail;
import de.unisaarland.cs.st.cut.NoRunnerFailFail;
import de.unisaarland.cs.st.cut.junit.PrintOutCurrentTestRunListener;

public class JUnitCoreTest {

	@Test
	public void createFakeDescription() throws InitializationError {
		Description d = Description.createTestDescription("de.unisaarland.cs.st.cut.DefaultRunnerPassFail",
				"defaultRunnerPassFail1-new");
		System.out.println("JUnitCoreTest.createFakeDescription() " + d);

		JUnitCore core = new JUnitCore();

		List<Runner> runners = new ArrayList<Runner>();
		runners.add(Request.method(DefaultRunnerPassFail.class, "defaultRunnerPassFail1").getRunner());

		List<Description> descriptions = new ArrayList<Description>();
		descriptions.add(d);

		PrintOutCurrentTestRunListener listener = new PrintOutCurrentTestRunListener(descriptions);
		core.addListener(listener);

		Result result = core.run(new PradetSuite((Class<?>) null, runners));

	}

	/**
	 * Approach 1 generate a request for each test method. Not sure how this
	 * will support parametric test. Probably Request is not serializable, but
	 * we can use ByValue semantic.
	 */
	@Test
	public void test() {
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListener() {
			@Override
			public void testRunStarted(Description description) throws Exception {
				System.out.println("testRunStarted() " + description);
				super.testRunStarted(description);
			}

			@Override
			public void testStarted(Description description) throws Exception {
				System.out.println("testStarted() " + description);
				super.testStarted(description);
			}
		});
		List<Request> requests = new ArrayList<>();
		requests.add(Request.method(DefaultRunnerPassFail.class, "defaultRunnerPassFail1"));
		requests.add(Request.method(DefaultRunnerPassFail.class, "defaultRunnerPassFail2"));

		for (Request request : requests) {
			core.run(request);
		}

		core = new JUnitCore();
		Collections.reverse(requests);
		for (Request request : requests) {
			core.run(request);
		}

		// public static Request classes(Computer computer, Class<?>... classes)
		// {
	}

	@Test
	public void test1() {
		JUnitCore core = new JUnitCore();
		core.run(DefaultRunnerPassFail.class);
	}

	/**
	 * Probably the only solution is to implement a SUITE runner, which takes
	 * classes or test methods or request for them as input and execute them on
	 * core. And this is what we do.
	 */

	@Test
	public void test3() {
		try {

			// SuiteMethodBuilder b = new SuiteMethodBuilder().
			// RunnerBuilder.runners(parent, children)
			JUnitCore core = new JUnitCore();
			core.addListener(new RunListener() {
				@Override
				public void testRunStarted(Description description) throws Exception {
					System.out.println("testRunStarted() " + description);
					super.testRunStarted(description);
				}

				@Override
				public void testStarted(Description description) throws Exception {
					System.out.println("testStarted() " + description);
					super.testStarted(description);
				}
			});
			List<Runner> runners = new ArrayList<>();
			runners.add(Request.method(DefaultRunnerPassFail.class, "defaultRunnerPassFail1").getRunner());
			runners.add(Request.method(NoRunnerFailFail.class, "noRunnerFailFail2").getRunner());

			PradetSuite s = new PradetSuite((Class<?>) null, runners);
			Result result = core.run(s);
			System.out.println("JUnitCoreTest.test2() result " + result.getRunCount());
			Assert.assertEquals(2, result.getRunCount());
		} catch (InitializationError e) {

		}
	}

	@Test
	public void testMixJUnit3AndJUnit4() {
		try {

			JUnitCore core = new JUnitCore();
			core.addListener(new RunListener() {
				@Override
				public void testRunStarted(Description description) throws Exception {
					System.out.println("testRunStarted() " + description);
					super.testRunStarted(description);
				}

				@Override
				public void testStarted(Description description) throws Exception {
					System.out.println("testStarted() " + description);
					super.testStarted(description);
				}
			});
			List<Runner> runners = new ArrayList<>();
			runners.add(Request.method(DefaultRunnerPassFail.class, "defaultRunnerPassFail1").getRunner());
			runners.add(Request.method(NoRunnerFailFail.class, "noRunnerFailFail2").getRunner());
			// Adding JUnit3 Style Tests
			runners.add(Request.method(JUnit3StylePassFail.class, "testDefaultRunnerPassFail1").getRunner());

			PradetSuite s = new PradetSuite((Class<?>) null, runners);
			Result result = core.run(s);
			Assert.assertEquals(3, result.getRunCount());
		} catch (InitializationError e) {

		}
	}

	@Test
	public void test2() {
		final List<String> referenceOrder = new ArrayList<>();
		referenceOrder.add("noRunnerFailFail2");
		referenceOrder.add("defaultRunnerPassFail2");

		JUnitCore core = new JUnitCore();
		core.addListener(new RunListener() {
			@Override
			public void testRunStarted(Description description) throws Exception {
				System.out.println("testRunStarted() " + description);
				super.testRunStarted(description);
			}

			@Override
			public void testStarted(Description description) throws Exception {
				System.out.println("testStarted() " + description);
				super.testStarted(description);
			}
		});
		Request request = Request.classes(DefaultRunnerPassFail.class, NoRunnerFailFail.class)//
				.filterWith(new Filter() {

					@Override
					public boolean shouldRun(Description description) {
						// System.out.println("shouldRun() " + description);
						// System.out.println("shouldRun() " +
						// description.getClassName());
						// System.out.println("shouldRun() " +
						// description.getDisplayName());
						if (description.getMethodName() == null)
							return true;
						// System.out.println("shouldRun() " +
						// description.getMethodName());
						return !description.getMethodName().endsWith("1");
					}

					@Override
					public String describe() {
						return null;
					}
				})//
				.sortWith(new Comparator<Description>() {

					@Override
					public int compare(Description o1, Description o2) {
						System.out.println("compare() " + o1.getDisplayName());
						System.out.println("compare() " + o2.getDisplayName());
						int o1InReference = referenceOrder.indexOf(o1.getMethodName());
						int o2InReference = referenceOrder.indexOf(o2.getMethodName());

						System.out.println(o1InReference);
						System.out.println(o2InReference);

						return o1InReference - o2InReference;
					}
				});

		Result result = core.run(request);
		System.out.println("JUnitCoreTest.test2() result " + result.getRunCount());
		Assert.assertEquals(2, result.getRunCount());
	}
	/**
	 * Use a COMPUTER and Classes. The sorting thingy is only in the context of
	 * a single class i suspect ...
	 */

}
