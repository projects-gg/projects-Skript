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
package org.skriptlang.skript.lang.changer;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.expression.Expression;

public interface ChangeableExpression<Type> extends Expression<Type> {

	Class<?> @Nullable [] acceptChange(ChangeMode mode);

	/**
	 * This method is called before this expression is set to another one.
	 * The return value is what will be used for change. You can use modified
	 * version of initial delta array or create a new one altogether
	 * <p>
	 * Default implementation will convert slots to items when they're set
	 * to variables, as specified in Skript documentation.
	 * @param changed What is about to be set.
	 * @param delta Initial delta array.
	 * @return Delta array to use for change.
	 */
	@Nullable
	default Object[] beforeChange(Expression<?> changed, @Nullable Object[] delta) {
		return beforeChangeLegacy(changed, delta);
	}

	void change(TriggerContext context, @Nullable Object[] delta, ChangeMode mode);

	static Object[] beforeChangeLegacy(Expression<?> changed, @Nullable Object[] delta) {
		// TODO this is terrible, find a way to make this method NOT default
		if (delta == null || delta.length == 0) // Nothing to nothing
			return null;

		// Slots must be transformed to item stacks when writing to variables
		// Also, some types must be cloned
		Object[] newDelta = null;
		if (changed instanceof Variable) {
			newDelta = new Object[delta.length];
			for (int i = 0; i < delta.length; i++) {
				Object value = delta[i];
				if (value instanceof Slot) {
					ItemStack item = ((Slot) value).getItem();
					if (item != null) {
						item = item.clone(); // ItemStack in inventory is mutable
					}

					newDelta[i] = item;
				} else {
					newDelta[i] = Classes.clone(delta[i]);
				}
			}
		}
		// Everything else (inventories, actions, etc.) does not need special handling

		// Return the given delta or an Object[] copy of it, with some values transformed
		return newDelta == null ? delta : newDelta;
	}

}
