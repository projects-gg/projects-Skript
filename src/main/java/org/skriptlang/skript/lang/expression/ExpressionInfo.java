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
package org.skriptlang.skript.lang.expression;

import org.skriptlang.skript.lang.SyntaxElementInfo;

public class ExpressionInfo<Element extends Expression<Type>, Type> extends SyntaxElementInfo<Element> {

	private final Class<Type> returnType;
	private final ExpressionType expressionType;

	public ExpressionInfo(
		Class<Element> elementClass, String originClassPath,
		Class<Type> returnType, ExpressionType expressionType, String... patterns
	) throws IllegalArgumentException {
		super(elementClass, originClassPath, patterns);
		this.returnType = returnType;
		this.expressionType = expressionType;
	}

	/**
	 * @return The return type of the {@link Expression} represented by this info.
	 */
	public Class<Type> getReturnType() {
		return returnType;
	}

	/**
	 * @return The type of the {@link Expression} represented by this info.
	 */
	public ExpressionType getExpressionType() {
		return expressionType;
	}

}
