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
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Duration;

@Name("Send Title")
@Description({
	"Sends a title/subtitle to the given player(s) with optional fadein/stay/fadeout times for Minecraft versions 1.11 and above. ",
	"",
	"If you're sending only the subtitle, it will be shown only if there's a title displayed at the moment, otherwise it will "
	+ "be sent with the next title. To show only the subtitle, use: <code>send title \" \" with subtitle \"yourtexthere\" to player</code>.",
	"",
	"Note: if no input is given for the times, it will keep the ones from the last title sent, "
	+ "use the <a href='effects.html#EffResetTitle'>reset title</a> effect to restore the default values."
})
@Examples({
	"send title \"Competition Started\" with subtitle \"Have fun, Stay safe!\" to player for 5 seconds",
	"send title \"Hi %player%\" to player",
	"send title \"Loot Drop\" with subtitle \"starts in 3 minutes\" to all players",
	"send title \"Hello %player%!\" with subtitle \"Welcome to our server\" to player for 5 seconds with fadein 1 second and fade out 1 second",
	"send subtitle \"Party!\" to all players"
})
@Since("2.3, INSERT VERSION (sending objects)")
public class EffSendTitle extends Effect {

	static {
		Skript.registerEffect(EffSendTitle.class,
			"send title %object% [with subtitle %-object%] [to %commandsenders%] [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]",
			"send subtitle %object% [to %commandsenders%] [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]"
		);
	}

	@Nullable
	private Expression<?> title;
	@Nullable
	private Expression<?> subtitle;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<CommandSender> recipients;
	@Nullable
	private Expression<Timespan> fadeIn, stay, fadeOut;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		title = matchedPattern == 0 ? exprs[0] : null;
		subtitle = exprs[1 - matchedPattern];
		recipients = (Expression<CommandSender>) exprs[2 - matchedPattern];
		stay = (Expression<Timespan>) exprs[3 - matchedPattern];
		fadeIn = (Expression<Timespan>) exprs[4 - matchedPattern];
		fadeOut = (Expression<Timespan>) exprs[5 - matchedPattern];
		return true;
	}

	@Override
	protected void execute(Event e) {
		Audience audience = Audience.audience(recipients.getArray(e));

		Timespan fadeIn = this.fadeIn != null ? this.fadeIn.getSingle(e) : null;
		Timespan stay = this.stay != null ? this.stay.getSingle(e) : null;
		Timespan fadeOut = this.fadeOut != null ? this.fadeOut.getSingle(e) : null;

		Duration fadeInDuration = fadeIn != null ? Ticks.duration(fadeIn.getTicks_i()) : Title.DEFAULT_TIMES.fadeIn();
		Duration stayDuration = stay != null ? Ticks.duration(stay.getTicks_i()) : Title.DEFAULT_TIMES.stay();
		Duration fadeOutDuration = fadeOut != null ? Ticks.duration(fadeOut.getTicks_i()) : Title.DEFAULT_TIMES.fadeOut();

		Times times = Times.times(fadeInDuration, stayDuration, fadeOutDuration);
		Title title = Title.title(ComponentHandler.parseFromSingleExpression(e, this.title), ComponentHandler.parseFromSingleExpression(e, this.subtitle), times);

		audience.showTitle(title);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		StringBuilder builder = new StringBuilder();
		if (title != null) {
			builder.append("send title ").append(title.toString(e, debug));
			if (subtitle != null)
				builder.append(" with subtitle ").append(subtitle.toString(e, debug));
		} else {
			assert subtitle != null;
			builder.append("send subtitle ").append(subtitle.toString(e, debug));
		}

		builder.append(" to ").append(recipients.toString(e, debug));

		if (stay != null) {
			builder.append(" for ").append(stay.toString(e, debug));
		} else {
			long ticks = Title.DEFAULT_TIMES.stay().toMillis() / Ticks.SINGLE_TICK_DURATION_MS;
			builder.append(" for ").append(ticks).append(" ticks");
		}
		if (fadeIn != null) {
			builder.append(" with fade in ").append(fadeIn.toString(e, debug));
		} else {
			long ticks = Title.DEFAULT_TIMES.fadeIn().toMillis() / Ticks.SINGLE_TICK_DURATION_MS;
			builder.append(" with fade in ").append(ticks).append(" ticks");
		}
		if (fadeOut != null) {
			builder.append(" with fade out ").append(fadeOut.toString(e, debug));
		} else {
			if (fadeIn != null) {
				builder.append(" and");
			} else {
				builder.append(" with");
			}
			long ticks = Title.DEFAULT_TIMES.fadeOut().toMillis() / Ticks.SINGLE_TICK_DURATION_MS;
			builder.append(" fade out ").append(ticks).append(" ticks");
		}

		return builder.toString();
	}

}
