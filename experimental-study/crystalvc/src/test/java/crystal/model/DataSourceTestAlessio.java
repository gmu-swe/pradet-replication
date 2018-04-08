package crystal.model;

import static org.junit.Assert.*;

import org.junit.Test;

import crystal.model.DataSource.RepoKind;

public class DataSourceTestAlessio {

	public static final int TIMEOUT = 20000;
	public static DataSource data;

	@Test
	public void testSetField() {
		String short_name = "Repository";
		String clone_string = "path";
		RepoKind repo_kind = RepoKind.HG;

		boolean hide = false; // This is not recognized as valid write. Probably
								// because false is default ? NO a check setting
								// that to true did not changed the result.

		// boolean enabled = false; // This is not recognized. Also an explicit
		// assignment to false does not reveal the data dep. The values is set
		// inside the constructor like the others.
		// data.setEnabled(enabled);
		//
		// Does the problem with boolean affects also other primitive types? Not
		// with Integer, but integer cause a dep on a String to disappear !
		// Does the problem with boolean is also at the constructor ? TODO Check
		// this

		String parent = "parent";// This also is not recognized. A String with
									// the same value was already create ?
									// A check using new String() results in the
									// right deps to be identified. String
									// parent = new String("parent");
		data = new DataSource(short_name, clone_string, repo_kind, hide, parent);

		// This has the effect of being identified as dep, even if there is no
		// READ. It might be a consequence of being a primitive type ?
		// TODO Check by removing reads of booleans
		int a = 1;
		// data.setA(a);
		//

		// assertTrue(1 == data.getA());

		assertTrue("enabled", data.isEnabled());
		assertTrue("shortName", data.getShortName().equals(short_name));
		assertTrue("repo_kind", data.getKind().equals(repo_kind));
		assertFalse("hide", data.isHidden());
		assertTrue("parent", data.getParent().equals(parent));
		assertNull("history", data.getHistory());
	}

	@Test
	/**
	 * <crystal.model.DataSource> <__enabled>true</__enabled>
	 * <__hide>false</__hide> <__shortName>Repository</__shortName>
	 * <__cloneString>path_2</__cloneString> <__parent>parent</__parent>
	 * <__repoKind dependsOn="INIT.INIT">HG</__repoKind>
	 * </crystal.model.DataSource>
	 * 
	 */
	public void testSetKind() {

		// Read clone string - WRONG - MISSING - Was written by
		// testSetCloneString

		System.out.println("DataSourceTestAlessio.testSetKind() data " + data);
		System.out.println("DataSourceTestAlessio.testSetKind() data.getCloneString() " + data.getCloneString());

		// When this runs after cloneString
		assertTrue("path".equals(data.getCloneString()));
		// String c = data.getCloneString();
		// data.setCloneString("test");
		// assertTrue("test".equals(data.getCloneString()));

		// Explicitly read kind - This makes no difference from the deps on data
		// But deps on single values of the enum are reporeted 3 times.
		// HG, GIT, HG -> the order is not considered but the sum is ok since we
		// READ KIND three times
		RepoKind k = data.getKind(); // HG

		System.out.println("DataSourceTestAlessio.testSetKind() READ REPO KIND " + k);

		// read kind - read HG - WRONG - Marked as written by INIT, was
		// testSetField
		assertTrue("Default repo kind", data.getKind().equals(RepoKind.HG)); // HG
		// write kind
		// read GIT
		data.setKind(RepoKind.GIT);
		// write enabled

		data.setEnabled(false);
		// write remoteCommand
		data.setRemoteCmd("A remote command");
		// read kind
		// read GIT
		assertTrue("Git kind", data.getKind().equals(RepoKind.GIT)); // GIT
	}

	@Test
	public void testSetCloneString() {
		// Read kind - OK - Marked as written by :
		// crystal.model.DataSourceTestShort.testSetField"
		RepoKind k = data.getKind();
		// Read cloneString - OK
		assertTrue("Default clone string", data.getCloneString().equals("path"));
		// Write cloneString
		data.setCloneString("path_2");
		// read enabled - OK
		data.isEnabled();
		// read remoteCommand - OK - This is MISSING because it is null and
		// never set
		data.getRemoteCmd();
		// read cloneString - OK
		assertTrue("Set path", data.getCloneString().equals("path_2"));
		//
		data.setCloneString("path");
	}

	@Test
	public void testSetCompileCommand() {
		assertTrue("Assert compile comman is null", data.getCompileCommand() == null);
		data.setCompileCommand("compileCommand");
		assertTrue("Assert compile comman  not null", data.getCompileCommand() != null);
	}

