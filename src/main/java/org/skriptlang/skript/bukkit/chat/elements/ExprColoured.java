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
package org.skriptlang.skript.bukkit.chat.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Coloured / Uncoloured")
@Description({
		"Parses &lt;colour&gt;s and, optionally, chat styles in a message or removes",
		"any colours <i>and</i> chat styles from the message. Parsing all",
		"chat styles requires this expression to be used in same line with",
		"the <a href=effects.html#EffSend>send effect</a>."
})
@Examples({
		"on chat:",
		"\tset message to coloured message # Safe; only colors get parsed",
		"command /fade &lt;player&gt;:",
		"\ttrigger:",
		"\t\tset display name of the player-argument to uncoloured display name of the player-argument",
		"command /format &lt;text&gt;:",
		"\ttrigger:",
		"\t\tmessage formatted text-argument # Safe, because we're sending to whoever used this command"
})
@Since("2.0")
public class ExprColoured extends SimpleExpression<Component> {

	static {
		Skript.registerExpression(ExprColoured.class, Component.class, ExpressionType.COMBINED,
				"(colo[u]r-|colo[u]red )%strings%",
				"(format-|formatted )%strings%",
				"(un|non)[-](colo[u]r-|colo[u]red |1¦format-|1¦formatted )%strings%"
		);
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<String> strings;
	
	// Whether colors should be parsed
	boolean color;
	// Whether all formatting should be parsed
	boolean format;
	// Whether all formatting should be removed
	boolean unformatted;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		strings = (Expression<String>) exprs[0];
		color = matchedPattern <= 1; // colored and formatted
		format = matchedPattern == 1;
		unformatted = parseResult.mark == 1;
		return true;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	protected Component[] get(Event e) {
		long start = System.nanoTime();
		List<Component> components = new ArrayList<>();

		Expression<String>[] expressions = strings instanceof ExpressionList ?
			(Expression<String>[]) ((ExpressionList<String>) strings).getExpressions() : new Expression[]{strings};

		for (Expression<String> expr : expressions) {
			if (expr instanceof VariableString) { // Avoid unnecessary parsing
				// Although a VariableString may already be parsed into a component, we have to do this again because it's colored.
				components.add(getComponent(((VariableString) expr).toString(false, e)));
			} else {
				for (String string : expr.getArray(e))
					components.add(getComponent(string));
			}
		}
		System.out.println("Finished Coloring: " + (1. * (System.nanoTime() - start) / 1000000.));

		return components.toArray(new Component[0]);
	}

	private Component getComponent(String string) {
		return color ? ComponentHandler.parse(string, !format) : ComponentHandler.plain(ComponentHandler.stripFormatting(string, unformatted));
	}

	@Override
	public boolean isSingle() {
		return strings.isSingle();
	}

	@Override
	public Class<? extends Component> getReturnType() {
		return Component.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (format)
			return "formatted " + strings.toString(e, debug);
		return (color ? "" : "un") + "coloured " + strings.toString(e, debug);
	}

}
