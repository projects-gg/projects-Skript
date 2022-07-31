package org.skriptlang.skript.lang;

import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.lang.expression.Expression;

public interface SyntaxElement {

	boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult);

	/**
	 * @see ParserInstance#get()
	 */
	default ParserInstance getParser() {
		return ParserInstance.get();
	}

}
