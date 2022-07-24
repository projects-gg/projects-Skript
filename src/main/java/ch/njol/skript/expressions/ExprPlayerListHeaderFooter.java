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

@Name("Player List Header and Footer")
@Description("The message above and below the player list in the tab menu.")
@Examples({
		"set all players' tab list header to \"Welcome to the Server!\"",
		"send \"%the player's tab list header%\" to player",
		"reset all players' tab list header"
})
@Since("2.4")
public class ExprPlayerListHeaderFooter extends SimplePropertyExpression<Player, Component> {

	static {
		PropertyExpression.register(ExprPlayerListHeaderFooter.class, Component.class,
			"(player|tab)[ ]list (header|:footer) [(text|message)]", "players"
		);
	}

	private boolean footer;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		footer = parseResult.hasTag("footer");
		return super.init(exprs, matchedPattern, isDelayed, parseResult);
	}

	@Override
	@Nullable
	public Component convert(Player player) {
		return footer ? player.playerListFooter() : player.playerListHeader();
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case DELETE:
			case RESET:
				return CollectionUtils.array(Component[].class);
			default:
				return null;
		}
	}

	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		Component component = Component.empty();
		for (Object userComponent : delta)
			component = component.append((Component) userComponent).append(Component.newline());

		Audience audience = Audience.audience(getExpr().getArray(e));
		if (footer) {
			audience.sendPlayerListFooter(component);
		} else {
			audience.sendPlayerListHeader(component);
		}
	}

	@Override
	public Class<? extends Component> getReturnType() {
		return Component.class;
	}

	@Override
	protected String getPropertyName() {
		return "player list " + (footer ? "header" : "footer");
	}

}
