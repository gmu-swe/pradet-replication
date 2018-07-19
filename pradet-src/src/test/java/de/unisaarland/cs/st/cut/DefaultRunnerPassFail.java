package de.unisaarland.cs.st.cut;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class DefaultRunnerPassFail {

	@Before
	public void before() {
		System.out.println("DefaultRunnerPassFail.before()");
	}

	@After
	public void after() {
		System.out.println("DefaultRunnerPassFail.after()");
	}

	@Test
	public void defaultRunnerPassFail1() {
		System.out.println("Executing defaultRunnerPassPass1");
		assertTrue(true);
	}

	@Test
	public void defaultRunnerPassFail2() {
		System.out.println("Executing defaultRunnerPassPass2");
		assertTrue(false);
	}
}
