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
package org.skriptlang.skript.lang.expression;

import ch.njol.util.Checker;
import org.skriptlang.skript.lang.context.TriggerContext;

public interface ListExpression<Type> extends Expression<Type> {

	/**
	 * This method significantly influences {@link #check(TriggerContext, Checker)}
	 *  and {@link #check(TriggerContext, Checker, boolean)} thus breaks conditions that use this expression if it returns a wrong value.
	 *
	 * This method will always return true if this is a {@link #isSingle() single} expression.
	 *
	 * @return Whether this expression returns all values at once or only part of them.
	 */
	boolean getAnd();

}
