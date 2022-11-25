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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.BlockStateBlock;
import ch.njol.util.Kleenean;

@Name("Structure Info")
@Description("Collect information about a structures' entities, size or blocks.")
@Examples({
	"loop all entities of structure {_structure}:",
		"\tif loop-entity is a diamond:",
			"\t\tmessage \"Race to the diamond at %loop-entity's location%\" to {_players::*}",
			"\t\tstop",
	"if the length of {_structure}'s vector is greater than 100:",
		"\tmessage \"&a+50 coins will be granted for winning on this larger map!\""
})
@RequiredPlugins("Minecraft 1.17.1+")
@Since("INSERT VERSION")
public class ExprStructureInfo extends SimplePropertyExpression<Structure, Object> {

	static {
		if (Skript.classExists("org.bukkit.structure.Structure"))
			register(ExprStructureInfo.class, Object.class, "(:blocks|:entities|size:(size|vector))", "structures");
	}

	private enum Property {
		BLOCKS(BlockStateBlock.class),
		SIZE(BlockVector.class),
		ENTITIES(Entity.class);

		private final Class<? extends Object> returnType;

		Property(Class<? extends Object> returnType) {
			this.returnType = returnType;
		}

		public Class<? extends Object> getReturnType() {
			return returnType;
		}
	}

	@Nullable
	private Expression<String> name;
	private Property property;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		property = Property.valueOf(parseResult.tags.get(0).toUpperCase(Locale.ENGLISH));
		setExpr((Expression<? extends Structure>) exprs[0]);
		return true;
	}

	@Override
	@Nullable
	public Object convert(Structure structure) {
		switch (property) {
			case BLOCKS:
				if (structure.getPaletteCount() > 0)
					return structure.getPalettes().get(0).getBlocks().stream()
							.map(state -> new BlockStateBlock(state, true))
							.toArray(BlockStateBlock[]::new);
			case ENTITIES:
				return structure.getEntities().toArray(Entity[]::new);
			case SIZE:
				return structure.getSize();
		}
		return null;
	}

	@Override
	@Nullable
	public Iterator<Object> iterator(Event event) {
		if (property != Property.BLOCKS)
			return getExpr().stream(event).map(this::convert).iterator();
		List<Object> blocks = new ArrayList<>();
		for (Structure structure : getExpr().getArray(event)) {
			if (structure.getPaletteCount() > 0)
				blocks.addAll(structure.getPalettes().get(0).getBlocks());
		}
		return blocks.iterator();
	}

	@Override
	public Class<? extends Object> getReturnType() {
		return property.getReturnType();
	}

	@Override
	protected String getPropertyName() {
		return property.name().toLowerCase(Locale.ENGLISH);
	}

}
