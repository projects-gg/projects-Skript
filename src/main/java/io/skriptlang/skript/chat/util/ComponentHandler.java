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
package io.skriptlang.skript.chat.util;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.registrations.Classes;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentHandler {

	private static final Map<String, Tag> SIMPLE_PLACEHOLDERS = new HashMap<>();
	private static final List<TagResolver> RESOLVERS = new ArrayList<>();

	/**
	 * Registers a simple key-value placeholder with Skript's message parsers.
	 * @param tag The name/key of the placeholder.
	 * @param result The result/value of the placeholder.
	 */
	public static void registerPlaceholder(String tag, String result) {
		SIMPLE_PLACEHOLDERS.put(tag, Tag.preProcessParsed(result));
	}

	/**
	 * Unregisters a simple key-value placeholder from Skript's message parsers.
	 * @param tag The name of the placeholder to unregister.
	 */
	public static void unregisterPlaceholder(String tag) {
		SIMPLE_PLACEHOLDERS.remove(tag);
	}

	/**
	 * Registers a TagResolver with Skript's message parsers.
	 * @param resolver The TagResolver to register.
	 */
	public static void registerResolver(TagResolver resolver) {
		RESOLVERS.add(resolver);
	}

	/**
	 * Unregisters a TagResolver from Skript's message parsers.
	 * @param resolver The TagResolver to unregister.
	 */
	public static void unregisterResolver(TagResolver resolver) {
		RESOLVERS.remove(resolver);
	}

	private static final TagResolver SKRIPT_TAG_RESOLVER = new TagResolver() {
		@Override
		@Nullable
		public Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
			Tag simple = SIMPLE_PLACEHOLDERS.get(name);
			if (simple != null)
				return simple;
			for (TagResolver resolver : RESOLVERS) {
				Tag resolved = resolver.resolve(name, arguments, ctx);
				if (resolved != null)
					return resolved;
			}
			return null;
		}

		@Override
		public boolean has(@NotNull String name) {
			if (SIMPLE_PLACEHOLDERS.containsKey(name))
				return true;
			for (TagResolver resolver : RESOLVERS) {
				if (resolver.has(name))
					return true;
			}
			return false;
		}
	};

	// The normal parser will process any proper tags
	private static final MiniMessage parser = MiniMessage.builder()
		.strict(false)
		.tags(TagResolver.builder()
			.resolver(StandardTags.defaults())
			.resolver(SKRIPT_TAG_RESOLVER)
			.build()
		)
		.build();

	// The safe parser only parses color/decoration/formatting related tags
	private static final MiniMessage safeParser = MiniMessage.builder()
		.strict(false)
		.tags(TagResolver.builder()
			.resolvers(
				StandardTags.color(), StandardTags.decorations(), StandardTags.font(),
				StandardTags.gradient(), StandardTags.rainbow(), StandardTags.newline(),
				StandardTags.reset(), StandardTags.transition()
			)
			.resolver(SKRIPT_TAG_RESOLVER)
			.build()
		)
		.build();

	/**
	 * Parses a string using one of the MiniMessage parsers.
	 * @param message The message to parse. Will be parsed with the safe parser by default.
	 * @return An adventure component from the parsed message.
	 * @see #parse(Object, boolean)
	 */
	public static Component parse(Object message) {
		return parse(message, true);
	}

	/**
	 * Parses a string using one of the MiniMessage parsers.
	 * @param message The message to parse.
	 * @param safe Whether only color/decoration/formatting related tags should be parsed.
	 * @return An adventure component from the parsed message.
	 */
	public static Component parse(Object message, boolean safe) {
		String realMessage = message instanceof String ? (String) message : Classes.toString(message);

		if (realMessage.isEmpty()) {
			return Component.empty();
		}

		if (realMessage.contains("&") || realMessage.contains("§")) {
			System.out.println("CALLED LEGACY PARSING");
			long start = System.nanoTime();
			StringBuilder reconstructedMessage = new StringBuilder();
			char[] messageChars = realMessage.toCharArray();
			int length = messageChars.length;
			for (int i = 0; i < length; i++) {
				char current = messageChars[i];
				char next = (i + 1 != length) ? messageChars[i + 1] : ' ';
				boolean isCode = current == '&' || current == '§';
				if (isCode && next == 'x') { // Try to parse as hex -> &x&1&2&3&4&5&6
					reconstructedMessage.append("<#");
					for (int i2 = i + 3; i2 < i + 14; i2 += 2) // Isolate the specific numbers
						reconstructedMessage.append(messageChars[i2]);
					reconstructedMessage.append('>');
					i += 13; // Skip to the end
				} else if (isCode) {
					String color = CodeConverter.getColor(next);
					if (color != null) { // This is a valid code
						reconstructedMessage.append('<').append(color).append('>');
						i++; // Skip to the end
					} else { // Not a valid color :(
						reconstructedMessage.append(current);
					}
				} else {
					reconstructedMessage.append(current);
				}
			}
			realMessage = reconstructedMessage.toString();
			System.out.println("FINISHED LEGACY PARSING: " + (1. * (System.nanoTime() - start) / 1000000.));
		}

		// Really annoying backwards compatibility check
		realMessage = realMessage.replace("<dark cyan>", "<dark_aqua>")
			.replace("<dark turquoise>", "<dark_aqua>")
			.replace("<dark yellow>", "<gold>")
			.replace("<light grey>", "<grey>")
			.replace("<light gray>", "<grey>")
			.replace("<dark silver>", "<dark_grey>")
			.replace("<light blue>", "<blue>")
			.replace("<light green>", "<green>")
			.replace("<lime green>", "<green>")
			.replace("<light cyan>", "<aqua>")
			.replace("<light aqua>", "<aqua>")
			.replace("<light red>", "<red>")
			.replace("<light yellow>", "<yellow>");

		return safe ? safeParser.deserialize(realMessage) : parser.deserialize(realMessage);
	}

	/**
	 * Constructs a list of components from the given expressions.
	 * @param e The event to get expression values with.
	 * @param expressions The expressions to parse from.
	 * @return A list of components parsed from the stringified expressions.
	 * @see #parseFromSingleExpression(Event, Expression)
	 */
	public static List<Component> parseFromExpressions(Event e, Expression<?>... expressions) {
		List<Component> components = new ArrayList<>();
		for (Expression<?> expression : expressions) {
			if (expression instanceof VariableString) { // Get the unformatted string since we'll be formatting it here
				components.add(((VariableString) expression).getAsComponent(e));
			} else { // Might not be safe, only parse formatting
				for (Object messageObject : expression.getArray(e)) {
					if (messageObject instanceof Component) { // No point in doing anything
						components.add((Component) messageObject);
					} else {
						components.add(parse(messageObject, true));
					}
				}
			}
		}
		return components;
	}

	/**
	 * Constructs a component from the given expression.
	 * @param e The event to get expression values with.
	 * @param expression The expression to parse from.
	 * @return A component parsed from the stringified expression.
	 * Will return an empty component if the provided expression is null.
	 * @see #parseFromExpressions(Event, Expression[])
	 */
	public static Component parseFromSingleExpression(Event e, @Nullable Expression<?> expression) {
		if (expression != null && expression.isSingle()) {
			if (expression instanceof VariableString)
				return ((VariableString) expression).getAsComponent(e);
			Object object = expression.getSingle(e);
			if (object != null) {
				if (object instanceof Component)
					return (Component) object;
				return ComponentHandler.parse(object, true);
			}
		}
		return Component.empty();
	}

	/**
	 * Creates a plain text component from an object.
	 * @param message The message to create a component from.
	 * @return An unprocessed component from the given message.
	 */
	public static Component plain(Object message) {
		return Component.text(message instanceof String ? (String) message : Classes.toString(message));
	}

	/**
	 * Escapes all tags known to Skript in the given string.
	 * @param string The string to escape tags in.
	 * @return The string with tags escaped.
	 */
	public static String escape(String string) {
		return parser.escapeTags(string);
	}

	/**
	 * Strips all formatting from a string.
	 * @param string The string to strip formatting from.
	 * @param all Whether ALL formatting/tags should be stripped.
	 *            If false, only safe tags like colors and decorations will be stripped.
	 * @return The stripped string.
	 */
	public static String stripFormatting(String string, boolean all) {
		return stripFormatting(parse(string, !all));
	}

	/**
	 * Strips all formatting from a component.
	 * @param component The component to strip formatting from.
	 * @return A stripped string from a component.
	 */
	public static String stripFormatting(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	/**
	 * Converts a string into a legacy formatted string.
	 * @param string The string to convert.
	 * @param all Whether ALL formatting/tags should be converted to a legacy format.
	 *            If false, only safe tags like colors and decorations will be converted.
	 * @return The legacy string.
	 */
	public static String toLegacyString(String string, boolean all) {
		return toLegacyString(parse(string, !all));
	}

	/**
	 * Converts a component into a legacy formatted string.
	 * @param component The component to convert.
	 * @return The legacy string.
	 */
	public static String toLegacyString(Component component) {
		return BukkitComponentSerializer.legacy().serialize(component);
	}

	@Nullable
	private static BukkitAudiences adventure = null; // Can't set here as we need an instance of Skript

	public static BukkitAudiences getAdventure() {
		if (adventure == null)
			adventure = BukkitAudiences.create(Skript.getInstance());
		return adventure;
	}

	/**
	 * Constructs an audience from command senders.
	 * @param senders The members of this audience.
	 * @return An audience consisting of the provided command senders.
	 */
	@SuppressWarnings("ConstantConditions")
	public static Audience audienceFrom(Collection<CommandSender> senders) {
		List<Audience> bukkitAudiences = new ArrayList<>();
		for (CommandSender sender : senders) {
			if (sender instanceof Audience) { // On paper, a CommandSender is an Audience
				bukkitAudiences.add(sender);
			} else {
				bukkitAudiences.add(getAdventure().sender(sender));
			}
		}
		return Audience.audience(bukkitAudiences);
	}

	/**
	 * Constructs an audience from command senders.
	 * @param senders The members of this audience.
	 * @return An audience consisting of the provided command senders.
	 */
	public static Audience audienceFrom(CommandSender... senders) {
		List<Audience> bukkitAudiences = new ArrayList<>();
		for (CommandSender sender : senders)
			bukkitAudiences.add(getAdventure().sender(sender));
		return Audience.audience(bukkitAudiences);
	}

}
