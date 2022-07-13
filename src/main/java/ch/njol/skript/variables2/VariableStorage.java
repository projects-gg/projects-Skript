package ch.njol.skript.variables2;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Variable storage. Implementations of this class will handle
 * getting, setting and saving of variables.
 */
public abstract class VariableStorage {

	protected StorageConfiguration configuration;

	/**
	 * The constructor for a storage. The constructor should only be used for setting final fields based on configurations.
	 * Use the {@link #initialize()} method for starting up the database. Errors can be called in this constructor. Logger on.
	 * 
	 * @param configuration The StorageConfiguration for this storage.
	 */
	public VariableStorage(StorageConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Used internally by Skript to modify the configuration field.
	 */
	final void reloadConfiguration(StorageConfiguration configuration) {
		onReload(configuration);
		this.configuration = configuration;
	}

	/**
	 * Return the StorageConfiguration for this storage which contains the user's values from the config.sk
	 * 
	 * @return A NonNull StorageConfiguration for this storage.
	 */
	public StorageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Skript supports reloading of storage configurations so thus any storage implementation must abide by configuration changes.
	 * The StorageConfiguration field is updated after this method to allow for comparing changes. The argument provided is the new updated configuration.
	 * 
	 * @param configuration The new StorageConfiguration
	 */
	abstract void onReload(StorageConfiguration configuration);

	/**
	 * Called after creation of the class.
	 * 
	 * @return boolean true if the initialize was successful.
	 */
	abstract boolean initialize();

	/**
	 * Gets a variable with given name.
	 * @param name Name of variable.
	 * @param event Event associated with the variable.
	 * @param local If it is a local variable or not.
	 * @return Variable or null if not found.
	 */
	@Nullable
	abstract Object getVariable(String name, @Nullable Event event, boolean local);

	/**
	 * Sets a variable with given name.
	 * @param name Name of variable.
	 * @param event Event associated with the variable.
	 * @param local If it is a local variable or not.
	 * @param value New value. Can be null to remove the variable.
	 */
	abstract void setVariable(String name, @Nullable Event event, boolean local, @Nullable Object value);

	/**
	 * Flushes all variables to storage from memory. This should be called on
	 * server shutdown, but rarely else.
	 */
	abstract void flush();

}
