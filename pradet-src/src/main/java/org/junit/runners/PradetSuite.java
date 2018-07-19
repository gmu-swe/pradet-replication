package org.junit.runners;

import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * Force specific orders
 * 
 * @author gambi
 *
 */
public class PradetSuite extends ParentRunner<Runner> {

	private final List<Runner> runners;

	/**
	 * Called by this class and subclasses once the runners making up the suite
	 * have been determined
	 *
	 * @param klass
	 *            root of the suite
	 * @param runners
	 *            for each class in the suite, a {@link Runner}
	 */
	public PradetSuite(Class<?> klass, List<Runner> runners) throws InitializationError {
		super(klass);
		this.runners = Collections.unmodifiableList(runners);
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	@Override
	protected Description describeChild(Runner child) {
		return child.getDescription();
	}

	@Override
	protected void runChild(Runner runner, final RunNotifier notifier) {
		runner.run(notifier);
	}
}
