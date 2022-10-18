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

import org.skriptlang.skript.lang.context.TriggerContext;

/**
 * To be implemented on objects where the addition of available context may be useful.
 * For example, obtaining the value(s) of an {@link org.skriptlang.skript.lang.expression.Expression}
 *  may be useful for providing an accurate String representation.
 */
public interface Debuggable {

	/**
	 * @param context Context surrounding the object being converted into a String.
	 * @param debug Whether additional, development-oriented information should be printed.
	 * @return A String representation of this object, using available context and debug status if applicable.
	 */
	String toString(TriggerContext context, boolean debug);

	/**
	 * Should return <tt>{@link #toString(TriggerContext, boolean) toString}({@link TriggerContext#dummy()}, false)</tt>
	 */
	@Override
	String toString();

}
