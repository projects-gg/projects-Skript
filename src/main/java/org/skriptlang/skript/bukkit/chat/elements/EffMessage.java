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
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

@Name("Send Message")
@Description({
		"Sends a message to the given player. Only styles written",
		"in given string or in <a href=expressions.html#ExprColoured>formatted expressions</a> will be parsed.",
		"Adding an optional sender allows the messages to be sent as if a specific player sent them.",
		"This is useful with Minecraft 1.16.4's new chat ignore system, in which players can choose to ignore other players,",
		"but for this to work, the message needs to be sent from a player."
})
@Examples({
		"message \"A wild %player% appeared!\"",
		"message \"This message is a distraction. Mwahaha!\"",
		"send \"Your kill streak is %{kill streak::%uuid of player%}%.\" to player",
		"if the targeted entity exists:",
		"\tmessage \"You're currently looking at a %type of the targeted entity%!\"",
		"on chat:",
		"\tcancel event",
		"\tsend \"[%player%] >> %message%\" to all players from player"
})
@RequiredPlugins("Minecraft 1.16.4+ for optional sender")
@Since("1.0, 2.2-dev26 (advanced features), 2.5.2 (optional sender), 2.6 (sending objects)")
public class EffMessage extends Effect {

	static {
		Skript.registerEffect(EffMessage.class,
			"(message|send [the] [message[s]]) %objects% [to %commandsenders%] [from %-player%]",
			"send [the] action bar [with text] %objects% [to %commandsenders%]"
		);
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?>[] messages;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?> messageExpr;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<CommandSender> recipients;

	@Nullable
	private Expression<Player> sender;

	boolean isChatMessage;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		isChatMessage = matchedPattern == 0;

		messageExpr = LiteralUtils.defendExpression(exprs[0]);
		if (messageExpr instanceof ExpressionList) {
			ExpressionList<?> exprList = (ExpressionList<?>) messageExpr;
			if (exprList.getAnd()) {
				messages = exprList.getExpressions();
			} else {
				messages = new Expression[]{CollectionUtils.getRandom(exprList.getExpressions())};
			}
		} else {
			messages = new Expression[]{messageExpr};
		}

		recipients = (Expression<CommandSender>) exprs[1];
		if (isChatMessage)
			sender = (Expression<Player>) exprs[2];
		return LiteralUtils.canInitSafely(messageExpr);
	}

	@Override
	protected void execute(Event e) {
		Audience audience = Audience.audience(recipients.getArray(e));

		List<Component> components = ComponentHandler.parseFromExpressions(e, messages);

		if (isChatMessage) {
			Player from = sender != null ? sender.getSingle(e) : null;
			Identity identity = from != null ? Identity.identity(from.getUniqueId()) : Identity.nil();
			for (Component component : components)
				audience.sendMessage(identity, component);
		} else {
			for (Component component : components)
				audience.sendActionBar(component);
		}
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (isChatMessage)
			return "send " + messageExpr.toString(e, debug) + " to " + recipients.toString(e, debug) +
				(sender != null ? " from " + sender.toString(e, debug) : "");
		return "send action bar " + messageExpr.toString(e, debug) + " to " + recipients.toString(e, debug);
	}

}
