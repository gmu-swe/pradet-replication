package de.unisaarland.cs.st.cut.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import de.unisaarland.cs.st.cut.utils.ClassFinder.Visitor;

public class EnumFinder {

	public static void main(String[] args) {

		for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
			System.out.println(s);
		}

		// Assuming only jars are there !
		String classpath = (args.length > 0) ? args[0] : System.getProperty("java.class.path");
		//

		List<URL> paths = new ArrayList<URL>();
		for (String cpEntry : classpath.split(System.getProperty("path.separator"))) {
			File file = new File(cpEntry);
			if (file.exists() && file.getName().toLowerCase().endsWith(".jar")) {
				try {
					paths.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
					System.out.println("EnumFinder.main() Problem with " + file + " skip !");
				}
			} else {
				if (file.isDirectory()) {
					try {
						paths.add(file.toURI().toURL());
					} catch (MalformedURLException e) {
						System.out.println("EnumFinder.main() Problem with " + file + " skip !");
					}
				}

			}
		}

		// // Load the provided jars into a separate URL classloader
		final URLClassLoader urlClassLoader = new URLClassLoader(paths.toArray(new URL[] {}),
				EnumFinder.class.getClassLoader());
		//
		// // Use the reflections util to extract all the Enums
		// Reflections reflections = new Reflections(new ConfigurationBuilder()
		// .setScanners(new SubTypesScanner(false), new
		// ResourcesScanner()).setUrls(paths));
		// // ClasspathHelper.forClassLoader(child)));
		//
		// Set<Class<? extends Enum>> allEnums =
		// reflections.getSubTypesOf(Enum.class);
		//
		// System.out.println("EnumFinder.main() " + allEnums);
		final Visitor<String> visitor = new Visitor<String>() {
			@Override
			public boolean visit(String clazz) {
				// Skip com/sun classes
				if (clazz.startsWith("com.sun.") //
						|| clazz.startsWith("sun.") //
						|| clazz.startsWith("com.oracle.") //
						|| clazz.startsWith("oracle.") //
						|| clazz.startsWith("jdk.") //
						|| clazz.startsWith("javax.") //
						|| clazz.startsWith("javafx.")// Not sure what's this
						|| clazz.startsWith("netscape.")// Not sure what's this
						|| clazz.startsWith("java.awt.") //
						|| clazz.startsWith("java.") //

				//
				) {
					return true;
				}

				try {
					if (urlClassLoader.loadClass(clazz).isEnum()) {
						System.out.println(clazz);
					}
				} catch (Throwable e) {
					System.out.println("Error while processing " + clazz);
				}
				return true;
			}
		};

		if (args.length == 0)
			ClassFinder.findClasses(visitor);
		else
			ClassFinder.findClasses(visitor, args[0]);
	}
}
