package ch.njol.skript.expressions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Structures")
@Description({
	"A structure is a utility that allows you to save a cuboid of blocks and entities.",
	"This syntax will returns an existing structure from memory or you can also create a structure between two locations.",
	"If the name contains a collon, it'll grab from the Minecraft structure space.",
})
@Examples({
	"set {_structure} to a new structure between {location1} and {location2} named \"Example\"",
	"set {_structure} to structure \"Example\""
})
@RequiredPlugins("Minecraft 1.17.1+")
@Since("INSERT VERSION")
public class ExprStructure extends SimpleExpression<Structure> {

	static {
		if (Skript.classExists("org.bukkit.structure.Structure"))
			Skript.registerExpression(ExprStructure.class, Structure.class, ExpressionType.COMBINED,
					"structure[s] [named] %strings%",
					"[a] [new] structure between %location% (and|to) %location% [(including|with) :entities] named %string%"
			);
	}

	@Nullable
	private Expression<Location> location1, location2;
	private Expression<String> names;
	private boolean entities;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = parseResult.hasTag("entities");
		if (matchedPattern == 0) {
			names = (Expression<String>) exprs[0];
			return true;
		}
		location1 = (Expression<Location>) exprs[0];
		location2 = (Expression<Location>) exprs[1];
		names = (Expression<String>) exprs[2];
		return true;
	}

	@Override
	protected Structure[] get(Event event) {
		StructureManager manager = Bukkit.getStructureManager();

		// Returning existing structure.
		if (location1 == null || location2 == null) {
			return names.stream(event)
					.map(name -> Utils.getNamespacedKey(name))
					.map(name -> name != null ? manager.loadStructure(name, false) : null)
					.toArray(Structure[]::new);
		}
		Location location1 = this.location1.getSingle(event);
		Location location2 = this.location2.getSingle(event);
		String name = this.names.getSingle(event);
		if (location1 == null || location2 == null || name == null)
			return new Structure[0];

		Structure structure = manager.loadStructure(Utils.getNamespacedKey(name), true);
		structure.fill(location1, location2, entities);
		return CollectionUtils.array(structure);
	}

	@Override
	public boolean isSingle() {
		return names.isSingle();
	}

	@Override
	public Class<? extends Structure> getReturnType() {
		return Structure.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (location1 == null || location2 == null)
			return "structures " + names.toString(event, debug);
		return "structure " + names.toString(event, debug)
			+ " from " + location1.toString(event, debug) + " to " + location2.toString(event, debug);
	}

}
