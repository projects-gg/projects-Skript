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

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.changer.ChangeableExpression;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.converter.ConvertableExpression;
import org.skriptlang.skript.lang.expression.Expression;
import org.skriptlang.skript.lang.expression.ListExpression;
import org.skriptlang.skript.lang.expression.SimplifiableExpression;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ExpressionList<Type> implements
	Expression<Type>, ListExpression<Type>, ConvertableExpression<Type>,
	ChangeableExpression<Type>, SimplifiableExpression<Type> {

	@Nullable
	private final ExpressionList<?> source;

	protected final Expression<? extends Type>[] expressions;

	private final Class<Type> returnType;

	private boolean and, single;

	public ExpressionList(Expression<? extends Type>[] expressions, Class<Type> returnType, boolean and) {
		this(expressions, returnType, and, null);
	}

	protected ExpressionList(
		Expression<? extends Type>[] expressions, Class<Type> returnType, boolean and, @Nullable ExpressionList<?> source
	) {
		this.source = source;
		this.expressions = expressions;
		this.returnType = returnType;
		setAnd(and);
	}

	/**
	 * @return The internal list of expressions. Can be modified with care.
	 */
	public Expression<? extends Type>[] getExpressions() {
		return expressions;
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException("ExpressionLists cannot be initialized");
	}

	@Override
	@Nullable
	public Type getSingle(TriggerContext context) {
		if (!single)
			throw new UnsupportedOperationException("Cannot call getSingle() on non-single ExpressionList");
		Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
		return expression != null ? expression.getSingle(context) : null;
	}

	@Override
	public Type[] getArray(TriggerContext context) {
		if (and)
			return getAll(context);
		Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
		//noinspection unchecked
		return expression != null ? expression.getArray(context) : (Type[]) Array.newInstance(returnType, 0);
	}

	@Override
	public Type[] getAll(TriggerContext context) {
		List<Type> values = new ArrayList<>();
		for (Expression<? extends Type> expression : expressions)
			values.addAll(Arrays.asList(expression.getAll(context)));
		//noinspection unchecked
		return values.toArray((Type[]) Array.newInstance(returnType, 0));
	}

	@Override
	@Nullable
	public Iterator<? extends Type> iterator(TriggerContext context) {
		if (!and) {
			Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
			return expression != null ? expression.iterator(context) : null;
		}
		return new Iterator<Type>() {
			private int i = 0;
			@Nullable
			private Iterator<? extends Type> current = null;

			@Override
			public boolean hasNext() {
				while (i < expressions.length && (current == null || !current.hasNext()))
					current = expressions[i++].iterator(context);
				return current != null && current.hasNext();
			}

			@Override
			public Type next() {
				if (!hasNext())
					throw new NoSuchElementException();
				if (current == null)
					throw new NoSuchElementException();
				Type value = current.next();
				assert value != null : current;
				return value;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean isSingle() {
		return single;
	}

	@Override
	public boolean check(TriggerContext context, Checker<? super Type> checker, boolean negated) {
		return negated ^ check(context, checker);
	}

	@Override
	public boolean check(TriggerContext context, Checker<? super Type> checker) {
		for (Expression<? extends Type> expression : expressions) {
			boolean b = expression.check(context, checker);
			if (and && !b)
				return false;
			if (!and && b)
				return true;
		}
		return and;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <NewType> ExpressionList<? extends NewType> getConvertedExpression(Class<NewType>... newTypes) {
		Expression<? extends NewType>[] convertedExpressions = new Expression[expressions.length];
		for (int i = 0; i < convertedExpressions.length; i++) {
			Expression<? extends Type> expression = expressions[i];
			if (!(expression instanceof ConvertableExpression))
				return null;

			Expression<? extends NewType> convertedExpression =
				((ConvertableExpression<?>) expression).getConvertedExpression(newTypes);
			if (convertedExpression == null)
				return null;

			convertedExpressions[i] = convertedExpression;
		}
		return new ExpressionList<>(convertedExpressions, (Class<NewType>) Utils.getSuperType(newTypes), and, this);
	}

	@Override
	public Class<Type> getReturnType() {
		return returnType;
	}

	public void setAnd(boolean and) {
		this.and = and;
		if (!and) {
			boolean single = true;
			for (Expression<?> expression : expressions) { // Not single if at least one expression is not single
				if (!expression.isSingle()) {
					single = false;
					break;
				}
			}
			this.single = single;
		} else {
			single = false; // Can't be single if 'and' is true
		}
	}

	@Override
	public boolean getAnd() {
		return and;
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		for (Expression<?> expression : expressions) {
			if (!(expression instanceof ChangeableExpression))
				return null;
		}

		Class<?>[] exprClasses = ((ChangeableExpression<?>) expressions[0]).acceptChange(mode);
		if (exprClasses == null) // No changes can be made
			return null;

		ArrayList<Class<?>> acceptedClasses = new ArrayList<>(Arrays.asList(exprClasses));
		for (int i = 1; i < expressions.length; i++) {
			exprClasses = ((ChangeableExpression<?>) expressions[i]).acceptChange(mode);
			if (exprClasses == null)
				return null;

			acceptedClasses.retainAll(Arrays.asList(exprClasses));
			if (acceptedClasses.isEmpty())
				return null;
		}

		return acceptedClasses.toArray(new Class[0]);
	}

	@Override
	public void change(TriggerContext context, Object @Nullable [] delta, ChangeMode mode) {
		for (Expression<?> expression : expressions)
			((ChangeableExpression<?>) expression).change(context, delta, mode);
	}

	@Override
	public Expression<?> getSource() {
		return source != null ? source : this;
	}

	@Override
	public String toString(TriggerContext context, boolean debug) {
		StringBuilder builder = new StringBuilder("(");

		for (int i = 0; i < expressions.length; i++) {
			if (i != 0) {
				if (i == expressions.length - 1) {
					builder.append(and ? " and " : " or ");
				} else {
					builder.append(", ");
				}
			}
			builder.append(expressions[i].toString(context, debug));
		}
		builder.append(")");

		if (debug)
			builder.append("[").append(returnType).append("]");

		return builder.toString();
	}

	@Override
	public String toString() {
		return toString(TriggerContext.dummy(), false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<? extends Type> simplify() {
		boolean isLiteralList = true;
		boolean isSimpleList = true;

		for (int i = 0; i < expressions.length; i++) {
			if (expressions[i] instanceof SimplifiableExpression)
				expressions[i] = ((SimplifiableExpression<? extends Type>) expressions[i]).simplify();
			isLiteralList &= expressions[i] instanceof Literal;
			isSimpleList &= expressions[i].isSingle();
		}

		if (isLiteralList && isSimpleList) {
			Type[] values = (Type[]) Array.newInstance(returnType, expressions.length);
			for (int i = 0; i < values.length; i++)
				values[i] = ((Literal<? extends Type>) expressions[i]).getSingle();
			return new SimpleLiteral<>(values, returnType, and);
		}

		if (isLiteralList) {
			Literal<? extends Type>[] literals = Arrays.copyOf(expressions, expressions.length, Literal[].class);
			return new LiteralList<>(literals, returnType, and);
		}

		return this;
	}

}
