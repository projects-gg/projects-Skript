package ch.njol.skript.variables2;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Closeable;

/**
 * Variable storage. Implementations of this class will handle
 * getting, setting and saving of variables.
 */
public abstract class VariableFileStorage extends VariableStorage implements Closeable {

	protected FileStorageConfiguration configuration;
	protected File file;

	/**
	 * The constructor for a storage. The constructor should only be used for setting final fields based on configurations.
	 * Use the {@link #initialize()} method for starting up the database. Errors can be called in this constructor. Logger on.
	 * 
	 * @param configuration The StorageConfiguration for this storage.
	 */
	public VariableFileStorage(FileStorageConfiguration configuration) {
		super(configuration);
		this.configuration = configuration;
	}

	@Override
	final boolean initialize() {
		File file = getFile(configuration.getFilePath()).getAbsoluteFile();
		this.file = file;
		if (file.exists() && !file.isFile()) {
			Skript.error("The database file '" + file.getName() + "' must be an actual file, not a directory.");
			return false;
		} else {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				Skript.error("Cannot create the database file '" + file.getName() + "': " + e.getLocalizedMessage());
				return false;
			}
		}
		if (!file.canWrite()) {
			Skript.error("Cannot write to the database file '" + file.getName() + "'!");
			return false;
		}
		if (!file.canRead()) {
			Skript.error("Cannot read from the database file '" + file.getName() + "'!");
			Skript.error("This means that no variables will be available and can also prevent new variables from being saved!");
			try {
				final File backup = FileUtils.backup(file);
				Skript.error("A backup of your variables.csv was created as " + backup.getName());
			} catch (final IOException e) {
				Skript.error("Failed to create a backup of your variables.csv: " + e.getLocalizedMessage());
			}
			return false;
		}
		return initialize(file);
	}

	@Nullable
	protected Task backupTask;

	void startBackupTask(Timespan timespan) {
		if (file == null || timespan.getTicks_i() == 0)
			return;
		backupTask = new Task(Skript.getInstance(), timespan.getTicks_i(), timespan.getTicks_i(), true) {
			@Override
			public void run() {
				synchronized (connectionLock) {
					disconnect();
					try {
						FileUtils.backup(file);
					} catch (final IOException e) {
						Skript.error("Automatic variables backup failed: " + e.getLocalizedMessage());
					} finally {
						connect();
					}
				}
			}
		};
	}

	abstract boolean initialize(File file);

	/**
	 * Used internally by Skript to modify the configuration field.
	 */
	final void reloadConfiguration(FileStorageConfiguration configuration) {
		onReload(configuration);
		this.configuration = configuration;
	}

	@Override
	public final boolean requiresFile() {
		return true;
	}

	/**
	 * Get the file from the user path input. Skript handles creating the file if this method doesn't.
	 * 
	 * @param path The path the user input into config.sk
	 * @return The file at the found path.
	 */
	public abstract File getFile(String path);

	/**
	 * Return the StorageConfiguration for this storage which contains the user's values from the config.sk
	 * 
	 * @return A NonNull StorageConfiguration for this storage.
	 */
	@Override
	public final FileStorageConfiguration getConfiguration() {
		return (FileStorageConfiguration) configuration;
	}

	/**
	 * Skript supports reloading of storage configurations so thus any storage implementation must abide by configuration changes.
	 * The StorageConfiguration field is updated after this method to allow for comparing changes. The argument provided is the new updated configuration.
	 * 
	 * @param configuration The new FileStorageConfiguration
	 */
	abstract void onReload(FileStorageConfiguration configuration);

	@Override
	final void onReload(StorageConfiguration configuration) {
		this.onReload((FileStorageConfiguration) configuration);
	}

}
