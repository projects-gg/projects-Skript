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
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Iterators;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Iterator;

@Name("Element of")
@Description({"The first, last or a random element of a set, e.g. a list variable.",
		"See also: <a href='#ExprRandom'>random</a>"})
@Examples("give a random element out of {free items::*} to the player")
@Since("2.0")
public class ExprElement extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprElement.class, Object.class, ExpressionType.PROPERTY, "(0¦[the] first|1¦[the] last|2¦[a] random|3¦%-number%(st|nd|rd|th)) element [out] of %objects%");
	}

	private enum ElementType {
		FIRST, LAST, RANDOM, ORDINAL
	}

	private ElementType type;

	private Expression<?> expr;

	@Nullable
	private Expression<Number> number;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		expr = LiteralUtils.defendExpression(exprs[1]);
		number = (Expression<Number>) exprs[0];
		type = ElementType.values()[parseResult.mark];
		return LiteralUtils.canInitSafely(expr);
	}

	@Override
	@Nullable
	protected Object[] get(Event e) {
		Iterator<?> iter = expr.iterator(e);
		if (iter == null || !iter.hasNext())
			return null;
		Object o = null;
		switch (type) {
			case FIRST:
				o = iter.next();
				break;
			case LAST:
				o = Iterators.getLast(iter);
				break;
			case ORDINAL:
				assert this.number != null;
				Number number = this.number.getSingle(e);
				if (number == null)
					return null;
				try {
					o = Iterators.get(iter, number.intValue() - 1);
				} catch (IndexOutOfBoundsException exception) {
					return null;
				}
				break;
			case RANDOM:
				Object[] allIterValues = Iterators.toArray(iter, Object.class);
				o = CollectionUtils.getRandom(allIterValues);
		}
		Object[] r = (Object[]) Array.newInstance(getReturnType(), 1);
		r[0] = o;
		return r;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		Expression<? extends R> convExpr = expr.getConvertedExpression(to);
		if (convExpr == null)
			return null;

		ExprElement exprElement = new ExprElement();
		exprElement.type = this.type;
		exprElement.expr = convExpr;
		exprElement.number = this.number;
		return (Expression<? extends R>) exprElement;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return expr.getReturnType();
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		String prefix;
		switch (type) {
			case FIRST:
				prefix = "the first";
				break;
			case LAST:
				prefix = "the last";
				break;
			case RANDOM:
				prefix = "a random";
				break;
			case ORDINAL:
				assert number != null;
				prefix = "the ";
				// Proper ordinal number
				if (number instanceof Literal) {
					Number number = ((Literal<Number>) this.number).getSingle();
					if (number == null)
						prefix += this.number.toString(e, debug) + "th";
					else
						prefix += StringUtils.fancyOrderNumber(number.intValue());
				} else {
					prefix += number.toString(e, debug) + "th";
				}
				break;
			default:
				throw new IllegalStateException();
		}
		return prefix + " element of " + expr.toString(e, debug);
	}

}
