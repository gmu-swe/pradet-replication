package de.unisaarland.cs.st.util;

import java.io.File;

import org.junit.Test;

public class JUnitVersionTest {

	@Test
	public void testJunitVersion() {
		
		
		String applicationClasspath = System.getProperty("java.class.path");
		// Extract JUNIT VERSION
		String[] cpEntries = applicationClasspath.split(File.pathSeparator);
		for( String cpEntry : cpEntries ){
			if( cpEntry.contains("PRADET") && cpEntry.contains("junit")){
				System.out.println("JUnitVersionTest.testJunitVersion() " + cpEntry );
			}
		}
		//
		System.out.println("JUnitVersionTest.testJunitVersion() " + applicationClasspath);
		
	}
}
