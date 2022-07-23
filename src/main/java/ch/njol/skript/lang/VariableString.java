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
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import org.skriptlang.skript.bukkit.chat.elements.ExprColoured;
import org.skriptlang.skript.bukkit.chat.util.ComponentHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a string that may contain expressions, and is thus "variable".
 * 
 * @author Peter Güttinger
 */
public class VariableString implements Expression<String> {
	
	private final String orig;
	
	@Nullable
	private final Object[] string;

	@Nullable
	private final String simple;
	@Nullable
	private final Component simpleComponent;
	@Nullable
	private final String simpleResolved;

	private final boolean isSimple;
	private final StringMode mode;

	/**
	 * Creates a new VariableString which does not contain variables.
	 * @param s Content for string.
	 */
	private VariableString(String s) {
		isSimple = true;
		simple = s.replace("%%", "%"); // This doesn't contain variables, so this wasn't done in newInstance!
		simpleComponent = ComponentHandler.parse(s, false);
		simpleResolved = ComponentHandler.toLegacyString(simpleComponent);
				
		orig = simple;
		string = null;
		mode = StringMode.MESSAGE;
	}
	
	/**
	 * Creates a new VariableString which contains variables.
	 * @param orig Original string (unparsed).
	 * @param string Objects, some of them are variables.
	 * @param mode String mode.
	 */
	private VariableString(String orig, Object[] string, StringMode mode) {
		this.orig = orig;

		this.string = string;
		
		this.mode = mode;
		
		isSimple = false;
		simple = null;
		simpleComponent = null;
		simpleResolved = null;
	}
	
	/**
	 * Prints errors
	 */
	@Nullable
	public static VariableString newInstance(String s) {
		return newInstance(s, StringMode.MESSAGE);
	}

	/**
	 * Tests whether a string is correctly quoted, i.e. only has doubled double quotes in it.
	 * Singular double quotes are only allowed between percentage signs.
	 * 
	 * @param s The string
	 * @param withQuotes Whether s must be surrounded by double quotes or not
	 * @return Whether the string is quoted correctly
	 */
	public static boolean isQuotedCorrectly(String s, boolean withQuotes) {
		if (withQuotes && (!s.startsWith("\"") || !s.endsWith("\"")))
			return false;
		boolean quote = false;
		boolean percentage = false;
		for (int i = withQuotes ? 1 : 0; i < (withQuotes ? s.length() - 1 : s.length()); i++) {
			if (percentage) {
				if (s.charAt(i) == '%')
					percentage = false;
				
				continue;
			}
			
			if (quote && s.charAt(i) != '"')
				return false;
			
			if (s.charAt(i) == '"') {
				quote = !quote;
			} else if (s.charAt(i) == '%') {
				percentage = true;
			}
		}
		return !quote;
	}
	
	/**
	 * Removes quoted quotes from a string.
	 * 
	 * @param s The string
	 * @param surroundingQuotes Whether the string has quotes at the start & end that should be removed
	 * @return The string with double quotes replaced with signle ones and optionally with removed surrounding quotes.
	 */
	public static String unquote(String s, boolean surroundingQuotes) {
		assert isQuotedCorrectly(s, surroundingQuotes);
		if (surroundingQuotes)
			return s.substring(1, s.length() - 1).replace("\"\"", "\"");
		return s.replace("\"\"", "\"");
	}
	
