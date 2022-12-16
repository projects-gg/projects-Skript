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
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Map;
import java.util.WeakHashMap;


@Name("Boss Bar")
@Description("The boss bar of a player")
@Examples({"set bossbar of player to \"Hello!\""})
@Since("INSERT VERSION")
public class ExprBossBar extends SimplePropertyExpression<Player, BossBar> {

	static {
		register(ExprBossBar.class, BossBar.class, "boss[ ]bar", "players");
	}

	private final static Map<Player, BossBar> playerBossBarMap = new WeakHashMap<>();

	@Nullable
	public static BossBar getBossBarForPlayer(Player player) {
		return playerBossBarMap.get(player);
	}

	@Override
	@Nullable
	public BossBar convert(final Player player) {
		return getBossBarForPlayer(player);
	}

	@Override
	protected String getPropertyName() {
		return "bossbar";
	}

	@Override
	public Class<BossBar> getReturnType() {
		return BossBar.class;
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE)
			return new Class[] {String.class};
		return null;
	}

	@Override
	public void change(final Event event, final @Nullable Object[] delta, final ChangeMode mode) {
		for (final Player player : getExpr().getArray(event)) {
			switch (mode) {
				case DELETE:
					BossBar bossBar = getBossBarForPlayer(player);
					if (bossBar != null) {
						bossBar.removePlayer(player);
						playerBossBarMap.remove(player);
					}
					break;
				case SET:
					bossBar = getBossBarForPlayer(player);
					if (bossBar == null) {
						bossBar = Bukkit.createBossBar((String) delta[0], BarColor.WHITE, BarStyle.SOLID);
						playerBossBarMap.put(player, bossBar);
					} else {
						bossBar.setTitle((String) delta[0]);
					}
					bossBar.addPlayer(player);
			}
		}
	}
}
