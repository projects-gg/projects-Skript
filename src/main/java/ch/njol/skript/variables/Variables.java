package ch.njol.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.ConfigurationSerializer;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable.Value;
import ch.njol.util.Kleenean;
import ch.njol.util.NonNullPair;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.SynchronizedReference;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.yggdrasil.Yggdrasil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Handles all things related to variables.
 *
 * @see #setVariable(String, Object, Event, boolean)
 * @see #getVariable(String, Event, boolean)
 */
public class Variables {

	/**
	 * The version of {@link Yggdrasil} this class is using.
	 */
	public static final short YGGDRASIL_VERSION = 1;

	/**
	 * The {@link Yggdrasil} instance used for (de)serialization.
	 */
	public static final Yggdrasil yggdrasil = new Yggdrasil(YGGDRASIL_VERSION);

	/**
	 * Whether variable names are case-sensitive.
	 */
	public static boolean caseInsensitiveVariables = true;

	/**
	 * The {@link ch.njol.yggdrasil.ClassResolver#getID(Class) ID} prefix
	 * for {@link ConfigurationSerializable} classes.
	 */
	private static final String CONFIGURATION_SERIALIZABLE_PREFIX = "ConfigurationSerializable_";

	private final static Multimap<Class<? extends VariablesStorage>, String> TYPES = HashMultimap.create();

	// Register some things with Yggdrasil
	static {
		registerStorage(FlatFileStorage.class, "csv", "file", "flatfile");
		registerStorage(SQLiteStorage.class, "sqlite");
		registerStorage(MySQLStorage.class, "mysql");
		yggdrasil.registerSingleClass(Kleenean.class, "Kleenean");
		// Register ConfigurationSerializable, Bukkit's serialization system
		yggdrasil.registerClassResolver(new ConfigurationSerializer<ConfigurationSerializable>() {
			{
				//noinspection unchecked
				info = (ClassInfo<? extends ConfigurationSerializable>) (ClassInfo<?>) Classes.getExactClassInfo(Object.class);
				// Info field is mostly unused in superclass, due to methods overridden below,
				//  so this illegal cast is fine
			}

			@Override
			@Nullable
			public String getID(@NotNull Class<?> c) {
				if (ConfigurationSerializable.class.isAssignableFrom(c)
						&& Classes.getSuperClassInfo(c) == Classes.getExactClassInfo(Object.class))
					return CONFIGURATION_SERIALIZABLE_PREFIX +
							ConfigurationSerialization.getAlias(c.asSubclass(ConfigurationSerializable.class));

				return null;
			}

			@Override
			@Nullable
			public Class<? extends ConfigurationSerializable> getClass(@NotNull String id) {
				if (id.startsWith(CONFIGURATION_SERIALIZABLE_PREFIX))
					return ConfigurationSerialization.getClassByAlias(
							id.substring(CONFIGURATION_SERIALIZABLE_PREFIX.length()));

				return null;
			}
		});
	}

	/**
	 * The variable storages configured.
	 */
	static final List<VariablesStorage> STORAGES = new ArrayList<>();

	/**
	 * @return a copy of the list of variable storage handlers
	 */
	public static @UnmodifiableView List<VariablesStorage> getStores() {
		return Collections.unmodifiableList(STORAGES);
	}

	/**
	 * Register a VariableStorage class for Skript to create if the user config value matches.
	 *
	 * @param <T> A class to extend VariableStorage.
	 * @param storage The class of the VariableStorage implementation.
	 * @param names The names used in the config of Skript to select this VariableStorage.
	 * @return if the operation was successful, or if it's already registered.
	 */
	public static <T extends VariablesStorage> boolean registerStorage(Class<T> storage, String... names) {
		if (TYPES.containsKey(storage))
			return false;
		for (String name : names) {
			if (TYPES.containsValue(name.toLowerCase(Locale.ENGLISH)))
				return false;
		}
		for (String name : names)
			TYPES.put(storage, name.toLowerCase(Locale.ENGLISH));
		return true;
	}

