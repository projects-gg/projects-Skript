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
package io.skriptlang.skript.chat;

import ch.njol.skript.SkriptAddon;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import io.skriptlang.skript.chat.util.ComponentHandler;
import net.kyori.adventure.text.Component;

import java.io.IOException;

import static io.skriptlang.skript.chat.util.ComponentHandler.registerPlaceholder;

public class ChatRegistration {

	public void register(SkriptAddon addon) {

		try {
			addon.loadClasses("io.skriptlang.skript.chat.elements");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Converters.registerConverter(String.class, Component.class, ComponentHandler::parse);
		Converters.registerConverter(Component.class, String.class, ComponentHandler::toLegacyString);

		Classes.registerClass(new ClassInfo<>(Component.class, "component")
			.user("components?")
			.name("Component")
			.since("INSERT VERSION")
			.parser(new Parser<Component>() {
				@Override
				public boolean canParse(ParseContext context) {
					return false;
				}

				@Override
				public String toString(Component component, int flags) {
					return ComponentHandler.toLegacyString(component);
				}

				@Override
				public String toVariableNameString(Component component) {
					return "component:" + component;
				}
			})
		);

		// Just to initialize it now
		ComponentHandler.getAdventure();

		registerPlaceholder("dark_cyan", "<dark_aqua>");
		registerPlaceholder("dark_turquoise", "<dark_aqua>");
		registerPlaceholder("dark cyan", "<dark_aqua>");
		registerPlaceholder("dark turquoise", "<dark_aqua>");
		registerPlaceholder("cyan", "<dark_aqua>");

		registerPlaceholder("purple", "<dark_purple>");

		registerPlaceholder("dark_yellow", "<gold>");
		registerPlaceholder("dark yellow", "<gold>");
		registerPlaceholder("orange", "<gold>");

		registerPlaceholder("light_grey", "<grey>");
		registerPlaceholder("light_gray", "<grey>");
		registerPlaceholder("light grey", "<grey>");
		registerPlaceholder("light gray", "<grey>");
		registerPlaceholder("silver", "<grey>");

		registerPlaceholder("dark_silver", "<dark_grey>");
		registerPlaceholder("dark silver", "<dark_grey>");

		registerPlaceholder("light_blue", "<blue>");
		registerPlaceholder("light blue", "<blue>");
		registerPlaceholder("indigo", "<blue>");

		registerPlaceholder("light_green", "<green>");
		registerPlaceholder("lime_green", "<green>");
		registerPlaceholder("light green", "<green>");
		registerPlaceholder("lime green", "<green>");
		registerPlaceholder("lime", "<green>");

		registerPlaceholder("light_cyan", "<aqua>");
		registerPlaceholder("light_aqua", "<aqua>");
		registerPlaceholder("light cyan", "<aqua>");
		registerPlaceholder("light aqua", "<aqua>");
		registerPlaceholder("turquoise", "<aqua>");

		registerPlaceholder("light_red", "<red>");
		registerPlaceholder("light red", "<red>");

		registerPlaceholder("pink", "<light_purple>");
		registerPlaceholder("magenta", "<light_purple>");

		registerPlaceholder("light_yellow", "<yellow>");
		registerPlaceholder("light yellow", "<yellow>");

		registerPlaceholder("underline", "<underlined>");

		registerPlaceholder(tag -> {
			if (tag.startsWith("unicode:u") && tag.length() == 13) {
				return Component.text(("\\" + tag.substring(8)).toCharArray()[0]);
			}
			return null;
		});

	}

}
