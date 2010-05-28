/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.ctl;

/**
 * An error location within CTL code.
 *
 * @author Martin Janik, Javlin a.s. &lt;martin.janik@javlin.eu&gt;
 *
 * @version 26th May 2010
 * @created 26th May 2010
 *
 * @see ProblemReporter
 */
public final class ErrorLocation {

	/** The line where the location begins. */
	private final int beginLine;
	/** The column where the location begins. */
	private final int beginColumn;
	/** The line where the location ends. */
	private final int endLine;
	/** The column where the location ends. */
	private final int endColumn;

	/**
	 * Constructs an <code>ErrorLocation</code> instance for given syntactic positions.
	 *
	 * @param begin the syntactic position where the location begins
	 * @param end the syntactic position where the location ends
	 */
	public ErrorLocation(SyntacticPosition begin, SyntacticPosition end) {
		this(begin.getLine(), begin.getColumn(), end.getLine(), end.getColumn());
	}

	/**
	 * Constructs an <code>ErrorLocation</code> instance for given lines and columns.
	 *
	 * @param beginLine line where the location begins
	 * @param beginColumn column where the location begins
	 * @param endLine line where the location ends
	 * @param endColumn column where the location ends
	 */
	public ErrorLocation(int beginLine, int beginColumn, int endLine, int endColumn) {
		this.beginLine = beginLine;
		this.beginColumn = beginColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	/**
	 * @return the line where the location begins
	 */
	public int getBeginLine() {
		return beginLine;
	}

	/**
	 * @return column where the location begins
	 */
	public int getBeginColumn() {
		return beginColumn;
	}

	/**
	 * @return line where the location ends
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * @return column where the location ends
	 */
	public int getEndColumn() {
		return endColumn;
	}

	@Override
	public String toString() {
		return "Line " + beginLine + " column " + beginColumn + " - Line " + endLine + " column " + endColumn;
	}

}
