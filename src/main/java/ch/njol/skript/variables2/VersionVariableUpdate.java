package ch.njol.skript.variables2;

import ch.njol.skript.util.Version;

/**
 * A class that handles updating variables on a Skript version change.
 * Example: In older Skript versions, a variable may be serialized different than future versions.
 */
public abstract class VersionVariableUpdate {

	protected final Version skriptVersion;

	/**
	 * Define the old Skript version that this changer handles from.
	 * 
	 * @param skriptVersion The older Skript version to check if updating from.
	 */
	public VersionVariableUpdate(Version skriptVersion) {
		this.skriptVersion = skriptVersion;
	}

	public Version getOldSkriptVersion() {
		return skriptVersion;
	}

	public abstract void onVariablePass(String type, byte[] values);

}
