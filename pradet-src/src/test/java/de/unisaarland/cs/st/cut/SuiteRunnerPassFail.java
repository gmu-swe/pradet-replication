package de.unisaarland.cs.st.cut;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(
		{
			NoRunnerPassPass.class,
			NoRunnerPassFail.class,
			DefaultRunnerPassFail.class
		})
public class SuiteRunnerPassFail {

}
