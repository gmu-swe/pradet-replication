package de.unisaarland.cs.st.cut;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParameterizedRunnerPassFail {

	
	private int input;

	public ParameterizedRunnerPassFail(int input) {
		this.input = input;
	}
	
	@Parameters
	public static List<Object> data() {
		return Arrays.asList(new Object[] {0,1});
	}

	@Test
	public void parameterizedPassFail1() {
		System.out.println("Executing parameterizedPassFail1 with input: " + input);
		assertTrue(true);
	}

	@Test
	public void parameterizedPassFail2() {
		System.out.println("Executing parameterizedPassFail2 with input: " + input);
		assertTrue(false);
	}
}