	/**
	 * Load the variables configuration and all variables.
	 * <p>
	 * May only be called once, when Skript is loading.
	 *
	 * @return whether the loading was successful.
	 */
	public static boolean load() {
		assert variables.treeMap.isEmpty();
		assert variables.hashMap.isEmpty();
		assert STORAGES.isEmpty();

		Config config = SkriptConfig.getConfig();
		if (config == null)
			throw new SkriptAPIException("Cannot load variables before the config");

		Node databases = config.getMainNode().get("databases");
		if (!(databases instanceof SectionNode)) {
			Skript.error("The config is missing the required 'databases' section that defines where the variables are saved");
			return false;
		}

		Skript.closeOnDisable(Variables::close);

		// reports once per second how many variables were loaded. Useful to make clear that Skript is still doing something if it's loading many variables
		Thread loadingLoggerThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(Skript.logNormal() ? 1000 : 5000); // low verbosity won't disable these messages, but makes them more rare
				} catch (InterruptedException ignored) {}

				synchronized (TEMP_VARIABLES) {
					Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
					if (tvs != null)
						Skript.info("Loaded " + tvs.size() + " variables so far...");
					else
						break; // variables loaded, exit thread
				}
			}
		});
		loadingLoggerThread.start();

		try {
			boolean successful = true;

			for (Node node : (SectionNode) databases) {
				if (node instanceof SectionNode) {
					SectionNode sectionNode = (SectionNode) node;

					String type = sectionNode.getValue("type");
					if (type == null) {
						Skript.error("Missing entry 'type' in database definition");
						successful = false;
						continue;
					}

					String name = sectionNode.getKey();
					assert name != null;

					// Initiate the right VariablesStorage class
					VariablesStorage variablesStorage;
					Optional<?> optional = TYPES.entries().stream()
							.filter(entry -> entry.getValue().equalsIgnoreCase(type))
							.map(Entry::getKey)
							.findFirst();
					if (!optional.isPresent()) {
						if (!type.equalsIgnoreCase("disabled") && !type.equalsIgnoreCase("none")) {
							Skript.error("Invalid database type '" + type + "'");
							successful = false;
						}
						continue;
					}

					try {
						@SuppressWarnings("unchecked")
						Class<? extends VariablesStorage> storageClass = (Class<? extends VariablesStorage>) optional.get();
						Constructor<?> constructor = storageClass.getDeclaredConstructor(String.class);
						constructor.setAccessible(true);
						variablesStorage = (VariablesStorage) constructor.newInstance(type);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						Skript.error("Failed to initialize database `" + name + "`");
						successful = false;
						continue;
					}

					// Get the amount of variables currently loaded
					int totalVariablesLoaded;
					synchronized (TEMP_VARIABLES) {
						Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
						assert tvs != null;
						totalVariablesLoaded = tvs.size();
					}

					long start = System.currentTimeMillis();
					if (Skript.logVeryHigh())
						Skript.info("Loading database '" + node.getKey() + "'...");

					// Load the variables
					if (variablesStorage.load(sectionNode))
						STORAGES.add(variablesStorage);
					else
						successful = false;

					// Get the amount of variables loaded by this variables storage object
					int newVariablesLoaded;
					synchronized (TEMP_VARIABLES) {
						Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
						assert tvs != null;
						newVariablesLoaded = tvs.size() - totalVariablesLoaded;
					}

					if (Skript.logVeryHigh()) {
						Skript.info("Loaded " + newVariablesLoaded + " variables from the database " +
							"'" + sectionNode.getKey() + "' in " +
							((System.currentTimeMillis() - start) / 100) / 10.0 + " seconds");
					}
				} else {
					Skript.error("Invalid line in databases: databases must be defined as sections");
					successful = false;
				}
			}
			if (!successful)
				return false;

			if (STORAGES.isEmpty()) {
				Skript.error("No databases to store variables are defined. Please enable at least the default database, even if you don't use variables at all.");
				return false;
			}
		} finally {
			SkriptLogger.setNode(null);

			// make sure to put the loaded variables into the variables map
			int notStoredVariablesCount = onStoragesLoaded();
			if (notStoredVariablesCount != 0) {
				Skript.warning(notStoredVariablesCount + " variables were possibly discarded due to not belonging to any database " +
						"(SQL databases keep such variables and will continue to generate this warning, " +
						"while CSV discards them).");
			}

			// Interrupt the loading logger thread to make it exit earlier
			loadingLoggerThread.interrupt();

			saveThread.start();
		}
		return true;
	}

	/**
	 * Splits the given variable name into its parts,
	 * separated by {@link Variable#SEPARATOR}.
	 *
	 * @param name the variable name.
	 * @return the parts.
	 */
	public static String[] splitVariableName(String name) {
		String sep = Variable.SEPARATOR;
		int sepLen = sep.length();
		// Fast path for 0 or 1 separators — covers the vast majority of cases
		// and avoids ArrayList allocation entirely.
		int first = name.indexOf(sep);
		if (first == -1)
			return new String[]{name};
		int second = name.indexOf(sep, first + sepLen);
		if (second == -1)
			return new String[]{name.substring(0, first), name.substring(first + sepLen)};
		// 3+ parts — use a list for the remainder
		List<String> parts = new ArrayList<>();
		parts.add(name.substring(0, first));
		parts.add(name.substring(first + sepLen, second));
		int start = second + sepLen;
		int index;
		while ((index = name.indexOf(sep, start)) != -1) {
			parts.add(name.substring(start, index));
			start = index + sepLen;
		}
		parts.add(name.substring(start));
		return parts.toArray(String[]::new);
	}

	/**
	 * A lock for reading and writing variables.
	 * <p>
	 * Deliberately <b>not</b> fair. Global variable reads happen many thousands of times per tick,
	 * and a fair lock makes every reader queue behind any already-waiting thread, so each read
	 * becomes a park/unpark pair instead of an uncontended CAS. Under Skript's read rate the queue
	 * never drains and the convoy sustains itself, which parks the server thread indefinitely.
	 * <p>
	 * Readers cannot starve a writer here: global writes go through
	 * {@link #setVariable(String, Object)}, which uses a barging {@code tryLock} (unaffected by the
	 * fairness policy) and falls back to {@link #changeQueue}. The only blocking write lock
	 * acquisitions happen while loading and while shutting down, when nothing is reading.
	 */
	static final ReadWriteLock variablesLock = new ReentrantReadWriteLock(false);

	/**
	 * The {@link VariablesMap} storing global variables,
	 * must be locked with {@link #variablesLock}.
	 */
	static final VariablesMap variables = new VariablesMap();

	/**
	 * A map storing all local variables,
	 * indexed by their {@link Event}.
	 */
	private static final Map<Event, VariablesMap> localVariables = new ConcurrentHashMap<>();

	/**
	 * Gets the {@link TreeMap} of all global variables.
	 * <p>
	 * Remember to lock with {@link #getReadLock()} and to not make any changes!
	 */
	static TreeMap<String, Object> getVariables() {
		return variables.treeMap;
	}

	/**
	 * Gets the {@link Map} of all global variables.
	 * <p>
	 * This map cannot be modified.
	 * Remember to lock with {@link #getReadLock()}!
	 */
	static Map<String, Object> getVariablesHashMap() {
		return Collections.unmodifiableMap(variables.hashMap);
	}

	/**
	 * Gets the lock for reading variables.
	 *
	 * @return the lock.
	 *
	 * @see #variablesLock
	 */
	static Lock getReadLock() {
		return variablesLock.readLock();
	}

	/**
	 * Removes local variables associated with given event and returns them,
	 * if they exist.
	 *
	 * @param event the event.
	 * @return the local variables from the event,
	 * or {@code null} if the event had no local variables.
	 */
	@Nullable
	public static VariablesMap removeLocals(Event event) {
		return localVariables.remove(event);
	}

	/**
	 * Sets local variables associated with given event.
	 * <p>
	 * If the given map is {@code null}, local variables for this event
	 * will be <b>removed</b>.
	 * <p>
	 * Warning: this can overwrite local variables!
	 *
	 * @param event the event.
	 * @param map the new local variables.
	 */
	public static void setLocalVariables(Event event, @Nullable Object map) {
		if (map != null) {
			localVariables.put(event, (VariablesMap) map);
		} else {
			removeLocals(event);
		}
	}

	/**
	 * Creates a copy of the {@link VariablesMap} for local variables
	 * in an event.
	 *
	 * @param event the event to copy local variables from.
	 * @return the copy.
	 */
	public static @Nullable Object copyLocalVariables(Event event) {
		VariablesMap from = localVariables.get(event);
		if (from == null)
			return null;

		// copy() iterates the backing TreeMap, which must not run concurrently with a
		// write from another thread (async sections). Synchronize on the per-event map.
		synchronized (from) {
			return from.copy();
		}
	}

	/**
	 * Copies local variables from provider to user, runs action, then copies variables back to provider.
	 * Removes local variables from user after action is finished.
	 * @param provider The originator of the local variables.
	 * @param user The event to copy the variables to and back from.
	 * @param action The code to run while the variables are copied.
	 */
	public static void withLocalVariables(Event provider, Event user, @NotNull Runnable action) {
		Variables.setLocalVariables(user, Variables.copyLocalVariables(provider));
		action.run();
		Variables.setLocalVariables(provider, Variables.copyLocalVariables(user));
		Variables.removeLocals(user);
	}

	/**
	 * Returns the internal value of the requested variable.
	 * <p>
	 * <b>Do not modify the returned value!</b>
	 * <p>
	 * This does not take into consideration default variables. You must use get methods from {@link ch.njol.skript.lang.Variable}
	 *
	 * @param name the variable's name.
	 * @param event if {@code local} is {@code true}, this is the event
	 *                 the local variable resides in.
	 * @param local if this variable is a local or global variable.
	 * @return an {@link Object} for a normal variable
	 * or a {@code Map<String, Object>} for a list variable,
	 * or {@code null} if the variable is not set.
	 */
	// TODO don't expose the internal value, bad API
	@Nullable
	public static Object getVariable(String name, @Nullable Event event, boolean local) {
		String n;
		if (caseInsensitiveVariables) {
			n = name.toLowerCase(Locale.ENGLISH);
		} else {
			n = name;
		}

		if (local) {
			VariablesMap map = localVariables.get(event);
			if (map == null)
				return null;

			// Per-event local variable maps are not thread-safe. An async section
			// (e.g. skript-reflect) may access the same event's variables concurrently
			// with the main thread. Synchronize on the per-event map to avoid corrupting
			// its backing TreeMap (which otherwise spins forever in TreeMap.getEntry).
			synchronized (map) {
				return map.getVariable(n);
			}
		} else {
			variablesLock.readLock().lock();
			try {
				// Prevent race conditions from returning variables with incorrect values.
				// queuedChanges always holds the most recently queued change per name, so this
				// resolves in constant time rather than scanning the whole queue on every read.
				if (!queuedChanges.isEmpty()) {
					VariableChange variableChange = queuedChanges.get(n);

					if (variableChange != null) {
						return variableChange.value;
					}
				}

				return variables.getVariable(n);
			} finally {
				variablesLock.readLock().unlock();
			}
		}
	}

	/**
	 * Returns an iterator over the values of this list variable.
	 *
	 * @param name the variable's name. This must be the name of a list variable, ie. it must end in *.
	 * @param event if {@code local} is {@code true}, this is the event
	 *                 the local variable resides in.
	 * @param local if this variable is a local or global variable.
	 * @return an {@link Iterator} of {@link Pair}s, containing the {@link String} index and {@link Object} value of the
	 * 			elements of the list. An empty iterator is returned if the variable does not exist.
	 */
	public static Iterator<Pair<String, Object>> getVariableIterator(String name, boolean local, @Nullable Event event) {
		assert name.endsWith("*");
		String subName = StringUtils.substring(name, 0, -1);

		// Snapshot the list variable's keys. For local variables this must be done while
		// holding the per-event map's monitor, otherwise a concurrent write from an async
		// section (skript-reflect) can corrupt the backing TreeMap mid-copy.
		VariablesMap localMap = (local && event != null) ? localVariables.get(event) : null;
		Iterator<String> keys;
		if (localMap != null) {
			synchronized (localMap) {
				keys = snapshotListKeys(name, event, local);
			}
		} else {
			keys = snapshotListKeys(name, event, local);
		}
		if (keys == null)
			return new EmptyIterator<>();
		return new Iterator<>() {
			@Nullable
			private String key;
			@Nullable
			private Object next = null;

			@Override
			public boolean hasNext() {
				if (next != null)
					return true;
				while (keys.hasNext()) {
					key = keys.next();
					if (key != null) {
						next = Variable.convertIfOldPlayer(subName + key, local, event, Variables.getVariable(subName + key, event, local));
						if (next != null && !(next instanceof TreeMap))
							return true;
					}
				}
				next = null;
				return false;
			}

			@Override
			public Pair<String, Object> next() {
				if (!hasNext())
					throw new NoSuchElementException();
				Pair<String, Object> n = new Pair<>(key, next);
				next = null;
				return n;
			}

			@Override
			public void remove() {
				if (key == null)
					throw new IllegalStateException();
				Variables.deleteVariable(key, event, local);
			}
		};
	}

	/**
	 * Snapshots the keys of a list variable into a detached iterator.
	 * <p>
	 * Callers that operate on a local variable must hold the per-event
	 * {@link VariablesMap} monitor while calling this, so the snapshot cannot
	 * race with a concurrent write from another thread.
	 */
	@SuppressWarnings("unchecked")
	private static @Nullable Iterator<String> snapshotListKeys(String name, @Nullable Event event, boolean local) {
		Object val = getVariable(name, event, local);
		if (val == null)
			return null;
		assert val instanceof TreeMap;
		// temporary list to prevent CMEs
		return new ArrayList<>(((Map<String, Object>) val).keySet()).iterator();
	}

	/**
	 * Deletes a variable.
	 *
	 * @param name the variable's name.
	 * @param event if {@code local} is {@code true}, this is the event
	 *                 the local variable resides in.
	 * @param local if this variable is a local or global variable.
	 */
	public static void deleteVariable(String name, @Nullable Event event, boolean local) {
		setVariable(name, null, event, local);
	}

	/**
	 * Sets a variable.
	 *
	 * @param name the variable's name.
	 *                Can be a "list variable::*", but {@code value}
	 *                must be {@code null} in this case.
	 * @param value The variable's value. Use {@code null}
	 *                 to delete the variable.
	 * @param event if {@code local} is {@code true}, this is the event
	 *                 the local variable resides in.
	 * @param local if this variable is a local or global variable.
	 */
	public static void setVariable(String name, @Nullable Object value, @Nullable Event event, boolean local) {
		if (caseInsensitiveVariables) {
			name = name.toLowerCase(Locale.ENGLISH);
		}

		// Check if conversion is needed due to ClassInfo#getSerializeAs
		if (value != null) {
			assert !name.endsWith("::*");

			ClassInfo<?> ci = Classes.getSuperClassInfo(value.getClass());
			Class<?> sas = ci.getSerializeAs();

			if (sas != null) {
				value = Converters.convert(value, sas);
				assert value != null : ci + ", " + sas;
			}
		}

		if (local) {
			assert event != null : name;

			// Get the variables map and set the variable in it
			VariablesMap map = localVariables.computeIfAbsent(event, e -> new VariablesMap());
			// Per-event local variable maps are not thread-safe. An async section
			// (e.g. skript-reflect) may mutate the same event's variables concurrently
			// with the main thread. Synchronize on the per-event map to avoid corrupting
			// its backing TreeMap (which otherwise spins forever in TreeMap.getEntry).
			synchronized (map) {
				map.setVariable(name, value);
			}
		} else {
			setVariable(name, value);
		}
	}

	/**
	 * Sets the given global variable name to the given value.
	 *
	 * @param name the variable name.
	 * @param value the value, or {@code null} to delete the variable.
	 */
	static void setVariable(String name, @Nullable Object value) {
		if (variablesLock.writeLock().tryLock()) {
			List<VariableChange> applied = null;
			try {
				if (!changeQueue.isEmpty()) { // Process older, queued changes if available
					applied = applyChangeQueue(DRAIN_BATCH_SIZE);
				}
				// Process requested change
				variables.setVariable(name, value);
			} finally {
				variablesLock.writeLock().unlock();
			}
			// Serialization (Yggdrasil) is too expensive to run while holding the write lock:
			// every global variable read parks until the lock frees (visible in spark as
			// Variables.getVariable -> ReadLock.lock -> park, stalling the main thread).
			// Apply map changes under the lock, serialize for storage after releasing it.
			// A concurrent writer may serialize a newer value of the same name first and be
			// overwritten by this older one in storage; the periodic full CSV rewrite
			// reconciles storage with the live map, so this window is acceptable.
			if (applied != null) {
				for (VariableChange change : applied)
					saveVariableChange(change.name, change.value);
			}
			saveVariableChange(name, value);
		} else {
			// Couldn't acquire variable write lock, queue the change (blocking here is a bad idea)
			queueVariableChange(name, value);
		}
	}

	/**
	 * Changes to variables that have not yet been performed.
	 */
	static final Queue<VariableChange> changeQueue = new ConcurrentLinkedQueue<>();

	/**
	 * An index over {@link #changeQueue}, mapping a variable name to the most recently queued
	 * change for it. Lets {@link #getVariable(String, Event, boolean)} resolve a pending change
	 * in constant time; scanning the queue itself is O(queue size) on every single read, which
	 * collapses once a long-running read lock holder (such as a full CSV rewrite) forces every
	 * write to be queued.
	 */
	private static final Map<String, VariableChange> queuedChanges = new ConcurrentHashMap<>();

	/**
	 * A variable change name-value pair.
	 */
	private static class VariableChange {

		/**
		 * The name of the changed variable.
		 */
		public final String name;

		/**
		 * The (possibly {@code null}) value of the variable change.
		 */
		@Nullable
		public final Object value;

		/**
		 * Creates a new {@link VariableChange} with the given name and value.
		 *
		 * @param name the variable name.
		 * @param value the new variable value.
		 */
		public VariableChange(String name, @Nullable Object value) {
			this.name = name;
			this.value = value;
		}

	}

	/**
	 * Queues a variable change. Only to be called when direct write is not
	 * possible, but thread cannot be allowed to block.
	 *
	 * @param name the variable name.
	 * @param value the new value.
	 */
	private static void queueVariableChange(String name, @Nullable Object value) {
		VariableChange change = new VariableChange(name, value);
		// The index must be populated before the queue, so that a change polled by
		// processChangeQueue is always still present in the index and can be removed again.
		queuedChanges.put(name, change);
		changeQueue.add(change);
	}

	/**
	 * Processes all entries in variable change queue.
	 * <p>
	 * Note that caller must acquire write lock before calling this,
	 * then release it.
	 */
	static void processChangeQueue() {
		for (VariableChange change : applyChangeQueue(Integer.MAX_VALUE))
			saveVariableChange(change.name, change.value);
	}

	/**
	 * How many queued changes a single write lock hold may apply. Bounds how long
	 * readers (the main thread) can park behind a backlog drain: after a full CSV
	 * rewrite the queue can hold the entire rewrite window's writes, and applying
	 * all of them in one hold caused multi-hundred-ms tick spikes.
	 */
	private static final int DRAIN_BATCH_SIZE = 10000;

	/**
	 * Applies up to {@code limit} entries of the variable change queue to the
	 * variables map, without serializing them for the storages.
	 * <p>
	 * Caller must hold the write lock. The returned changes must still be passed to
	 * {@link #saveVariableChange(String, Object)} by the caller — preferably after
	 * releasing the lock, so that readers don't park while the backlog serializes.
	 */
	static List<VariableChange> applyChangeQueue(int limit) {
		List<VariableChange> applied = new ArrayList<>();
		while (applied.size() < limit) { // Run as long as we still have changes
			VariableChange change = changeQueue.poll();
			if (change == null)
				break;

			try {
				variables.setVariable(change.name, change.value);
				applied.add(change);
			} finally {
				// This must run even if applying threw: a leftover index entry would make every
				// later read of this name return this change's value and shadow the map for good.
				// Only drop it if it still refers to the change just applied, so that a newer
				// queued change for the same name stays visible to readers.
				queuedChanges.remove(change.name, change);
			}
		}
		return applied;
	}

	/**
	 * Drains the change queue without blocking readers for the whole backlog:
	 * repeatedly applies one bounded batch to the variables map under a
	 * non-blocking write lock, releases the lock so readers can interleave,
	 * then serializes the batch for the storages. Stops early if the lock is
	 * contended.
	 */
	static void tryDrainChangeQueue() {
		while (!changeQueue.isEmpty()) {
			if (!variablesLock.writeLock().tryLock())
				return;
			List<VariableChange> applied;
			try {
				applied = applyChangeQueue(DRAIN_BATCH_SIZE);
			} finally {
				variablesLock.writeLock().unlock();
			}
			for (VariableChange change : applied)
				saveVariableChange(change.name, change.value);
			if (applied.isEmpty())
				return;
		}
	}

	/**
	 * Stores loaded variables while variable storages are being loaded.
	 * <p>
	 * Access must be synchronised.
	 */
	private static final SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> TEMP_VARIABLES =
			new SynchronizedReference<>(new HashMap<>());

	/**
	 * The amount of variable conflicts between variable storages where
	 * a warning will be given, with any conflicts than this value, no more
	 * warnings will be given.
	 *
	 * @see #loadConflicts
	 */
	private static final int MAX_CONFLICT_WARNINGS = 50;

	/**
	 * Keeps track of the amount of variable conflicts between variable storages
	 * while loading.
	 */
	private static int loadConflicts = 0;

	/**
	 * Sets a variable and moves it to the appropriate database
	 * if the config was changed.
	 * <p>
	 * Must only be used while variables are loaded
	 * when Skript is starting. Must be called on Bukkit's main thread.
	 * This method directly invokes
	 * {@link VariablesStorage#save(String, String, byte[])},
	 * i.e. you should not be holding any database locks or such
	 * when calling this!
	 *
	 * @param name the variable name.
	 * @param value the variable value.
	 * @param source the storage the variable came from.
	 * @return Whether the variable was stored somewhere. Not valid while storages are loading.
	 */
	static boolean variableLoaded(String name, @Nullable Object value, VariablesStorage source) {
		assert Bukkit.isPrimaryThread(); // required by serialisation

		if (value == null)
			return false;

		synchronized (TEMP_VARIABLES) {
			Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
			if (tvs != null) {
				NonNullPair<Object, VariablesStorage> existingVariable = tvs.get(name);

				// Check for conflicts with other storages
				conflict: if (existingVariable != null) {
					VariablesStorage existingVariableStorage = existingVariable.getSecond();

					if (existingVariableStorage == source) {
						// No conflict if from the same storage
						break conflict;
					}

					// Variable already loaded from another database, conflict
					loadConflicts++;

					// Warn if needed
					if (loadConflicts <= MAX_CONFLICT_WARNINGS) {
						Skript.warning("The variable {" + name + "} was loaded twice from different databases (" +
							existingVariableStorage.getUserConfigurationName() + " and " + source.getUserConfigurationName() +
							"), only the one from " + source.getUserConfigurationName() + " will be kept.");
					} else if (loadConflicts == MAX_CONFLICT_WARNINGS + 1) {
						Skript.warning("[!] More than " + MAX_CONFLICT_WARNINGS +
							" variables were loaded more than once from different databases, " +
							"no more warnings will be printed.");
					}

					// Remove the value from the existing variable's storage
					existingVariableStorage.save(name, null, null);
				}

				// Add to the loaded variables
				tvs.put(name, new NonNullPair<>(value, source));

				return false;
			}
		}

		variablesLock.writeLock().lock();
		try {
			variables.setVariable(name, value);
		} finally {
			variablesLock.writeLock().unlock();
		}

		// Move the variable to the right storage
		try {
			for (VariablesStorage variablesStorage : STORAGES) {
				if (variablesStorage.accept(name)) {
					if (variablesStorage != source) {
						// Serialize and set value in new storage
						Value serializedValue = serialize(value);
						if (serializedValue == null) {
							variablesStorage.save(name, null, null);
						} else {
							variablesStorage.save(name, serializedValue.type, serializedValue.data);
						}

						// Remove from old storage
						if (value != null)
							source.save(name, null, null);
					}
					return true;
				}
			}
		} catch (Exception e) {
			//noinspection ThrowableNotThrown
			Skript.exception(e, "Error saving variable named " + name);
		}

		return false;
	}

	/**
	 * Stores loaded variables into the variables map
	 * and the appropriate databases.
	 *
	 * @return the amount of variables
	 * that don't have a storage that accepts them.
	 */
	@SuppressWarnings("null")
	private static int onStoragesLoaded() {
		if (loadConflicts > MAX_CONFLICT_WARNINGS)
			Skript.warning("A total of " + loadConflicts + " variables were loaded more than once from different databases");

		Skript.debug("Databases loaded, setting variables...");

		synchronized (TEMP_VARIABLES) {
			Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
			TEMP_VARIABLES.set(null);
			assert tvs != null;

			variablesLock.writeLock().lock();
			try {
				// Calculate the amount of variables that don't have a storage
				int unstoredVariables = 0;
				for (Entry<String, NonNullPair<Object, VariablesStorage>> tv : tvs.entrySet()) {
					if (!variableLoaded(tv.getKey(), tv.getValue().getFirst(), tv.getValue().getSecond()))
						unstoredVariables++;
				}

				for (VariablesStorage variablesStorage : STORAGES)
					variablesStorage.allLoaded();

				Skript.debug("Variables set. Queue size = " + saveQueue.size());

				return unstoredVariables;
			} finally {
				variablesLock.writeLock().unlock();
			}
		}
	}

	/**
	 * Creates a {@link SerializedVariable} from the given variable name
	 * and value.
	 * <p>
	 * Must be called from Bukkit's main thread.
	 *
	 * @param name the variable name.
	 * @param value the value.
	 * @return the serialized variable.
	 */
	public static SerializedVariable serialize(String name, @Nullable Object value) {
		assert Bukkit.isPrimaryThread();

		// First, serialize the variable.
		SerializedVariable.Value var;
		try {
			var = serialize(value);
		} catch (Exception e) {
			throw Skript.exception(e, "Error saving variable named " + name);
		}

		return new SerializedVariable(name, var);
	}

	/**
	 * Serializes the given value.
	 * <p>
	 * Must be called from Bukkit's main thread.
	 *
	 * @param value the value to serialize.
	 * @return the serialized value.
	 */
	public static SerializedVariable.@Nullable Value serialize(@Nullable Object value) {
		assert Bukkit.isPrimaryThread();

		return Classes.serialize(value);
	}

	/**
	 * Serializes and adds the variable change to the {@link #saveQueue}.
	 *
	 * @param name the variable name.
	 * @param value the value of the variable.
	 */
	private static void saveVariableChange(String name, @Nullable Object value) {
		if (name.startsWith(Variable.EPHEMERAL_VARIABLE_TOKEN))
			return;
		saveQueue.add(serialize(name, value));
	}

	/**
	 * The queue of serialized variables that have not yet been written
	 * to the storage.
	 */
	static final BlockingQueue<SerializedVariable> saveQueue = new LinkedBlockingQueue<>();

	/**
	 * Whether the {@link #saveThread} should be stopped.
	 */
	private static volatile boolean closed = false;

	/**
	 * The thread that saves variables, i.e. stores in the appropriate storage.
	 */
	private static final Thread saveThread = Skript.newThread(() -> {
		while (!closed) {
			try {
				// Save one variable change
				SerializedVariable variable = saveQueue.take();

				for (VariablesStorage variablesStorage : STORAGES) {
					if (variablesStorage.accept(variable.name)) {
						variablesStorage.save(variable);

						break;
					}
				}
			} catch (InterruptedException ignored) {}
		}
	}, "Skript variable save thread");

	/**
	 * Closes the variable systems:
	 * <ul>
	 *     <li>Process all changes left in the {@link #changeQueue}.</li>
	 *     <li>Stops the {@link #saveThread}.</li>
	 * </ul>
	 */
	public static void close() {
		try { // Ensure that all changes are to save soon
			variablesLock.writeLock().lock();
			processChangeQueue();
		} finally {
			variablesLock.writeLock().unlock();
		}

		// First, make sure all variables are saved
		while (saveQueue.size() > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ignored) {}
		}

		// Then we can safely interrupt and stop the thread
		closed = true;
		saveThread.interrupt();
	}

	/**
	 * Creates a structural deep copy of the global variables tree.
	 * <p>
	 * Caller must hold the read lock — but only for the duration of the copy
	 * (milliseconds), instead of for a whole storage rewrite. Values are shared
	 * by reference: Skript replaces values on change rather than mutating them
	 * in place, so the snapshot stays a consistent point-in-time view.
	 */
	@SuppressWarnings("unchecked")
	static TreeMap<String, Object> copyVariablesTree() {
		return copyTree(variables.treeMap);
	}

	private static TreeMap<String, Object> copyTree(TreeMap<String, Object> map) {
		TreeMap<String, Object> copy = new TreeMap<>(map.comparator());
		for (Entry<String, Object> entry : map.entrySet()) {
			Object value = entry.getValue();
			//noinspection unchecked
			copy.put(entry.getKey(), value instanceof TreeMap ? copyTree((TreeMap<String, Object>) value) : value);
		}
		return copy;
	}

	/**
	 * Gets the amount of variables currently on the server.
	 *
	 * @return the amount of variables.
	 */
	public static int numVariables() {
		try {
			variablesLock.readLock().lock();
			return variables.hashMap.size();
		} finally {
			variablesLock.readLock().unlock();
		}
	}

}
