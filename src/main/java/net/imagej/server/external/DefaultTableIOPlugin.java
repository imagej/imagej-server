/*
 * #%L
 * ImageJ server for RESTful access to ImageJ.
 * %%
 * Copyright (C) 2013 - 2016 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package net.imagej.server.external;

import io.scif.io.IRandomAccess;
import io.scif.io.VirtualHandle;
import io.scif.services.LocationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import net.imagej.table.DefaultGenericTable;
import net.imagej.table.GenericTable;

import org.scijava.Priority;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.FileUtils;

/**
 * Plugin for reading/writing {@link GenericTable}s.
 * 
 * @author Leon Yang
 */
@Plugin(type = IOPlugin.class, priority = Priority.LOW_PRIORITY)
public class DefaultTableIOPlugin extends AbstractIOPlugin<GenericTable> {

	@Parameter
	private LocationService locationService;

	/** Reads the first row of the input file as column headers. */
	@Parameter(required = false)
	private boolean readColHeaders = true;

	/** Writes column headers to file if there exists at least one. */
	@Parameter(required = false)
	private boolean writeColHeaders = true;

	/** Reads the first column of the input file as row headers. */
	@Parameter(required = false)
	private boolean readRowHeaders = false;

	/** Writes row headers to file if there exists at least one. */
	@Parameter(required = false)
	private boolean writeRowHeaders = true;

	/** Regex pattern that separates cells in each row of the table. */
	@Parameter(required = false)
	private String separator = ",";

	/** End of line when writing to file. */
	@Parameter(required = false)
	private String eol = System.lineSeparator();

	/** Skips empty lines. */
	@Parameter(required = false)
	private boolean skipEmpty = true;

	/**
	 * Quote character used for escaping separator and empty strings. Use two
	 * consecutive quotes to escape one.
	 */
	@Parameter(required = false)
	private char quote = '"';

	/**
	 * Text that appears at the top left corner when both column and row headers
	 * present.
	 */
	@Parameter(required = false)
	private String cornerText = "\\";

	/**
	 * Lambda function that converts the string of a cell to an appropriate value.
	 */
	@Parameter(required = false)
	private Function<String, Object> parser = s -> s;

	/** Lambda function that convert the cell content to a string. */
	@Parameter(required = false)
	private Function<Object, String> formatter = o -> o.toString();

