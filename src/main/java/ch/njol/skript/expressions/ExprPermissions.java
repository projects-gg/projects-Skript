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

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.PermissionAttachment;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("All Permissions")
@Description("Returns all permissions of the defined permissible(s). A permissible is an object like an entity that can have permissions.")
@Examples("set {_permissions::*} to all permissions of the player")
@Since("2.2-dev33, INSERT VERSION (Changers)")
public class ExprPermissions extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprPermissions.class, String.class, ExpressionType.PROPERTY, "[(all [[of] the]|the)] permissions (from|of) %entities%", "[(all [[of] the]|the)] %entities%'[s] permissions");
	}

	// Metadata tag
	private static final String PERMISSION_TAG = "skript-permissions";

	private Expression<Entity> entities;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = (Expression<Entity>) exprs[0];
		return true;
	}

	@Override
	@Nullable
	protected String[] get(Event event) {
		return entities.stream(event)
				.flatMap(permissible -> permissible.getEffectivePermissions().stream())
				.map(permission -> permission.getPermission())
				.toArray(String[]::new);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "permissions " + " of " + entities.toString(event, debug);
	}

	@Override
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE)
			return CollectionUtils.array(String.class, String[].class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		for (Entity entity : entities.getAll(event)) {
			PermissionAttachment perm = getPermission(entity);
			for (String string : (String[]) delta)
				perm.setPermission(string, mode == ChangeMode.ADD);
		}
	}

	private PermissionAttachment getPermission(Entity entity) {
		Skript instance = Skript.getInstance();
		if (!entity.hasMetadata(PERMISSION_TAG))
			entity.setMetadata(PERMISSION_TAG, new FixedMetadataValue(instance, entity.addAttachment(instance)));
		return (PermissionAttachment) entity.getMetadata(PERMISSION_TAG).get(0).value();
	}

}
