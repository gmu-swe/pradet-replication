package de.unisaarland.cs.st.cut;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class DefaultRunnerPassPass {

	@Test
	public void defaultRunnerPassPass1() {
		System.out.println("Executing defaultRunnerPassPass1");
		assertTrue(true);
	}

	@Test
	public void defaultRunnerPassPass2() {
		System.out.println("Executing defaultRunnerPassPass2");
		assertTrue(true);
	}
}