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

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;

@Name("Player List Header and Footer")
@Description("The message above and below the player list in the tab menu.")
@Examples({
		"set all players' tab list header to \"Welcome to the Server!\"",
		"send \"%the player's tab list header%\" to player",
		"reset all players' tab list header"
})
@Since("2.4")
public class ExprPlayerListHeaderFooter extends SimplePropertyExpression<Player, String> {

	static {
		PropertyExpression.register(ExprPlayerListHeaderFooter.class, String.class, "(player|tab)[ ]list (header|1¦footer) [(text|message)]", "players");
	}

	private boolean isHeader;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		isHeader = parseResult.mark == 0;
		return super.init(exprs, matchedPattern, isDelayed, parseResult);
	}

	@Override
	@Nullable
	public String convert(Player player) {
		return isHeader ? player.getPlayerListHeader() : player.getPlayerListFooter();
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case DELETE:
			case RESET:
				return CollectionUtils.array(String[].class, Component.class);
			default:
				return null;
		}
	}

	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		Component component;
		if (delta == null) {
			component = Component.empty();
		} else if (delta[0] instanceof Component) {
			component = (Component) delta[0];
		} else {
			component = ComponentHandler.parse(String.join("\n", (String[]) delta), false);
		}
		Audience audience = ComponentHandler.audienceFrom(getExpr().getArray(e));
		if (isHeader) {
			audience.sendPlayerListHeader(component);
		} else {
			audience.sendPlayerListFooter(component);
		}
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	protected String getPropertyName() {
		return "player list " + (isHeader ? "header" : "footer");
	}

}
