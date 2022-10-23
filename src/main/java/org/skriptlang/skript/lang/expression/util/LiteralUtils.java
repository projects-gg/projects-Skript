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
package org.skriptlang.skript.lang.expression.util;

import java.util.stream.Stream;

import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.util.Utils;
import org.skriptlang.skript.lang.expression.Expression;
import org.skriptlang.skript.lang.expression.Literal;
import org.skriptlang.skript.lang.expression.base.ExpressionList;

/**
 * A class that contains methods based around making it easier to deal with {@link UnparsedLiteral} objects.
 */
// TODO UnparsedLiteral replacement
public class LiteralUtils {

	/**
	 * Checks an {@link Expression} for {@link UnparsedLiteral} objects and converts them if found.
	 * @param expr The expression to check for {@link UnparsedLiteral} objects.
	 * @return The passed expression without {@link UnparsedLiteral} objects.
	 */
	@SuppressWarnings("unchecked")
	public static <Type> Expression<Type> defendExpression(Expression<?> expr) {
		if (expr instanceof ExpressionList) {
			Expression<?>[] oldExpressions = ((ExpressionList<?>) expr).getExpressions();

			Expression<? extends Type>[] newExpressions = new Expression[oldExpressions.length];
			Class<?>[] returnTypes = new Class[oldExpressions.length];

			for (int i = 0; i < oldExpressions.length; i++) {
				newExpressions[i] = LiteralUtils.defendExpression(oldExpressions[i]);
				returnTypes[i] = newExpressions[i].getReturnType();
			}

			return new ExpressionList<>(newExpressions, (Class<Type>) Utils.getSuperType(returnTypes), ((ExpressionList<?>) expr).getAnd());
		} else if (expr instanceof UnparsedLiteral) {
			Literal<?> parsedLiteral = ((UnparsedLiteral) expr).getConvertedExpression(Object.class);
			return (Expression<Type>) (parsedLiteral == null ? expr : parsedLiteral);
		}
		return (Expression<Type>) expr;
	}

	/**
	 * Checks if an Expression contains {@link UnparsedLiteral} objects.
	 * @param expr The Expression to check for {@link UnparsedLiteral} objects.
	 * @return Whether the passed expressions contains {@link UnparsedLiteral} objects.
	 */
	public static boolean hasUnparsedLiteral(Expression<?> expr) {
		if (expr instanceof UnparsedLiteral) {
			return true;
		} else if (expr instanceof ExpressionList) {
			return Stream.of(((ExpressionList<?>) expr).getExpressions())
				.anyMatch(e -> e instanceof UnparsedLiteral);
		}
		return false;
	}

	/**
	 * Checks if the passed Expressions are non-null and do not contain {@link UnparsedLiteral} objects.
	 * @param expressions The expressions to check for {@link UnparsedLiteral} objects.
	 * @return Whether the passed expressions contain {@link UnparsedLiteral} objects.
	 */
	public static boolean canInitSafely(Expression<?>... expressions) {
		for (Expression<?> expression : expressions) {
			if (expression == null || hasUnparsedLiteral(expression))
				return false;
		}
		return true;
	}

}
