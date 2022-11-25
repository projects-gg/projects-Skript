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
package ch.njol.skript.effects;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.structure.StructureManager;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;

@Name("Structure Place Settings")
@Description("Unregisters or saves a structure by it's namespace key.")
@Examples("unregister structure named \"Example\"")
@RequiredPlugins("Minecraft 1.17.1+")
@Since("INSERT VERSION")
public class EffStructureSaveUnregister extends Effect {

	static {
		Skript.registerEffect(EffStructureSaveUnregister.class, "(:save|(delete|unregister)) structure[s] [named] %strings%");
	}

	private Expression<String> names;
	private boolean save;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		names = (Expression<String>) exprs[0];
		save = parseResult.hasTag("save");
		return true;
	}

	@Override
	protected void execute(Event event) {
		StructureManager manager = Bukkit.getStructureManager();
		for (String name : names.getArray(event)) {
			NamespacedKey key = Utils.getNamespacedKey(name);
			if (key == null)
				continue;
			if (save) {
				manager.saveStructure(key);
			} else {
				try {
					manager.deleteStructure(key);
				} catch (IOException e) {
					Skript.error("Failed to save structure " + name);
					if (Skript.debug())
						e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return save ? "save " : "delete " + " structures " + names.toString(event, debug);
	}

}
