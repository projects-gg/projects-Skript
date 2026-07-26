package ch.njol.skript.lang;

import org.bukkit.event.Event;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Represents a loop section.
 * 
 * @see ch.njol.skript.sections.SecWhile
 * @see ch.njol.skript.sections.SecLoop
 */
public abstract class LoopSection extends Section implements SyntaxElement, Debuggable, SectionExitHandler {

	/**
	 * Loop state is keyed by event because one parsed loop is shared by every execution of its
	 * trigger. Those executions are not necessarily sequential: a {@code wait} inside the loop
	 * suspends one and lets another enter, and an async continuation runs the loop on a pool
	 * thread entirely, so the map is written concurrently. It must therefore be synchronized —
	 * concurrent writes to a plain {@link WeakHashMap} can corrupt its table and make a later
	 * lookup spin forever.
	 * <p>
	 * Weak keys are kept: {@link #exit(Event)} only runs when a loop finishes normally, so an
	 * abandoned loop (a {@code stop}, or an exception) relies on the event being collected.
	 */
	protected final transient Map<Event, Long> currentLoopCounter =
		Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * @param event The event where the loop is used to return its loop iterations
	 * @return The loop iteration number
	 */
	public long getLoopCounter(Event event) {
		return currentLoopCounter.getOrDefault(event, 1L);
	}

	/**
	 * @return The next {@link TriggerItem} after the loop
	 */
	public abstract TriggerItem getActualNext();

	/**
	 * Exit the loop, used to reset the loop properties such as iterations counter
	 * @param event The event where the loop is used to reset its relevant properties
	 */
	@Override
	public void exit(Event event) {
		currentLoopCounter.remove(event);
	}

}
