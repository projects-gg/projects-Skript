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
package org.skriptlang.skript.bukkit.chat;

import ch.njol.skript.SkriptAddon;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Comparator;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Comparators;
import ch.njol.skript.registrations.Converters;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;

public class ChatModule {

	public void register(SkriptAddon addon) {

		try {
			addon.loadClasses("org.skriptlang.skript.bukkit.chat.elements");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Converters.registerConverter(String.class, Component.class, ComponentHandler::parse);
		Converters.registerConverter(Component.class, String.class, ComponentHandler::toLegacyString);
		Comparators.registerComparator(String.class, Component.class, new Comparator<String, Component>() {
			@Override
			public Relation compare(String o1, Component o2) {
				return Relation.get(o1.equals(ComponentHandler.toLegacyString(o2)));
			}

			@Override
			public boolean supportsOrdering() {
				return false;
			}
		});

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

		ComponentHandler.registerPlaceholder("dark_cyan", "<dark_aqua>");
		ComponentHandler.registerPlaceholder("dark_turquoise", "<dark_aqua>");
		ComponentHandler.registerPlaceholder("cyan", "<dark_aqua>");

		ComponentHandler.registerPlaceholder("purple", "<dark_purple>");

		ComponentHandler.registerPlaceholder("dark_yellow", "<gold>");
		ComponentHandler.registerPlaceholder("orange", "<gold>");

		ComponentHandler.registerPlaceholder("light_grey", "<grey>");
		ComponentHandler.registerPlaceholder("light_gray", "<grey>");
		ComponentHandler.registerPlaceholder("silver", "<grey>");

		ComponentHandler.registerPlaceholder("dark_silver", "<dark_grey>");

		ComponentHandler.registerPlaceholder("light_blue", "<blue>");
		ComponentHandler.registerPlaceholder("indigo", "<blue>");

		ComponentHandler.registerPlaceholder("light_green", "<green>");
		ComponentHandler.registerPlaceholder("lime_green", "<green>");
		ComponentHandler.registerPlaceholder("lime", "<green>");

		ComponentHandler.registerPlaceholder("light_cyan", "<aqua>");
		ComponentHandler.registerPlaceholder("light_aqua", "<aqua>");
		ComponentHandler.registerPlaceholder("turquoise", "<aqua>");

		ComponentHandler.registerPlaceholder("light_red", "<red>");


		ComponentHandler.registerPlaceholder("pink", "<light_purple>");
		ComponentHandler.registerPlaceholder("magenta", "<light_purple>");

		ComponentHandler.registerPlaceholder("light_yellow", "<yellow>");

		ComponentHandler.registerPlaceholder("underline", "<underlined>");

		ComponentHandler.registerResolver(TagResolver.resolver("unicode", (argumentQueue, context) -> {
			String unicode = argumentQueue.popOr("A unicode tag must have an argument of the unicode").value();
			return Tag.selfClosingInserting(Component.text(StringEscapeUtils.unescapeJava("\\" + unicode)));
		}));

	}

}
