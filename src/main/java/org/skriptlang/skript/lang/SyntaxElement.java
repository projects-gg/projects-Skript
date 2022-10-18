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
package org.skriptlang.skript.lang;

import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.lang.expression.Expression;

/**
 * A SyntaxElement represents a general structure or instruction type for the language.
 */
public interface SyntaxElement extends Debuggable {

	/**
	 * Called just after the constructor.
	 *
	 * @param exprs All %expr%s included in the matching pattern in the order they appear in the pattern.
	 * If an optional value was left out, it will still be included in this list holding the default value
	 *  of the desired type which usually depends on the event.
	 * @param matchedPattern The index of the pattern matched.
	 * @param isDelayed Whether this expression is being used after some sort of delay.
	 * @param parseResult Additional information about the parsing and matching results.
	 * @return Whether this expression was initialised successfully.
	 * An error should be printed prior to returning false to specify the cause.
	 */
	boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult);

	/**
	 * @see ParserInstance#get()
	 */
	default ParserInstance getParser() {
		return ParserInstance.get();
	}

}
