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
package ch.njol.skript.bukkitutil;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import ch.njol.skript.Skript;

public class LookGoal implements Goal<Mob> {

	private final float speed, maxPitch;
	private final Object target;
	private final Mob mob;
	private int ticks = 0;

	LookGoal(Object target, Mob mob, float speed, float maxPitch) {
		this.maxPitch = maxPitch;
		this.target = target;
		this.speed = speed;
		this.mob = mob;
	}

	@Override
	public boolean shouldActivate() {
		return ticks < 50;
	}

	@Override
	public void tick() {
		if (target instanceof Vector) {
			Vector vector = ((Vector)target);
			mob.lookAt(vector.getX(), vector.getY(), vector.getZ(), speed, maxPitch);
		} else if (target instanceof Location) {
			mob.lookAt((Location) target, speed, maxPitch);
		} else if (target instanceof Entity) {
			mob.lookAt((Entity) target, speed, maxPitch);
		}
		ticks++;
	}

	@Override
	public GoalKey<Mob> getKey() {
		return GoalKey.of(Mob.class, new NamespacedKey(Skript.getInstance(), "skript_entity_look"));
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.LOOK);
	}

}
