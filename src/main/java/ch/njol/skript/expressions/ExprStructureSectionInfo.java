package ch.njol.skript.expressions;

import java.util.Locale;

import org.bukkit.Rotation;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.sections.EffSecStructurePlace;
import ch.njol.skript.sections.EffSecStructurePlace.StructurePlaceEvent;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Structure Place Settings")
@Description({
	"Returns or modifies the settings for placing of a structure.",
	"- includes entities will determine if the enitites saved should spawn when placing.",
	"- rotation will allow placement of the structure based on the rotation at the location point.",
	"- integrity determines how damaged the building should look by randomly skipping blocks to place. " +
			"This value can range from 0 to 1. With 0 removing all blocks and 1 spawning the structure in pristine condition.",
	"- pallet index is what iteration of the structure to use, starting at 0, or -1 to pick a random palette. Useful for Minecraft structures.",
	"- mirror the mirror setting for the structure on placement.",
	"Default settings for settings are rotation = none, pallet = 0, mirror = none, entities = true, integrity = 1"
})
@Examples({
	"place structure \"minecraft:end_city\" at player's location:",
		"\tset includes entities to false",
		"\tset integrity to 0.9",
		"\tset pallet to 2",
		"\tset rotation to clockwise 90",
		"\tset mirror to left to right"
})
@RequiredPlugins("Minecraft 1.17.1+")
@Since("INSERT VERSION")
public class ExprStructureSectionInfo extends SimpleExpression<Object> {

	static {
		if (Skript.classExists("org.bukkit.structure.Structure")) {
			Skript.registerExpression(ExprStructureSectionInfo.class, Object.class, ExpressionType.SIMPLE,
					"includes entities", "rotation", "integrity", "pallet [index]", "mirror");
		}
	}

	private enum Setting {
		INCLUDES_ENTITIES(Boolean.class),
		ROTATION(Rotation.class),
		INTEGRITY(Float.class),
		PALLET(Integer.class),
		MIRROR(Mirror.class);

		private final Class<? extends Object> returnType;

		Setting(Class<? extends Object> returnType) {
			this.returnType = returnType;
		}

		public Class<? extends Object> getReturnType() {
			return returnType;
		}
	}

	private Setting setting;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentSection(EffSecStructurePlace.class)) {
			Skript.error(parseResult.expr + " can only be used in a structure place section!");
			return false;
		}
		setting = Setting.values()[matchedPattern];
		return true;
	}

	@Override
	protected Object[] get(Event event) {
		if (!(event instanceof StructurePlaceEvent))
			return new Object[0];
		StructurePlaceEvent details = (StructurePlaceEvent) event;
		switch (setting) {
			case INCLUDES_ENTITIES:
				return CollectionUtils.array(details.includeEntities());
			case INTEGRITY:
				return CollectionUtils.array(details.includeEntities());
			case PALLET:
				return CollectionUtils.array(details.includeEntities());
			case MIRROR:
				return CollectionUtils.array(details.getMirror());
			case ROTATION:
				return CollectionUtils.array(details.getRotation());
		}
		return new Object[0];
	}

	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode != ChangeMode.SET)
			return null;
		if (setting.getReturnType().isAssignableFrom(Number.class))
			return CollectionUtils.array(Number.class);
		return CollectionUtils.array(setting.getReturnType());
	}

	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		if (delta == null)
			return;
		if (!(event instanceof StructurePlaceEvent))
			return;
		StructurePlaceEvent details = (StructurePlaceEvent) event;
		switch (setting) {
			case INCLUDES_ENTITIES:
				details.setIncludesEntities((boolean) delta[0]);
				break;
			case INTEGRITY:
				float integrity = ((Number) delta[0]).floatValue();
				details.setIntegrity(Math.min(1, Math.max(0, integrity)));
				break;
			case PALLET:
				int pallet = ((Number) delta[0]).intValue();
				int max = details.getStructure().getPaletteCount();
				details.setPallet(Math.min(max, Math.max(-1, pallet)));
				break;
			case MIRROR:
				details.setMirror((Mirror) delta[0]);
				break;
			case ROTATION:
				details.setRotation((StructureRotation) delta[0]);
				break;
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Object> getReturnType() {
		return setting.getReturnType();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "structure place setting " + setting.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
	}

}
