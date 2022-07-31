package org.skriptlang.skript.lang.expression;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Checker;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.Debuggable;
import org.skriptlang.skript.lang.SyntaxElement;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Expression<Type> extends SyntaxElement, Debuggable {

	@Nullable
	Type getSingle(TriggerContext context);

	default Optional<Type> getOptionalSingle(TriggerContext context) {
		return Optional.ofNullable(getSingle(context));
	}

	Type[] getArray(TriggerContext context);

	Type[] getAll(TriggerContext context);

	default Stream<? extends Type> stream(TriggerContext context) {
		Iterator<? extends Type> iterator = iterator(context);
		if (iterator == null)
			return Stream.empty();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	boolean isSingle();

	boolean check(TriggerContext context, Checker<? super Type> checker, boolean negated);

	boolean check(TriggerContext context, Checker<? super Type> checker);

	@Nullable
	<NewType> Expression<? extends NewType> getConvertedExpression(Class<NewType>... newTypes);

	Class<? extends Type> getReturnType();

	boolean getAnd();

	boolean setTime(int time);

	int getTime();

	boolean isDefault();

	@Nullable
	Iterator<? extends Type> iterator(TriggerContext context);

	Expression<?> getSource();

	Expression<? extends Type> simplify();

	// Changer Methods

	@Nullable
	Class<?>[] acceptChange(ChangeMode mode);

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

	void change(TriggerContext context, @Nullable Object[] delta, ChangeMode mode);

}
