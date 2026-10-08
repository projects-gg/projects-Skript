package ch.njol.skript.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.variables.Variables;

/**
 * Effects that extend this class are ran asynchronously. Next trigger item will be ran
 * in main server thread, as if there had been a delay before.
 * <p>
 * Majority of Skript and Minecraft APIs are not thread-safe, so be careful.
 *
 * Make sure to add set {@link ch.njol.skript.ScriptLoader#hasDelayBefore} to
 * {@link ch.njol.util.Kleenean#TRUE} in the {@code init} method.
 */
public abstract class AsyncEffect extends Effect {
	
	@Override
	@Nullable
	protected TriggerItem walk(Event e) {
		debug(e, true);

		Object localVars = Variables.removeLocals(e); // Back up local variables

		if (!Skript.getInstance().isEnabled()) { // See https://github.com/SkriptLang/Skript/issues/3702
			Variables.setLocalVariables(e, localVars);
			return null;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Skript.getInstance(), () -> {
			Delay.addDelayedEvent(e); // Mark this event as delayed

			// The main thread may still be dispatching this event to other triggers, so the
			// variables must not go back under the shared event: bind them to this thread only.
			Object resultVars = Variables.runWithIsolatedLocals(e, localVars, () -> execute(e));

			TriggerItem next = getNext();
			if (next != null) {
				Bukkit.getScheduler().runTask(Skript.getInstance(), () -> { // Walk to next item synchronously
					Object timing = null;
					if (SkriptTimings.enabled()) { // getTrigger call is not free, do it only if we must
						Trigger trigger = getTrigger();
						if (trigger != null) {
							timing = SkriptTimings.start(trigger.getDebugLabel());
						}
					}

					// Whatever the walk leaves behind is dropped with the scope, so no cleanup is
					// needed; variables handed to a further continuation have left it already.
					Variables.runWithIsolatedLocals(e, resultVars, () -> TriggerItem.walk(next, e));

					SkriptTimings.stop(timing); // Stop timing if it was even started
				});
			}
		});
		return null;
	}
}