	/**
	 * Creates an instance of VariableString by parsing given string.
	 * Prints errors and returns null if it is somehow invalid.
	 * 
	 * @param orig Unquoted string to parse.
	 * @return A new VariableString instance.
	 */
	@Nullable
	public static VariableString newInstance(String orig, StringMode mode) {
		if (mode != StringMode.VARIABLE_NAME && !isQuotedCorrectly(orig, false))
			return null;
		int n = StringUtils.count(orig, '%');
		if (n % 2 != 0) {
			Skript.error("The percent sign is used for expressions (e.g. %player%). To insert a '%' type it twice: %%.");
			return null;
		}
		
		// We must not parse color codes yet, as JSON support would be broken :(
		String s;
		if (mode != StringMode.VARIABLE_NAME) {
			// Replace every double " character with a single ", except for those in expressions (between %)
			StringBuilder stringBuilder = new StringBuilder();
			
			boolean expression = false;
			for (int i = 0; i < orig.length(); i++) {
				char c = orig.charAt(i);
				stringBuilder.append(c);
				
				if (c == '%')
					expression = !expression;
				
				if (!expression && c == '"')
					i++;
			}
			s = stringBuilder.toString();
		} else {
			s = orig;
		}
		
		List<Object> string = new ArrayList<>(n / 2 + 2); // List of strings and expressions
		
		int c = s.indexOf('%');
		if (c != -1) {
			if (c != 0)
				string.add(s.substring(0, c));
			while (c != s.length()) {
				int c2 = s.indexOf('%', c + 1);
				
				int a = c;
				int b;
				while (c2 != -1 && (b = s.indexOf('{', a + 1)) != -1 && b < c2) {
					a = nextVariableBracket(s, b + 1);
					if (a == -1) {
						Skript.error("Missing closing bracket '}' to end variable");
						return null;
					}
					c2 = s.indexOf('%', a + 1);
				}
				if (c2 == -1) {
					assert false;
					return null;
				}
				if (c + 1 == c2) {
					// %% escaped -> one % in result string
					if (string.size() > 0 && string.get(string.size() - 1) instanceof String) {
						string.set(string.size() - 1, (String) string.get(string.size() - 1) + "%");
					} else {
						string.add("%");
					}
				} else {
					RetainingLogHandler log = SkriptLogger.startRetainingLog();
					try {
						Expression<?> expr =
							new SkriptParser(s.substring(c + 1, c2), SkriptParser.PARSE_EXPRESSIONS, ParseContext.DEFAULT)
								.parseExpression(Object.class);
						if (expr == null) {
							log.printErrors("Can't understand this expression: " + s.substring(c + 1, c2));
							return null;
						} else {
							string.add(expr);
						}
						log.printLog();
					} finally {
						log.stop();
					}
				}
				c = s.indexOf('%', c2 + 1);
				if (c == -1)
					c = s.length();
				String l = s.substring(c2 + 1, c); // Try to get string (non-variable) part
				if (!l.isEmpty()) { // This is string part (no variables)
					if (string.size() > 0 && string.get(string.size() - 1) instanceof String) {
						// We can append last string part in the list, so let's do so
						string.set(string.size() - 1, (String) string.get(string.size() - 1) + l);
					} else { // Can't append, just add new part
						string.add(l);
					}
				}
			}
		} else {
			// Only one string, no variable parts
			string.add(s);
		}
		
		// Check if this isn't actually variable string, and return
		if (string.size() == 1 && string.get(0) instanceof String)
			return new VariableString(s);
		
		Object[] sa = string.toArray();
		if (string.size() == 1 && string.get(0) instanceof Expression &&
				((Expression<?>) string.get(0)).getReturnType() == String.class &&
				((Expression<?>) string.get(0)).isSingle() &&
				mode == StringMode.MESSAGE) {
			String expr = ((Expression<?>) string.get(0)).toString(null, false);
			Skript.warning(expr + " is already a text, so you should not put it in one (e.g. " + expr + " instead of " + "\"%" + expr.replace("\"", "\"\"") + "%\")");
		}
		return new VariableString(orig, sa, mode);
	}

	/**
	 * Copied from {@code SkriptParser#nextBracket(String, char, char, int, boolean)}, but removed escaping & returns -1 on error.
	 * 
	 * @param s
	 * @param start Index after the opening bracket
	 * @return The next closing curly bracket
	 */
	public static int nextVariableBracket(String s, int start) {
		int n = 0;
		for (int i = start; i < s.length(); i++) {
			if (s.charAt(i) == '}') {
				if (n == 0)
					return i;
				n--;
			} else if (s.charAt(i) == '{') {
				n++;
			}
		}
		return -1;
	}
	
	public static VariableString[] makeStrings(String[] args) {
		VariableString[] strings = new VariableString[args.length];
		int j = 0;
		for (String arg : args) {
			VariableString vs = newInstance(arg);
			if (vs != null)
				strings[j++] = vs;
		}
		if (j != args.length)
			strings = Arrays.copyOf(strings, j);
		return strings;
	}
	
	/**
	 * @param args Quoted strings - This is not checked!
	 * @return a new array containing all newly created VariableStrings, or null if one is invalid
	 */
	@Nullable
	public static VariableString[] makeStringsFromQuoted(List<String> args) {
		VariableString[] strings = new VariableString[args.size()];
		for (int i = 0; i < args.size(); i++) {
			assert args.get(i).startsWith("\"") && args.get(i).endsWith("\"");
			VariableString vs = newInstance(args.get(i).substring(1, args.get(i).length() - 1));
			if (vs == null)
				return null;
			strings[i] = vs;
		}
		return strings;
	}
	
	/**
	 * Parses all expressions in the string and returns it.
	 * If this is a simple string, the event may be null.
	 *
	 * Simple formatting (colors) will be processed.
	 * 
	 * @param e Event to pass to the expressions.
	 * @return The input string with all expressions replaced.
	 * @see #toString(boolean, Event)
	 */
	public String toString(@Nullable Event e) {
		return toString(true, e);
	}

