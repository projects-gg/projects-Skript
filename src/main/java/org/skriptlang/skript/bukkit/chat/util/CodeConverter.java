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
package org.skriptlang.skript.bukkit.chat.util;

import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

class CodeConverter {

	private static final Map<Character, String> CODE_MAP = new HashMap<>();

	static {
		CODE_MAP.put('0', "black");
		CODE_MAP.put('1', "dark_blue");
		CODE_MAP.put('2', "dark_green");
		CODE_MAP.put('3', "dark_aqua");
		CODE_MAP.put('4', "dark_red");
		CODE_MAP.put('5', "dark_purple");
		CODE_MAP.put('6', "gold");
		CODE_MAP.put('7', "gray");
		CODE_MAP.put('8', "dark_gray");
		CODE_MAP.put('9', "blue");
		CODE_MAP.put('a', "green");
		CODE_MAP.put('b', "aqua");
		CODE_MAP.put('c', "red");
		CODE_MAP.put('d', "light_purple");
		CODE_MAP.put('e', "yellow");
		CODE_MAP.put('f', "white");

		CODE_MAP.put('o', "italic");
		CODE_MAP.put('l', "bold");
		CODE_MAP.put('m', "strikethrough");
		CODE_MAP.put('n', "underlined");
		CODE_MAP.put('k', "obfuscated");
	}

	@Nullable
	public static String getColor(char code) {
		return CODE_MAP.get(code);
	}

}
