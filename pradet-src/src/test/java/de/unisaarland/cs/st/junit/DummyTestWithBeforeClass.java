package de.unisaarland.cs.st.junit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DummyTestWithBeforeClass {

	public static int count = 0;

	@BeforeClass
	public static void beforeClass() {
		count++;
		System.out.println("DummyTestWithBeforeClass.beforeClass() ---- " + count);
	}

	@Before
	public void before() {
		System.out.println("DummyTestWithBeforeClass.beforeClass()");
	}

	@Test
	public void test_1() {
		System.out.println("DummyTestWithBeforeClass.test_1()");
	}

	@Test
	public void test_2() {
		System.out.println("DummyTestWithBeforeClass.test_2()");
	}

	@Test
	public void test_3() {
		System.out.println("DummyTestWithBeforeClass.test_3()");
	}

	@After
	public void after() {
		System.out.println("DummyTestWithBeforeClass.after()");
	}

	@AfterClass
	public static void afterClass() {
		System.out.println("DummyTestWithBeforeClass.afterClass() ---- ");
	}
}
