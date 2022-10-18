/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package org.skriptlang.skript.bukkit.event;

import org.bukkit.event.Event;
import org.skriptlang.skript.lang.context.TriggerContext;

/**
 * A TriggerContext implementation to be used for {@link org.skriptlang.skript.lang.SyntaxElement}s that
 *  depend on a Bukkit {@link Event}.
 */
public class BukkitTriggerContext implements TriggerContext {

	private final Event event;
	private final String name;

	/**
	 * @param event The Bukkit Event occurring for all {@link org.skriptlang.skript.lang.SyntaxElement}s
	 *  processed through this context.
	 * @param name The name of this Bukkit Event (something like {@link Event#getEventName()}).
	 */
	public BukkitTriggerContext(Event event, String name) {
		this.event = event;
		this.name = name;
	}

	/**
	 * @return The Bukkit Event occurring for all {@link org.skriptlang.skript.lang.SyntaxElement}s
	 *  processed through this context.
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * @return The name of this Bukkit Event (as specified in the constructor).
	 */
	@Override
	public String getName() {
		return name;
	}

}
