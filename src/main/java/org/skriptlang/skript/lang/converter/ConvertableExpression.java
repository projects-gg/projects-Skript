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
package org.skriptlang.skript.lang.converter;

import ch.njol.skript.classes.Converter;
import ch.njol.skript.lang.util.ConvertedExpression;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.expression.Expression;

public interface ConvertableExpression<Type> extends Expression<Type> {

	/**
	 * Tries to convert this expression to the given type. This method can print an error prior to returning null to specify the cause.
	 * <p>
	 * Please note that expressions whose {@link #getReturnType() returnType} is not Object
	 *  will not be parsed at all for a certain class if there's no converter from the expression's returnType
	 *  to the desired class. Thus, this method should only be overridden if this expression's returnType is Object.
	 * <p>
	 * The returned expression should delegate this method to the original expression's method
	 *  to prevent excessive converted expression chains (see also {@link ConvertedExpression}).
	 *
	 * @param newTypes The desired return type of the returned expression
	 * @return Expression with the desired return type or null if the expression can't be converted to the given type. Returns the expression itself if it already returns the
	 *         desired type.
	 *
	 * @see Converter
	 * @see ConvertedExpression
	 */
	@Nullable
	<NewType> Expression<? extends NewType> getConvertedExpression(Class<NewType>... newTypes);

}
