package crystal.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DataSourceTestShort {

	public static DataSourceMod data;

	@Test
	public void testSetField() {
		String short_name = "Repository";
		String clone_string = "path";
		DataSourceMod.RepoKind repo_kind = DataSourceMod.RepoKind.HG;
		boolean hide = false;
		String parent = "parent";

		data = new DataSourceMod(short_name, clone_string, repo_kind, hide, parent);
		assertTrue("enabled", data.isEnabled());
		assertTrue("shortName", data.getShortName().equals(short_name));
		assertTrue("repo_kind", data.getKind().equals(repo_kind));
		assertFalse("hide", data.isHidden());
		assertTrue("parent", data.getParent().equals(parent));
	}

	@Test
	public void testSetKind() {
		// Read something out of data
		String c = data.getCloneString();

		assertTrue("Default repo kind", data.getKind().equals(DataSourceMod.RepoKind.HG));
		data.setKind(DataSourceMod.RepoKind.GIT);
		// What if we also force other mods ?
		data.setEnabled(false);
		data.setRemoteCmd("A remote command");
		assertTrue("Git kind", data.getKind().equals(DataSourceMod.RepoKind.GIT));
	}

	@Test
	public void testSetCloneString() {
		// Read the Kind
		DataSourceMod.RepoKind k = data.getKind();
		//
		assertTrue("Default clone string", data.getCloneString().equals("path"));
		data.setCloneString("path_2");
		// Access the data

		data.isEnabled();
		data.getRemoteCmd();
		//
		assertTrue("Set path", data.getCloneString().equals("path_2"));
	}

	static class DataSourceMod {

		public enum RepoKind {
			GIT, HG;
		}

		private boolean _enabled;

		private boolean _hide;

		private String _shortName;

		private String _cloneString;

		private String _parent;

		private RepoKind _repoKind;

		private String _remoteCmd = null;

		private String _compileCommand = null;

		private String _testCommand = null;

		public DataSourceMod(String shortName, String cloneString, RepoKind repoKind, boolean hide, String parent) {
			_enabled = true;
			_shortName = shortName.replace(' ', '_').replace('\\', '_').replace('/', '_').replace(':', '_').replace(';',
					'_');
			_cloneString = cloneString;
			_repoKind = repoKind;
			_hide = hide;
			setParent(parent);

		}

		public void setRemoteCmd(String remoteCmd) {
			_remoteCmd = remoteCmd;
		}

		public String getRemoteCmd() {
			return _remoteCmd;
		}

		public String getCompileCommand() {
			return _compileCommand;
		}

		public String getShortName() {
			return _shortName;
		}

		public String getCloneString() {
			return _cloneString;
		}

		public void setEnabled(boolean enabled) {
			_enabled = enabled;
		}

		public boolean isEnabled() {
			return _enabled;
		}

		public boolean isHidden() {
			return _hide;
		}

		public String getParent() {
			if (_parent == null)
				return "";
			else
				return _parent;
		}

		public void setParent(String parent) {
			if ((parent == null) || (parent.trim().equals("")))
				_parent = null;
			else
				_parent = parent;
		}

		public RepoKind getKind() {
			return _repoKind;
		}

		public void setKind(RepoKind kind) {
			_repoKind = kind;
		}

		public void setCloneString(String name) {
			_cloneString = name;
		}

	}

}
