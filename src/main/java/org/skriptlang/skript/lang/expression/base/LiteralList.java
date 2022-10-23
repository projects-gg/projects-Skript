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
package org.skriptlang.skript.lang.expression.base;

import ch.njol.skript.util.Utils;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.converter.ConvertableExpression;
import org.skriptlang.skript.lang.expression.Expression;
import org.skriptlang.skript.lang.expression.Literal;

public class LiteralList<Type> extends ExpressionList<Type> implements Literal<Type> {

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, boolean and) {
		super(literals, returnType, and);
	}

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, boolean and, LiteralList<?> source) {
		super(literals, returnType, and, source);
	}

	@Override
	public Literal<? extends Type>[] getExpressions() {
		return (Literal<? extends Type>[]) super.getExpressions();
	}

	@Override
	public Type getSingle() {
		return super.getSingle(TriggerContext.dummy());
	}

	@Override
	public Type[] getArray() {
		return super.getArray(TriggerContext.dummy());
	}

	@Override
	public Type[] getAll() {
		return super.getAll(TriggerContext.dummy());
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <NewType> LiteralList<? extends NewType> getConvertedExpression(Class<NewType>... newTypes) {
		Literal<? extends NewType>[] literals = new Literal[expressions.length];
		Class<?>[] classes = new Class[expressions.length];
		for (int i = 0; i < literals.length; i++) {
			Expression<? extends Type> expression = expressions[i];
			if (!(expression instanceof ConvertableExpression))
				return null;

			Literal<? extends NewType> convertedExpression =
				(Literal<? extends NewType>) ((ConvertableExpression<?>) expression).getConvertedExpression(newTypes);
			if (convertedExpression == null)
				return null;

			literals[i] = convertedExpression;
		}
		return new LiteralList<>(literals, (Class<NewType>) Utils.getSuperType(classes), getAnd(), this);
	}

	@Override
	public LiteralList<?> getSource() {
		return (LiteralList<?>) super.getSource();
	}

}
