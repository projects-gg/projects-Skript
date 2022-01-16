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
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.minimessage.placeholder.Replacement;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ComponentHandler {

	private static final Map<String, Replacement<?>> PLACEHOLDERS = new HashMap<>();

	/**
	 * Registers a simple key-value placeholder with Skript's message parsers.
	 * See https://docs.adventure.kyori.net/minimessage.html#placeholder for details.
	 * @param tag The tag to be replaced.
	 * @param result The output to replace the input.
	 * @see #registerPlaceholder(Function)
	 */
	public static void registerPlaceholder(String tag, String result) {
		PLACEHOLDERS.put(tag, Replacement.raw(result));
	}

	/**
	 * Unregisters a tag from Skript's message parsers.
	 * @param tag The tag to unregister.
	 */
	public static void unregisterPlaceholder(String tag) {
		PLACEHOLDERS.remove(tag);
	}

	private static final List<Function<String, ComponentLike>> PLACEHOLDER_RESOLVERS = new ArrayList<>();

	/**
	 * Registers a resolver with Skript's message parsers.
	 * See https://docs.adventure.kyori.net/minimessage.html#placeholder-resolver for details.
	 * @param resolver The resolver to register.
	 */
	public static void registerPlaceholder(Function<String, ComponentLike> resolver) {
		PLACEHOLDER_RESOLVERS.add(resolver);
	}

	/**
	 * Unregisters a resolver from Skript's message parsers.
	 * @param resolver The resolver to unregister.
	 */
	public static void unregisterPlaceholder(Function<String, ComponentLike> resolver) {
		PLACEHOLDER_RESOLVERS.remove(resolver);
	}

	// The normal parser will process any proper tags
	private static final MiniMessage parser = MiniMessage.builder()
		.parsingErrorMessageConsumer(list -> {
			// Do nothing - this is to avoid errors being printed to console for malformed formatting
		})
		.placeholderResolver(PlaceholderResolver.builder()
			.dynamic(tag -> {
				Replacement<?> simpleReplacement = PLACEHOLDERS.get(tag);
				if (simpleReplacement != null)
					return simpleReplacement;

				for (Function<String, ComponentLike> resolver : PLACEHOLDER_RESOLVERS) {
					ComponentLike result = resolver.apply(tag);
					if (result != null)
						return Replacement.component(result);
				}
				return null;
			})
			.build()
		)
		.build();

	// The safe parser only parses color/decoration/formatting related tags
	@SuppressWarnings("unchecked")
	private static final MiniMessage safeParser = MiniMessage.builder()
		.parsingErrorMessageConsumer(list -> {
			// Do nothing - this is to avoid errors being printed to console for malformed formatting
		})
		.placeholderResolver(PlaceholderResolver.builder()
			.dynamic(tag -> {
				Replacement<?> simpleReplacement = PLACEHOLDERS.get(tag);
				if (simpleReplacement != null)
					return simpleReplacement;

				for (Function<String, ComponentLike> resolver : PLACEHOLDER_RESOLVERS) {
					ComponentLike result = resolver.apply(tag);
					if (result != null)
						return Replacement.component(result);
				}
				return null;
			})
			.build()
		)
		.transformations(
			TransformationRegistry.builder().clear().add(
				TransformationType.COLOR, TransformationType.DECORATION, TransformationType.RAINBOW,
				TransformationType.GRADIENT, TransformationType.FONT
			).build()
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
		System.out.println("CALLED: " + realMessage);

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
	 * Strips all formatting from a string.
	 * @param string The string to strip formatting from.
	 * @param all Whether ALL formatting should be stripped.
	 *            If true, tags like keybinds will also be converted into their plain text form.
	 *            If false, they will be left unparsed.
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
	 * @return The legacy string.
	 */
	public static String toLegacyString(String string) {
		return toLegacyString(string, false);
	}

	/**
	 * Converts a string into a legacy formatted string.
	 * @param string The string to convert.
	 * @param processFormatting Whether formatting should be processed before conversion.
	 * @return The legacy string.
	 */
	public static String toLegacyString(String string, boolean processFormatting) {
		return toLegacyString(parse(string, !processFormatting));
	}

	/**
	 * Converts a component into a legacy formatted string.
	 * @param component The component to convert.
	 * @return The legacy string.
	 */
	public static String toLegacyString(Component component) {
		return BukkitComponentSerializer.legacy().serialize(component);
	}

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
