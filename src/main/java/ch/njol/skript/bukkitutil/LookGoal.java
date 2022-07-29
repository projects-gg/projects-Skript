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
