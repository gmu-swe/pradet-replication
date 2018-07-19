package de.unisaarland.cs.st.cut;

import junit.framework.TestCase;

public class JUnit3StylePassFail extends TestCase {

	public void setUp() {
		System.out.println("JUnit3StylePassFail.setup()");
	}

	public void testDefaultRunnerPassFail1() {
		System.out.println("JUnit3StylePassFail.testDefaultRunnerPassFail1()");
		assertTrue(true);
	}

	public void testDefaultRunnerPassFail2() {
		System.out.println("JUnit3StylePassFail.testDefaultRunnerPassFail2()");
		assertTrue(false);
	}
}
