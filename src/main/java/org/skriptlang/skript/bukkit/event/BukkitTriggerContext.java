package org.skriptlang.skript.bukkit.event;

import org.bukkit.event.Event;
import org.skriptlang.skript.lang.context.TriggerContext;

public class BukkitTriggerContext implements TriggerContext {

	private final Event event;
	private final String name;

	public BukkitTriggerContext(Event event, String name) {
		this.event = event;
		this.name = name;
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public String getName() {
		return name;
	}

}
