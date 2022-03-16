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
package ch.njol.skript.tests.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EffJavaTest extends Effect {

	static {
		Skript.registerEffect(EffJavaTest.class, "run java test %string% (from|located in) [class] %string%");
	}

	@Nullable
	private Expression<String> javaTestName;

	@Nullable
	private Expression<String> javaTestClass;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		javaTestName = (Expression<String>) exprs[0];
		javaTestClass = (Expression<String>) exprs[1];
		return true;
	}

	@Override
	protected void execute(Event e) {
		assert javaTestName != null && javaTestClass != null;
		String javaTestName = this.javaTestName.getSingle(e);
		String javaTestClass = this.javaTestClass.getSingle(e);

		if (javaTestName == null) {
			throw new SkriptAPIException("Test name not provided");
		} else if (javaTestClass == null) {
			TestTracker.testStarted(javaTestName);
			TestTracker.testFailed("Test failed because test class was null");
		} else {
			JavaTest test = null;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends JavaTest> testClass = (Class<? extends JavaTest>) Class.forName(javaTestClass);
				test = testClass.getDeclaredConstructor().newInstance();
			} catch (Exception ex) {
				TestTracker.testStarted(javaTestName);
				TestTracker.testFailed("Test failed because a " + ex.getClass().getSimpleName() + " occurred: " + ex.getMessage());
			}

			if (test != null) {
				test.run(javaTestName);
			}
		}

	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		assert javaTestClass != null && javaTestName != null;
		return "run java test " + javaTestName.toString(e, debug) + " located in " + javaTestClass.toString(e, debug);
	}

}
