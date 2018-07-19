package de.unisaarland.cs.st.cut;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParameterizedRunnerFailFail {

	private int input;

	public ParameterizedRunnerFailFail(int input) {
		this.input = input;
	}
	
	@Parameters
	public static List<Object> data() {
		return Arrays.asList(new Object[] {0,1});
	}

	@Test
	public void parameterizedFailFail1() {
		System.out.println("Executing parameterizedFailFail1 with input: " + input);
		assertTrue(false);
	}

	@Test
	public void parameterizedFailFail2() {
		System.out.println("Executing parameterizedFailFail2 with input: " + input);
		assertTrue(false);
	}
}
