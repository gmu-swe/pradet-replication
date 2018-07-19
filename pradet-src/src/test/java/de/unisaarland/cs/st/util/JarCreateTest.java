package de.unisaarland.cs.st.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class JarCreateTest {

	@Ignore
	@Test
	public void testCreateJarNameAndContent() throws IOException {
		String fullName = "/private/var/folders/jf/9l21bwgd7czdw8jm38z2pr3m0000gq/T/classLoaderCache/93d1b4ac-b06d-4a82-9b2e-ea93c07cd672/Users/gambi/Documents/Saarland/Master.Theses/Sebastian.Klapper/dataset/341-projects/test-subjects/Activiti/modules/activiti-engine/target/test-classes/org/activiti/standalone/cfg/custom-mybatis-xml-mappers-activiti.cfg.xml";

		String resourceName = fullName.split("target/test-classes/")[1];
		String resourcePath = resourceName.substring(0, resourceName.lastIndexOf("/") + 1);
		// String resourceName =
		// fullResourceName.substring(fullResourceName.lastIndexOf("/") + 1,
		// fullResourceName.length());

		System.out.println("JarCreateTest " + resourcePath);
		System.out.println("JarCreateTest " + resourceName);

		File dummyJarFile = File.createTempFile("" + System.currentTimeMillis(), ".jar");
		// dummyJarFile.deleteOnExit();

		System.out.println("RemoteClassLoader.useOffer() Creating Dummy Jar File " + dummyJarFile);

		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		try (FileOutputStream fout = new FileOutputStream(dummyJarFile);
				JarOutputStream jarOut = new JarOutputStream(fout, manifest);) {

			// add(new File("inputDirectory"), target);

			jarOut.putNextEntry(new ZipEntry(resourcePath));
			jarOut.putNextEntry(new ZipEntry(resourceName));
			jarOut.write(Files.readAllBytes(Paths.get(fullName)));
			jarOut.closeEntry();
			jarOut.close();
			fout.close();
		}
		Assert.assertTrue(dummyJarFile.exists());
		Assert.assertTrue(dummyJarFile.length() > 0);
		dummyJarFile.toURI().toURL();
	}

}
