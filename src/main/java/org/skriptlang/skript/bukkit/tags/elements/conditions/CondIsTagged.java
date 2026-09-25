package org.skriptlang.skript.bukkit.tags.elements.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.conditions.base.PropertyCondition.PropertyType;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.SkriptTag;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Name("Is Tagged")
@Description({
	"Checks whether an item, block, entity, or entitydata is tagged with the given tag."
})
@Example("""
	if player's tool is tagged with minecraft tag "enchantable/sharp_weapon":
		enchant player's tool with sharpness 1
	""")
@Example("if all logs are tagged with tag \"minecraft:logs\"")
@Since("2.10")
@Keywords({"blocks", "minecraft tag", "type", "category"})
public class CondIsTagged extends Condition {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.CONDITION,
			PropertyCondition.infoBuilder(
				CondIsTagged.class,
				PropertyType.BE,
				"tagged (as|with) %minecrafttags%",
				"itemtypes/entities/entitydatas"
			)
				.supplier(CondIsTagged::new)
				.build()
			);
	}

	private Expression<Tag<Keyed>> tags;
	private Expression<?> elements;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.elements = expressions[0];
		//noinspection unchecked
		this.tags = (Expression<Tag<Keyed>>) expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		Tag<Keyed>[] tags = this.tags.getAll(event);
		if (tags.length == 0)
			return isNegated();
		boolean and = this.tags.getAnd();
 		return elements.check(event, element -> {
			boolean isAny = (element instanceof ItemType itemType && !itemType.isAll());
			Keyed[] values = TagModule.getKeyed(element);
			if (values == null || values.length == 0)
				return false;

			Class<? extends Keyed> valueClass = values[0].getClass();

			for (Tag<Keyed> tag : tags) {
				// ensure the tag is the same type as the values
				Class<?> tagValueClass = getValueClass(tag);
				if (tagValueClass == null || !tagValueClass.isAssignableFrom(valueClass))
					return false;
				 if (isTagged(tag, values, !isAny)) {
					 if (!and)
						 return true;
				 } else if (and) {
					 return false;
				 }
			 }
			return and;
		}, isNegated());
	}

	/**
	 * The class of the values of every non-{@link SkriptTag} tag checked so far, keyed by tag class and then tag key.
	 * Bukkit hands out a new tag object on every lookup and some implementations rebuild their whole value set on
	 * every {@link Tag#getValues()} call, so the class is resolved once per tag class and key instead of per check.
	 * Custom Skript tags are skipped: one key may be used by tags of different types, and their value sets are cheap.
	 */
	private static final Map<Class<?>, Map<NamespacedKey, Class<?>>> TAG_VALUE_CLASSES = new ConcurrentHashMap<>();

	/**
	 * @return The class of the values of the given tag, or null if the tag has no values.
	 */
	private static @Nullable Class<?> getValueClass(Tag<Keyed> tag) {
		if (tag instanceof SkriptTag)
			return getFirstValueClass(tag);
		Map<NamespacedKey, Class<?>> classesByKey = TAG_VALUE_CLASSES.get(tag.getClass());
		if (classesByKey == null)
			classesByKey = TAG_VALUE_CLASSES.computeIfAbsent(tag.getClass(), tagClass -> new ConcurrentHashMap<>());
		NamespacedKey key = tag.getKey();
		Class<?> valueClass = classesByKey.get(key);
		if (valueClass == null) {
			valueClass = getFirstValueClass(tag);
			if (valueClass != null)
				classesByKey.putIfAbsent(key, valueClass);
		}
		return valueClass;
	}

	private static @Nullable Class<?> getFirstValueClass(Tag<Keyed> tag) {
		Iterator<Keyed> values = tag.getValues().iterator();
		return values.hasNext() ? values.next().getClass() : null;
	}

	/**
	 * Helper method for checking if a series of values have a tag.
	 * @param tag The tag to check for.
	 * @param values The values to check against.
	 * @param allTagged Whether all values need to have the tag (true), or just one (false).
	 * @return Whether the values are tagged with the tag.
	 */
	private boolean isTagged(Tag<Keyed> tag, Keyed @NotNull [] values, boolean allTagged) {
		for (Keyed value : values) {
			if (tag.isTagged(value)) {
				if (!allTagged)
					return true;
			} else if (allTagged) {
				return false;
			}
		}
		return allTagged;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return PropertyCondition.toString(this, PropertyType.BE, event, debug, elements,
				" tagged as " + tags.toString(event, debug));
	}

}
