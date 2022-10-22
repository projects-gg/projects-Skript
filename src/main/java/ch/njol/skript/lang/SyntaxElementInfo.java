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

/**
 * @author Peter Güttinger
 * @param <E> the syntax element this info is for
 */
public class SyntaxElementInfo<E extends SyntaxElement> extends org.skriptlang.skript.lang.SyntaxElementInfo<E> {

	public final Class<E> c;
	public final String[] patterns;
	public final String originClassPath;
	
	public SyntaxElementInfo(final String[] patterns, final Class<E> c, final String originClassPath) throws IllegalArgumentException {
		super(c, originClassPath, patterns);
		this.c = c;
		this.patterns = patterns;
		this.originClassPath = originClassPath;
	}
	
	/**
	 * Get the class that represents this element.
	 * @return The Class of the element
	 */
	@Override
	public Class<E> getElementClass() {
		return c;
	}

}