	@Test
	public void testSetCompileCommandToNull() {
		assertTrue("Assert compile comman  not null", data.getCompileCommand() != null);
		data.setCompileCommand(null);
		assertTrue("Assert compile comman is null", data.getCompileCommand() == null);
	}

	@Test
	/**
	 * <crystal.model.DataSource> <__enabled>false</__enabled>
	 * <__hide>false</__hide> <__shortName>Repository</__shortName>
	 * <__cloneString>path_2</__cloneString> <__parent>parent</__parent>
	 * <__repoKind dependsOn="crystal.model.DataSourceTestShort.testSetKind">GIT
	 * </__repoKind> <__remoteCmd>A remote
	 * command</__remoteCmd> </crystal.model.DataSource>
	 * 
	 * boolean hide = false; String parent = "parent"
	 */
	public void testReadALL() {
		//

		data.getCloneString(); // WRONG - MISSING: should be :
								// crystal.model.DataSourceTestShort.testSetCloneString
		data.getCompileCommand(); // OK - MISSING
		data.getHistory(); // OK - MISSING

		data.getKind(); // OK - crystal.model.DataSourceTestShort.testSetKind

		data.getParent(); // WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetField

		data.getRemoteCmd();// WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetKind
		// This one does not even registerd by heapWalk

		data.getShortName(); // WRONG - MISSING : should be :
								// crystal.model.DataSourceTestShort.testSetField
		data.getTestCommand(); // OK - MISSING
		// data.getA(); // Read also the integere
		//
		// data.isEnabled(); // WRONG - MISSING : should be :
		// crystal.model.DataSourceTestShort.testSetKind -
		// IS THIS TAKEN OVER BY - data.getKind() ?
		// data.isEnabled(); // WRONG - MISSING : should be :
		// crystal.model.DataSourceTestShort.testSetKind -
		// IS THIS TAKEN OVER BY - data.getKind() ?
		// This gets registered but disappear- CHECK IF ORDER /NUMBER OF READS
		// MATTERS
		// This gets registered but disappear
		// data.isHidden(); // WRONG - MISSING : should be :
		// crystal.model.DataSourceTestShort.testSetField -
		// CAN BE DEFAULT VALUES SO MAKE NO DIFFERENCE ?
	}

	@Test
	public void testReadALL2() {
		//
		data.getHistory();
		data.getCloneString();
		data.getCompileCommand();
		data.getKind();
		data.getParent();
		data.getRemoteCmd();
		data.getShortName();
		data.getTestCommand(); // OK - MISSING
		// data.getA(); // Read also the integer
		//
		// data.isEnabled(); // WRONG - MISSING : should be :
		data.isEnabled();
		// crystal.model.DataSourceTestShort.testSetKind -
		// IS THIS TAKEN OVER BY - data.getKind() ?
		// This gets registered but disappear
		// data.isHidden(); // WRONG - MISSING : should be :
		// crystal.model.DataSourceTestShort.testSetField -
		// CAN BE DEFAULT VALUES SO MAKE NO DIFFERENCE ?
		// data.getA();
	}

	@Test
	public void testReadALL3() {
		data.getHistory();
		data.getCloneString();
		data.getCompileCommand();
		data.getKind();
		data.getParent();
		data.getRemoteCmd();
		data.getShortName();
		data.getTestCommand(); // OK - MISSING
		// data.getA(); // Read also the integer
		//
		data.isEnabled(); // WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetKind -
							// IS THIS TAKEN OVER BY - data.getKind() ?
		// This gets registered but disappear
		data.isHidden(); // WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetField -
							// CAN BE DEFAULT VALUES SO MAKE NO DIFFERENCE ?
	}

	@Test
	public void testReadALL4() {
		//
		data.getHistory();
		data.getCloneString();
		data.getCompileCommand();
		data.getKind();
		data.getParent();
		data.getRemoteCmd();
		data.getShortName();
		data.getTestCommand(); // OK - MISSING
		//
		// data.getA(); // Read also the integer
		data.isEnabled(); // WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetKind -
							// IS THIS TAKEN OVER BY - data.getKind() ?
		// This gets registered but disappear
		data.isHidden(); // WRONG - MISSING : should be :
							// crystal.model.DataSourceTestShort.testSetField -
							// CAN BE DEFAULT VALUES SO MAKE NO DIFFERENCE ?
	}
}
