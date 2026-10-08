package ch.njol.skript.variables;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The local variables of every event, keyed by the event.
 * <p>
 * Normally every thread sees one shared map per event. A flow that keeps running on another
 * thread while its event may still be dispatched to other triggers (an async continuation) must
 * not use that shared slot: installing its variables there replaces the variables of whichever
 * trigger is walking the same event at that moment, and its cleanup wipes them. Such a flow runs
 * through {@link #runIsolated} instead. For the duration of that call every access the
 * <i>calling thread</i> makes to that event's local variables goes to the flow's own map; other
 * threads, and the shared slot, never see it.
 * <p>
 * The routing lives in the map itself rather than in {@link Variables}, so addons that reach this
 * map directly (skript-reflect does, through reflection) get the same view as Skript. Only the
 * methods Skript and those addons use are routed ({@link #get}, {@link #containsKey},
 * {@link #getOrDefault}, {@link #put}, {@link #putIfAbsent}, {@link #remove(Object)},
 * {@link #remove(Object, Object)} and {@link #computeIfAbsent}); every other method only sees the
 * shared slots.
 */
final class LocalVariablesStore extends ConcurrentHashMap<Event, VariablesMap> {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * The innermost isolated scope running on this thread, {@code null} outside of one.
	 * <p>
	 * A stack rather than a single slot: a continuation can run another one inline (an async pool
	 * that is saturated runs work on the submitting thread), so scopes of the same or of different
	 * events nest. A lookup always resolves to the innermost scope of the requested event.
	 */
	private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

	/**
	 * Runs {@code action} with {@code map} as the local variables of {@code event}, visible to the
	 * calling thread only.
	 *
	 * @return the local variables the event has when {@code action} returns, or {@code null} if it
	 * has none, e.g. because they were handed to another continuation
	 */
	@Nullable VariablesMap runIsolated(Event event, @Nullable VariablesMap map, Runnable action) {
		Scope outer = SCOPE.get();
		Scope scope = new Scope(event, map, outer);
		SCOPE.set(scope);
		try {
			action.run();
		} finally {
			SCOPE.set(outer);
		}
		return scope.map;
	}

	/**
	 * Removes the local variables of {@code event}, unless they are claimed by a detached flow
	 * (see {@link Variables#setLocalVariablesDetached(Object, boolean)}).
	 *
	 * @return the removed local variables, or {@code null} if nothing was removed
	 */
	@Nullable VariablesMap removeUnlessDetached(Event event) {
		Scope scope = scopeOf(event);
		if (scope != null) {
			VariablesMap map = scope.map;
			if (map == null || map.isDetached())
				return null;
			scope.map = null;
			return map;
		}
		VariablesMap map = super.get(event);
		if (map == null || map.isDetached())
			return null;
		// Conditional: never remove a map that replaced the one observed above.
		return super.remove(event, map) ? map : null;
	}

	@Override
	public @Nullable VariablesMap get(Object event) {
		Scope scope = scopeOf(event);
		return scope != null ? scope.map : super.get(event);
	}

	@Override
	public @Nullable VariablesMap put(Event event, VariablesMap map) {
		Scope scope = scopeOf(event);
		if (scope == null)
			return super.put(event, map);
		if (map == null)
			throw new NullPointerException(); // same contract as the shared slots
		VariablesMap previous = scope.map;
		scope.map = map;
		return previous;
	}

	@Override
	public @Nullable VariablesMap putIfAbsent(Event event, VariablesMap map) {
		Scope scope = scopeOf(event);
		if (scope == null)
			return super.putIfAbsent(event, map);
		if (map == null)
			throw new NullPointerException(); // same contract as the shared slots
		VariablesMap previous = scope.map;
		if (previous == null)
			scope.map = map;
		return previous;
	}

	@Override
	public @Nullable VariablesMap remove(Object event) {
		Scope scope = scopeOf(event);
		if (scope == null)
			return super.remove(event);
		VariablesMap previous = scope.map;
		scope.map = null;
		return previous;
	}

	@Override
	public boolean remove(Object event, Object map) {
		Scope scope = scopeOf(event);
		if (scope == null)
			return super.remove(event, map);
		if (map == null || scope.map != map)
			return false;
		scope.map = null;
		return true;
	}

	@Override
	public VariablesMap computeIfAbsent(Event event, Function<? super Event, ? extends VariablesMap> mappingFunction) {
		Scope scope = scopeOf(event);
		if (scope == null)
			return super.computeIfAbsent(event, mappingFunction);
		VariablesMap map = scope.map;
		if (map == null) {
			map = mappingFunction.apply(event);
			scope.map = map;
		}
		return map;
	}

	/**
	 * @return the innermost scope of {@code event} on this thread, or {@code null} if the event's
	 * local variables live in the shared slot
	 */
	private static @Nullable Scope scopeOf(Object event) {
		for (Scope scope = SCOPE.get(); scope != null; scope = scope.outer) {
			if (scope.event == event)
				return scope;
		}
		return null;
	}

	/**
	 * One {@link #runIsolated} call. Only ever touched by the thread running it.
	 */
	private static final class Scope {

		private final Event event;
		private final @Nullable Scope outer;
		private @Nullable VariablesMap map;

		private Scope(Event event, @Nullable VariablesMap map, @Nullable Scope outer) {
			this.event = event;
			this.map = map;
			this.outer = outer;
		}

	}

}
