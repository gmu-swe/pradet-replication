package de.unisaarland.cs.st.cut;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(
		{
			NoRunnerFailFail.class,
			DefaultRunnerFailFail.class,
			ParameterizedRunnerFailFail.class
		})
public class SuiteRunnerFail {

}