	// FIXME: The "txt" extension is extremely general and will conflict with
	// other plugins. Consider another way to check supportsOpen/Close.
	private static final Set<String> SUPPORTED_EXTENSIONS = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList("csv", "txt", "prn", "dif",
			"rtf")));

	@Override
	public Class<GenericTable> getDataType() {
		return GenericTable.class;
	}

	@Override
	public boolean supportsOpen(final String source) {
		final String ext = FileUtils.getExtension(source).toLowerCase();
		return SUPPORTED_EXTENSIONS.contains(ext);
	}

	@Override
	public boolean supportsSave(final String source) {
		return supportsOpen(source);
	}

	@Override
	public GenericTable open(final String source) throws IOException {
		final IRandomAccess handle = locationService.getHandle(source, false);
		if (handle instanceof VirtualHandle) {
			throw new IOException("Cannot open source");
		}
		final byte[] buffer = new byte[(int) handle.length()];
		handle.read(buffer);
		final String text = new String(buffer);

		final GenericTable table = new DefaultGenericTable();
		final Pattern pattern = Pattern.compile(String.format(
			"(%1$s)|%2$c([^%2$c]*)%2$c|(%3$s)|(.)", eol, quote, separator));
		final Matcher m = pattern.matcher(text);

		int numCols = -1;
		int row = 0;
		final ArrayList<String> tokens = new ArrayList<>();
		final StringBuilder sb = new StringBuilder();
		// To find consecutive quotes
		boolean wasQuoted = false;
		// Used to differentiate empty lines and lines with empty cells
		boolean lineEmpty = true;
		while (true) {
			// EOL or EOF
			if (!m.find() || m.group(1) != null) {
				if (!lineEmpty) tokens.add(sb.toString());
				sb.setLength(0);
				lineEmpty = true;
				wasQuoted = false;
				if (tokens.size() == 0) {
					if (m.hitEnd()) break;
					if (!skipEmpty) table.appendRow();
					continue;
				}
				final String rowHeader = readRowHeaders ? tokens.remove(0) : null;
				if (numCols == -1) {
					numCols = tokens.size();
					if (readColHeaders) {
						table.appendColumns(tokens.toArray(new String[numCols]));
						tokens.clear();
						continue;
					}
					table.appendColumns(numCols);
				}
				if (tokens.size() != numCols) {
					throw new IOException("Line " + row +
						" is not the same length as the first line.");
				}
				if (readRowHeaders) table.appendRow(rowHeader);
				else table.appendRow();
				for (int col = 0; col < numCols; col++) {
					table.set(col, row, parser.apply(tokens.get(col)));
				}
				row++;
				tokens.clear();
			}
			// Quoted
			else if (m.group(2) != null) {
				// Two consecutive quotes that are not paired escape one quote
				if (wasQuoted) sb.append(quote);
				sb.append(m.group(2));
				lineEmpty = false;
				wasQuoted = true;
			}
			// Separator
			else if (m.group(3) != null) {
				tokens.add(sb.toString());
				sb.setLength(0);
				lineEmpty = false;
				wasQuoted = false;
			}
			// Single character
			else {
				sb.append(m.group(4));
				lineEmpty = false;
				wasQuoted = false;
			}
		}
		return table;
	}

	@Override
	public void save(final GenericTable table, final String source)
		throws IOException
	{
		final IRandomAccess handle = locationService.getHandle(source, true);
		if (handle instanceof VirtualHandle) {
			throw new IOException("Cannot open source");
		}

		final boolean writeRH = this.writeRowHeaders && table.getRowCount() > 0 &&
			IntStream.range(0, table.getRowCount()).allMatch(row -> table
				.getRowHeader(row) != null);
		final boolean writeCH = this.writeColHeaders && table
			.getColumnCount() > 0 && table.stream().allMatch(col -> col
				.getHeader() != null);

		final StringBuilder sb = new StringBuilder();
		// write column headers
		if (writeCH) {
			if (writeRH) {
				sb.append(tryQuote(cornerText));
				if (table.getColumnCount() > 0) {
					sb.append(separator);
					sb.append(tryQuote(table.getColumnHeader(0)));
				}
			}
			// avoid adding extra separator when there is 0 column
			else if (table.getColumnCount() > 0) {
				sb.append(tryQuote(table.getColumnHeader(0)));
			}
			for (int col = 1; col < table.getColumnCount(); col++) {
				sb.append(separator);
				sb.append(tryQuote(table.getColumnHeader(col)));
			}
			sb.append(eol);
			handle.writeBytes(sb.toString());
			sb.setLength(0);
		}
		// write each row
		for (int row = 0; row < table.getRowCount(); row++) {
			if (writeRH) {
				sb.append(tryQuote(table.getRowHeader(row)));
				if (table.getColumnCount() > 0) {
					sb.append(separator);
					sb.append(tryQuote(formatter.apply(table.get(0, row))));
				}
			}
			// avoid adding extra separator when there is 0 column
			else if (table.getColumnCount() > 0) {
				sb.append(tryQuote(formatter.apply(table.get(0, row))));
			}
			for (int col = 1; col < table.getColumnCount(); col++) {
				sb.append(separator);
				sb.append(tryQuote(formatter.apply(table.get(col, row))));
			}
			sb.append(eol);
			handle.writeBytes(sb.toString());
			sb.setLength(0);
		}
	}

	/**
	 * Try to quote a string if:
	 * <li>it is null or empty</li>
	 * <li>it has quotes inside</li>
	 * <li>it has separators or EOL inside</li>
	 * 
	 * @param str string to quote
	 * @return string, possibly quoted
	 */
	private String tryQuote(final String str) {
		if (str == null || str.length() == 0) return "" + quote + quote;
		if (str.indexOf(quote) != -1) return quote + str.replace("" + quote, "" +
			quote + quote) + quote;
		if (str.contains(separator) || str.contains(eol)) return quote + str +
			quote;
		return str;
	}
}
