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
package org.skriptlang.skript.base.event.eventvalues;

import ch.njol.skript.registrations.EventValues;
import org.skriptlang.skript.lang.expression.Expression;

public interface TimeSensitiveExpression<Type> extends Expression<Type> {

	/**
	 * Sets the time of this expression, i.e. whether the returned value represents this expression before, during or after an event.
	 * If this method returns false the expression will be discarded and an error message is printed.
	 *
	 * @param time {@link EventValues#TIME_PAST}, {@link EventValues#TIME_NOW}, or {@link EventValues#TIME_FUTURE}.
	 * @return Whether this expression has distinct time states.
	 */
	boolean setTime(int time);

	/**
	 * @return Whether this expression is before, during or after an event.
	 * @see #setTime(int)
	 */
	int getTime();

}
