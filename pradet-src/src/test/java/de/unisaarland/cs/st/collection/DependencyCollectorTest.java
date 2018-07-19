package de.unisaarland.cs.st.collection;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.lexicalscope.jewel.cli.CliFactory;

import de.unisaarland.cs.st.cut.collection.DependencyCollector;
import de.unisaarland.cs.st.cut.utils.EnumFinder;

public class DependencyCollectorTest {

	// @Test
	public void findEnumsFormApplicationpath() {
		String applicationClasspath = "/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Kappler/poldet/scripts_bash/projects/crystalvc/target/classes:"
				+ "/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Kappler/poldet/scripts_bash/projects/crystalvc/target/test-classes:"
				+ "/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:"
				+ "/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Kappler/poldet/scripts_bash/projects/crystalvc/lib/hgKit-279.jar:"
				+ "/Users/gambi/.m2/repository/org/jdom/jdom/1.1/jdom-1.1.jar:"
				+ "/Users/gambi/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:"
				+ "/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:"
				+ "/Users/gambi/.m2/repository/junit/junit/4.12/junit-4.12.jar";

		EnumFinder.main(new String[] { applicationClasspath });
	}

	// @Test
	public void findEnumsFormSystemClasspath() {
		EnumFinder.main(new String[] {});
	}

	@Test
	public void smokeTest() {
		// TODO ALL VALUES ARE HARDCODED !!
		String ddh = "/Users/gambi/Documents/Saarland/Frameworks/datadep-detector/target/";
		//
		String runOrder = "/Users/gambi/Desktop/PRADET/crystalvc/modcrystal-orig-order";
		//
		String packageFilter = "/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Kappler/poldet/scripts_bash/projects/crystalvc/package-filter";
		//
		String applicationClasspath = "/Users/gambi/.m2/repository/modcrystal/modcrystal/1.0-UDS-SNAPSHOT/modcrystal-1.0-UDS-SNAPSHOT.jar:"
				+ "/Users/gambi/.m2/repository/modcrystal/modcrystal/1.0-UDS-SNAPSHOT/modcrystal-1.0-UDS-SNAPSHOT-tests.jar:"
				+ "/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:"
				+ "/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Kappler/poldet/scripts_bash/projects/crystalvc/lib/hgKit-279.jar:"
				+ "/Users/gambi/.m2/repository/org/jdom/jdom/1.1/jdom-1.1.jar:"
				+ "/Users/gambi/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:"
				+ "/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:"
				+ "/Users/gambi/.m2/repository/junit/junit/4.12/junit-4.12.jar";

		try {
			// System.setProperty("debug", "true");
			// System.setProperty("enum-list",
			// "/Users/gambi/PRADET/code/src/test/resources/enumerations");
			//
			DependencyCollector.ParsingInterface cli = CliFactory.parseArguments(
					DependencyCollector.ParsingInterface.class,
					new String[] { //
							"--datadep-detector-home", ddh, //
							"--run-order", runOrder, //
							"--package-filter", packageFilter, //
							"--application-classpath", applicationClasspath, //
							"--enums-file", "/Users/gambi/PRADET/code/src/test/resources/enumerations" });
			//
			List<String> additionalArgs = new ArrayList<>();
			//
			additionalArgs.add("-DPROJECT_PATH=/Users/gambi/Desktop/PRADET/crystalvc");
			//
			DependencyCollector dc = new DependencyCollector(cli.getApplicationClasspath(),
					DependencyCollector.readTestsFromFile(cli.getRunOrder()), cli.getDatadepDetectorHome(),
					cli.getPackageFilter(), cli.getEnumerationsFile(), //
					additionalArgs, false);
			///
			List<String> deps = dc.collect();
			// This must be repeatable -
			Assert.assertTrue(deps.size() == 105);
			// for (String dep : deps) {
			// System.out.println(dep);
			// }

		} catch (Throwable e) {
			Assert.fail();
		}
	}
}
