package ch.njol.skript.variables2;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;

/**
 * Represents the users configurations in the config.sk for a storage.
 */
public class StorageConfiguration {

	private final String databaseType;
	private final SectionNode node;
	private final String pattern;

	private Pattern variablePattern;

	/**
	 * @param databaseType The node 'type' in the config.sk for a storage. The name that was used.
	 * @param node The entire node of the storage configuration.
	 */
	public StorageConfiguration(String databaseType, SectionNode node) {
		this.databaseType = databaseType;
		this.node = node;
		this.pattern = getValue("pattern");
	}

	/**
	 * Used by Skript to validate configuration.
	 */
	boolean validate() {
		if (pattern == null)
			return false;
		try {
			variablePattern = pattern.equals(".*") || pattern.equals(".+") ? null : Pattern.compile(pattern);
		} catch (final PatternSyntaxException e) {
			Skript.error("Invalid pattern '" + pattern + "': " + e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	/**
	 * The variable pattern to match against to reference to this database.
	 * If a variable has this pattern, it will be saved to this database.
	 * 
	 * @return The String pattern defined in the configurations by the user. Null if it support anything.
	 */
	@Nullable
	public Pattern getPattern() {
		return variablePattern;
	}

	/**
	 * The value for 'type' in the config.sk that the user used.
	 * 
	 * @return The String value for node 'type'
	 */
	public String getUsedDatabaseType() {
		return databaseType;
	}

	/**
	 * Grab a string value from the storage configuration.
	 * 
	 * @param key The key to search and grab.
	 * @return A string of the storage configuration key. Null if not found.
	 */
	@Nullable
	public String getValue(String key) {
		return getValue(key, String.class);
	}

	/**
	 * Grab a value from the storage configuration and parsing to Skript type using classinfos.
	 * 
	 * @param <T> The assumed generic returning type.
	 * @param key key The key to search and grab.
	 * @param type The Skript classinfo type to parse a string against.
	 * @return The returning type based on the 'type' parameter. Null if not found or failure to parse. (Skript handles error reporting to user)
	 */
	@Nullable
	public <T> T getValue(String key, Class<T> type) {
		String value = node.getValue(key);
		if (value == null) {
			Skript.error("The storage configuration is missing the entry for '" + key + "' in the database '" + databaseType + "'");
			return null;
		}
		ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			T parsed = Classes.parse(value, type, ParseContext.CONFIG);
			if (parsed == null)
				log.printError("The entry for '" + key + "' in the database '" + databaseType + "' must be " + Classes.getSuperClassInfo(type).getName().withIndefiniteArticle());
			else
				log.printLog();
			return parsed;
		} finally {
			log.stop();
		}
	}

}
