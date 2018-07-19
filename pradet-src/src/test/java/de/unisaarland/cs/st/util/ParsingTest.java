package de.unisaarland.cs.st.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.lexicalscope.jewel.cli.CliFactory;

import de.unisaarland.cs.st.cut.refinement.DependencyRefiner.ParsingInterface;

public class ParsingTest {

	@Test
	public void removeTrailingElement() {
		String testName_1 = "org.apache.crunch.impl.mr.collect.UnionCollectionTestunionMaterializeShouldNotThrowNPE[0]";
		String testName_2 = "org.apache.crunch.impl.mr.collect.UnionCollectionTestunionWriteShouldNotThrowNPE[0]";

		System.out.println("ParsingTest.removeTrailingElement(): " + testName_1.replaceAll("\\[.*\\]$", ""));
		System.out.println("ParsingTest.removeTrailingElement(): " + testName_2.replaceAll("\\[.*\\]$", ""));
	}

	@Test
	public void parseDefaultDisplayName() {
		String testName_1 = "testGetLocalState(crystal.model.LocalStateResultTest)";

		Matcher m = Pattern.compile("^(.*)\\((.*?)\\)").matcher(testName_1);
		while (m.find()) {
			System.out.println(m.group(2) + "." + m.group(1));
		}
	}

	@Test
	public void cliParsingTest() {
		ParsingInterface flags = CliFactory.parseArguments(ParsingInterface.class, //
				"--strict", //
				"--run-order", "", //
				"--application-classpath", "");
		assertTrue(flags.isStrictMode());
		//
		flags = CliFactory.parseArguments(ParsingInterface.class, //

				"--run-order", "", //
				"--application-classpath", "");
		assertFalse(flags.isStrictMode());
	}
}
