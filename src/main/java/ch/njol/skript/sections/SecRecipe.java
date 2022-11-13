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
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterables;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SecRecipe extends Section {

	static {
		Skript.registerSection(SecRecipe.class, "(create|add|register) [a] [crafting] recipe for %itemtype% with [the] key %string%");
		Skript.registerEffect(EffRecipeLine.class, "%itemtypes%");
	}

	private Expression<String> key;
	private Expression<ItemType> result;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
		result = (Expression<ItemType>) exprs[0];
		key = (Expression<String>) exprs[1];
		loadCode(sectionNode);
		if (first == null) {
			Skript.error("A recipe section must contain 3 recipe lines");
			return false;
		}
		TriggerItem current = first;
		for (int i = 0; i < 3; i++) {
			if (current == null || current.getClass() != EffRecipeLine.class) {
				Skript.error("A recipe section may only contain 3 recipe lines - 1");
				return false;
			}
			current = current.getNext();
		}
		if (Iterables.size(sectionNode) != 3) {
			Skript.error("A recipe section may only contain 3 recipe lines- 2");
			return false;
		}
		return true;
	}

	private ItemType[] getAllIngredients(Event event) {
		EffRecipeLine firstLine = (EffRecipeLine) first;
		EffRecipeLine secondLine = (EffRecipeLine) firstLine.getNext();
		EffRecipeLine thirdLine = (EffRecipeLine) secondLine.getNext();
		return Stream.of(firstLine.getIngredients(event), secondLine.getIngredients(event), thirdLine.getIngredients(event))
			.flatMap(Arrays::stream)
			.toArray(ItemType[]::new);
	}

	@Override
	public String toString(Event event, boolean debug) {
		return "create crafting recipe for " + result.toString(event, debug) + " with key " + key.toString(event, debug);
	}

	@Override
	protected TriggerItem walk(Event event) {
		execute(event);
		return walk(event, false);
	}

	protected void execute(Event event) {
		String key = this.key.getSingle(event);
		if (key == null)
			return;
		ItemType result = this.result.getSingle(event);
		if (result == null)
			return;
		ItemType[] ingredients = getAllIngredients(event);
		if (ingredients.length != 9)
			return;
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(Skript.getInstance(), key), result.getRandom());
		recipe.shape("abc", "def", "ghi");
		for (char c = 'a'; c < 'j'; c++) {
			recipe.setIngredient(c, ingredients[c - 'a'].getMaterial());
		}
		try {
			Bukkit.getServer().addRecipe(recipe);
		} catch (IllegalStateException ignored) {
			// Bukkit throws a IllegalStateException if a duplicate recipe is registered
		}
	}


	public static class EffRecipeLine extends Effect {

		private Expression<ItemType> ingredients;

		@SuppressWarnings("unchecked")
		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
			ingredients = (Expression<ItemType>) exprs[0];
			if (!(ingredients instanceof ExpressionList)) {
				Skript.error("A recipe line may only contain a list of items");
				return false;
			}
			if (((ExpressionList<ItemType>) ingredients).getExpressions().length != 3) {
				Skript.error("An recipe line may only contain three items");
			}
			List<TriggerSection> currentSections = getParser().getCurrentSections();
			if (currentSections.isEmpty())
				return false;
			return currentSections.get(currentSections.size() - 1).getClass() == SecRecipe.class;
		}

		@Override
		protected void execute(Event event) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return ingredients.toString(event, debug);
		}

		public ItemType[] getIngredients(Event event) {
			return ingredients.getArray(event);
		}
	}

}
