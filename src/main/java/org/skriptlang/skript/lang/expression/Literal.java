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

import org.skriptlang.skript.lang.context.TriggerContext;

/**
 * Literals are constants that do not depend on {@link org.skriptlang.skript.lang.context.TriggerContext}.
 */
public interface Literal<Type> extends Expression<Type> {

	Type getSingle();

	@Override
	default Type getSingle(TriggerContext context) {
		return getSingle();
	}

	Type[] getArray();

	@Override
	default Type[] getArray(TriggerContext context) {
		return getArray();
	}

	Type[] getAll();

	@Override
	default Type[] getAll(TriggerContext context) {
		return getAll();
	}

}
