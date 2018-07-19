package de.unisaarland.cs.st.cut;

import static org.junit.Assert.*;

import org.junit.Test;

public class NoRunnerPassFail {
	
	@Test
	public void noRunnerPassFail1() {
		System.out.println("Executing noRunnerPassFail1");
		assertTrue(true);
	}

	@Test
	public void noRunnerPassFail2() {
		System.out.println("Executing noRunnerPassFail2");
		assertTrue(false);
	}

}
