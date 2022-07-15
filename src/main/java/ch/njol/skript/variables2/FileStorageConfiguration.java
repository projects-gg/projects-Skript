package ch.njol.skript.variables2;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.util.Timespan;

/**
 * Represents the users configurations in the config.sk for a storage.
 */
public class FileStorageConfiguration extends StorageConfiguration {

	private final String filePath;
	private Timespan backup;

	/**
	 * @param databaseType The node 'type' in the config.sk for a storage. The name that was used.
	 * @param node The entire node of the storage configuration.
	 */
	public FileStorageConfiguration(String databaseType, SectionNode node) {
		super(databaseType,  node);
		this.filePath = getValue("file");
	}

	/**
	 * Used by Skript to validate configuration.
	 */
	@Override
	boolean validate() {
		if (filePath == null) {
			Skript.error("The 'file' path for database '" + getUsedDatabaseType() + "' was null");
			return false;
		}
		if (!"0".equals(getValue("backup interval")))
			backup = getValue("backup interval", Timespan.class);
		return super.validate();
	}

	/**
	 * Grabs the file path the user defined in the config.sk for this storage.
	 * 
	 * @return The file path the user defined in the config.sk for this storage.
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Grab the backup interval that the user defined for this file storage.
	 * 
	 * @return the backup interval that the user defined for this file storage. Null if they don't want one.
	 */
	@Nullable
	public Timespan getBackupInterval() {
		return backup;
	}

}
