package de.unisaarland.cs.st.cut;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class DefaultRunnerFailFail {
	@Test
	public void defaultRunnerFailFail1() {
		System.out.println("Executing defaultRunnerFailFail1");
		assertTrue(false);
	}

	@Test
	public void defaultRunnerFailFail2() {
		System.out.println("Executing defaultRunnerFailFail2");
		assertTrue(false);
	}
}
