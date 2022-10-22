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

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.lang.expression.Expression;
import org.skriptlang.skript.lang.expression.ExpressionType;

import java.util.ArrayList;
import java.util.List;

public interface TimeSensitiveExpression<Type> extends Expression<Type> {

	/**
	 * Registers an expression.
	 *
	 * @param expressionClass The expression's class
	 * @param returnType The superclass of all values returned by the expression
	 * @param type The expression's {@link ExpressionType type}. This is used to determine in which order to try to parse expressions.
	 * @param patterns Skript patterns that match this expression
	 * @throws IllegalArgumentException if returnType is not a normal class
	 */
	static <Element extends Expression<Type>, Type> void registerExpression(
		Class<Element> expressionClass, Class<Type> returnType, ExpressionType type, String... patterns
	) {

		List<String> newPatterns = new ArrayList<>();
		for (String pattern : patterns) {
			newPatterns.add(pattern);
			newPatterns.add("(past:)[the] (former|past|old) [state] [of] " + pattern);
			newPatterns.add("(past:)before [the event]" + pattern);
			newPatterns.add("(future:)[the] (future|to-be|new) [state] [of] " + pattern);
			newPatterns.add("(future:)(-to-be| after[(wards| the event)])");
		}

		Skript.registerExpression(expressionClass, returnType, type, newPatterns.toArray(new String[0]));
	}

	@Override
	default boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		boolean past = parseResult.hasTag("past");
		if (past || parseResult.hasTag("future")) {
			if (isDelayed == Kleenean.TRUE) {
				Skript.error("Cannot use time states after the event has already passed", ErrorQuality.SEMANTIC_ERROR);
				return false;
			}
			if (!setTime(past ? EventValues.TIME_PAST : EventValues.TIME_FUTURE)) {
				Skript.error(this + " does not have a " + (past ? "past" : "future") + " state", ErrorQuality.SEMANTIC_ERROR);
				return false;
			}
		}

		return true;
	}

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
