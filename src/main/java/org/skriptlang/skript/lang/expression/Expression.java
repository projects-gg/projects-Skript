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

import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Checker;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.SyntaxElement;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Expression<Type> extends SyntaxElement {

	/**
	 * A method to obtain the singular value of this Expression.
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @return The value or null in scenarios where a call to @link #getArray(TriggerContext)} would result
	 *  in an empty array being returned.
	 * @throws UnsupportedOperationException May occur if this method was called on a non-single expression.
	 * If unknown, this should be checked using {@link #isSingle()}.
	 */
	@Nullable
	Type getSingle(TriggerContext context);

	/**
	 * @return The result of {@link #getSingle(TriggerContext)} contained within an Optional.
	 */
	default Optional<Type> getOptionalSingle(TriggerContext context) {
		return Optional.ofNullable(getSingle(context));
	}

	/**
	 * A method to obtain all values of this Expression.
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @return All values or an empty array if no values could be successfully obtained using the provided context.
	 */
	Type[] getArray(TriggerContext context);

	/**
	 * Gets all possible return values of this expression, i.e. it returns the same as
	 *  {@link #getArray(TriggerContext)} if {@link #getAnd()} is true,
	 *  otherwise all possible values for {@link #getSingle(TriggerContext)}.
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @return An array of all possible values of this expression for the given event which must
	 *  neither be null nor contain nulls, and which must not be an internal array.
	 */
	Type[] getAll(TriggerContext context);

	/**
	 * Gets a non-null stream of this expression's values.
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @return A non-null stream of this expression's values
	 */
	default Stream<? extends Type> stream(TriggerContext context) {
		Iterator<? extends Type> iterator = iterator(context);
		if (iterator == null)
			return Stream.empty();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	/**
	 * @return true if this expression will ever only return one value at most,
	 *  false if it can return multiple values.
	 */
	boolean isSingle();

	/**
	 * Checks this expression against the given checker. This is the normal version of this method
	 *  and the one which must be used for simple checks, or as the innermost check of nested checks.
	 *
	 * <p>
	 * Usual implementation (may differ, e.g. may return false for nonexistent values independent of <tt>negated</tt>):
	 * </p>
	 * <code>
	 * return negated ^ {@link #check(TriggerContext, Checker)};
	 * </code>
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @param checker A checker
	 * @param negated The checking condition's negated state. This is used to invert the output of the checker if set to true (i.e. <tt>negated ^ checker.check(...)</tt>)
	 * @return Whether this expression matches or doesn't match the given checker depending on the condition's negated state.
	 * @see SimpleExpression#check(Object[], Checker, boolean, boolean)
	 */
	boolean check(TriggerContext context, Checker<? super Type> checker, boolean negated);

	/**
	 * Checks this expression against the given checker. This method must only be used around other checks,
	 *  use {@link #check(TriggerContext, Checker, boolean)} for a simple check
	 *  or the innermost check of a nested check.
	 *
	 * @param context Context surrounding the execution of the
	 * {@link ch.njol.skript.lang.Statement} this expression is a part of.
	 * @param checker A checker
	 * @return Whether this expression matches the given checker.
	 * @see SimpleExpression#check(Object[], Checker, boolean, boolean)
	 */
	boolean check(TriggerContext context, Checker<? super Type> checker);

	/**
	 * Gets the return type of this expression.
	 *
	 * @return A supertype of any objects returned by {@link #getSingle(TriggerContext)}
	 *  and the component type of any arrays returned by {@link #getArray(TriggerContext)}.
	 */
	Class<? extends Type> getReturnType();

	@Nullable
	Iterator<? extends Type> iterator(TriggerContext context);

	Expression<?> getSource();

}