	/**
	 * Parses all expressions in the string and returns it.
	 * If this is a simple string, the event may be null.
	 *
	 * @param format Whether simple formatting (colors) should be processed.
	 * @param e Event to pass to the expressions.
	 * @return The input string with all expressions replaced.
	 */
	public String toString(boolean format, @Nullable Event e) {
		if (isSimple) {
			if (format) {
				assert simpleResolved != null;
				return simpleResolved;
			} else {
				assert simple != null;
				return simple;
			}
		}

		if (e == null) {
			throw new IllegalArgumentException("Event may not be null in non-simple VariableStrings!");
		}

		Object[] string = this.string;
		assert string != null;
		StringBuilder b = new StringBuilder();
		for (Object o : string) {
			if (o instanceof Expression<?>) {
				boolean escape = !(o instanceof ExprColoured) && format;
				String expression = Classes.toString(((Expression<?>) o).getArray(e), true, mode);
				if (escape)
					ComponentHandler.escape(expression);
				b.append(expression);
			} else {
				b.append(o);
			}
		}
		return format ? ComponentHandler.toLegacyString(b.toString(), false) : b.toString();
	}
	
	@Override
	public String toString() {
		return toString(null, false);
	}
	
	/**
	 * Use {@link #toString(Event)} to get the actual string
	 */
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (isSimple) {
			assert simple != null;
			return '"' + simple + '"';
		}
		Object[] string = this.string;
		assert string != null;
		StringBuilder b = new StringBuilder("\"");
		for (Object o : string) {
			if (o instanceof Expression) {
				b.append("%").append(((Expression<?>) o).toString(e, debug)).append("%");
			} else {
				b.append(o);
			}
		}
		b.append('"');
		return b.toString();
	}

	public Component getAsComponent(@Nullable Event e) {
		if (isSimple) {
			assert simpleComponent != null;
			return simpleComponent;
		}

		if (e == null)
			throw new IllegalArgumentException("Event may not be null in non-simple VariableStrings!");

		return ComponentHandler.parse(toString(false, e));
	}
	
	public String getDefaultVariableName() {
		if (isSimple) {
			assert simple != null;
			return simple;
		}
		Object[] string = this.string;
		assert string != null;
		StringBuilder b = new StringBuilder();
		for (Object o : string) {
			if (o instanceof Expression) {
				b.append("<")
					.append(Classes.getSuperClassInfo(((Expression<?>) o).getReturnType()).getCodeName())
					.append(">");
			} else {
				b.append(o);
			}
		}
		return b.toString();
	}
	
	public boolean isSimple() {
		return isSimple;
	}
	
	public StringMode getMode() {
		return mode;
	}
	
	public VariableString setMode(StringMode mode) {
		if (this.mode == mode || isSimple)
			return this;
		BlockingLogHandler h = new BlockingLogHandler().start();
		try {
			VariableString vs = newInstance(orig, mode);
			if (vs == null) {
				assert false : this + "; " + mode;
				return this;
			}
			return vs;
		} finally {
			h.stop();
		}
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getSingle(Event e) {
		return toString(e);
	}
	
	@Override
	public String[] getArray(Event e) {
		return new String[] {toString(e)};
	}
	
	@Override
	public String[] getAll(Event e) {
		return new String[] {toString(e)};
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public boolean check(Event e, Checker<? super String> c, boolean negated) {
		return SimpleExpression.check(getAll(e), c, negated, false);
	}
	
	@Override
	public boolean check(Event e, Checker<? super String> c) {
		return SimpleExpression.check(getAll(e), c, false, false);
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, String.class))
			return (Expression<? extends R>) this;
		if (CollectionUtils.containsSuperclass(to, Component.class))
			return new SimpleExpression<R>() {
				@Override
				public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
					throw new UnsupportedOperationException();
				}

				@Override
				protected @Nullable R[] get(Event e) {
					return (R[]) new Component[]{getAsComponent(e)};
				}

				@Override
				public boolean isSingle() {
					return true;
				}

				@Override
				public Class<? extends R> getReturnType() {
					return (Class<? extends R>) Component.class;
				}

				@Override
				public String toString(@Nullable Event e, boolean debug) {
					return VariableString.this.toString(e, debug);
				}
			};
		return ConvertedExpression.newInstance(this, to);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean getAnd() {
		return true;
	}
	
	@Override
	public boolean setTime(int time) {
		return false;
	}
	
	@Override
	public int getTime() {
		return 0;
	}
	
	@Override
	public boolean isDefault() {
		return false;
	}
	
	@Override
	public Iterator<? extends String> iterator(Event e) {
		return new SingleItemIterator<>(toString(e));
	}
	
	@Override
	public boolean isLoopOf(String s) {
		return false;
	}
	
	@Override
	public Expression<?> getSource() {
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Expression<T> setStringMode(Expression<T> e, StringMode mode) {
		if (e instanceof ExpressionList) {
			Expression<?>[] ls = ((ExpressionList<?>) e).getExpressions();
			for (int i = 0; i < ls.length; i++) {
				Expression<?> l = ls[i];
				assert l != null;
				ls[i] = setStringMode(l, mode);
			}
		} else if (e instanceof VariableString) {
			return (Expression<T>) ((VariableString) e).setMode(mode);
		}
		return e;
	}
	
	@Override
	public Expression<String> simplify() {
		return this;
	}

}
