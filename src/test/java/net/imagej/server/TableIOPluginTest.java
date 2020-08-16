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

package net.imagej.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.io.handle.BytesHandle;
import org.scijava.io.location.BytesLocation;
import org.scijava.table.Table;
import org.scijava.table.io.TableIOOptions;
import org.scijava.table.io.TableIOPlugin;
import org.scijava.table.io.TableIOService;

/**
 * Tests for {@link TableIOPlugin}.
 * 
 * @author Leon Yang
 */
public class TableIOPluginTest {

	private static final Context ctx = new Context();

	/**
	 * Tests if the parser works on a common tab-delimited table.
	 */
	@Test
	public void testParser() {
		final String[][] cells = { { "col1", "col2", "col3", "col4", "col5" }, {
			"123", "-123.0", "+123.0f", "0123.0d", "0.0" }, { "00000",
				"1234567890.0987654321", "+NaN", "-Infinity", "000.000" } };
		final String tableSource = makeTableSource(cells, "\t", "\n");

		final String[] colHeaders = cells[0];
		final String[] rowHeaders = { null, null };
		final Double[][] content = { { 123.0, -123.0, 123.0, 123.0, 0.0 }, { 0.0,
			1234567890.0987654321, Double.NaN, Double.NEGATIVE_INFINITY, 0.0 } };

		final String expected = "col1\tcol2\tcol3\tcol4\tcol5\n" +
			"123.000\t-123.000\t123.000\t123.000\t0.000\n" +
			"0.000\t1234567890.099\tNaN\t-Infinity\t0.000\n";

		final TableIOService tableIO = ctx.service(TableIOService.class);
		try {
			final Function<String, Double> parser = Double::valueOf;
			final Function<Object, String> formatter = val -> String.format("%.3f",
					(Double)val);

			TableIOOptions options = TableIOOptions.options()
					.readColumnHeaders(true)
					.writeColumnHeaders(true)
					.readRowHeaders(false)
					.writeRowHeaders(false)
					.columnDelimiter('\t')
					.rowDelimiter("\n")
					.quote('\"')
					.cornerText("\\")
					.parser(parser)
					.formatter(formatter);

			final Table table = openTable(tableSource, tableIO, options);
			assertTableEquals(colHeaders, rowHeaders, content, table);
			assertEquals(expected, saveTable(table, tableIO, options));
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			fail(exc.getMessage());
		}
	}

	/**
	 * Tests if quoting works in different senarios.
	 */
	@Test
	public void testQuote() {
		final String[][] cells = { { "CORNER_TEXT",
			"' col  1 with white   spaces '", "'col 2 with ''QUOTE'' inside'",
			"'col 3 'connect,two' quoted strings'" }, { "should\tnot,break",
				"'unnecessary_quotes'", "should break" }, { "some,empty,cells", "",
					" ''" } };
		final String tableSource = makeTableSource(cells, " ", "\r\n");

		final String[] colHeaders = { " col  1 with white   spaces ",
			"col 2 with 'QUOTE' inside", "col 3 connect,two quoted strings" };
		final String[] rowHeaders = { "should\tnot,break", "some,empty,cells" };
		final String[][] content = { { "unnecessary_quotes", "should", "break" }, {
			"", "", "" } };

		final String expected = "CORNER_TEXT, col  1 with white   spaces ," +
			"'col 2 with ''QUOTE'' inside'," +
			"'col 3 connect,two quoted strings'\r\n" +
			"'should\tnot,break',unnecessary_quotes,should,break\r\n" +
			"'some,empty,cells','','',''\r\n";

		final TableIOService tableIO = ctx.service(TableIOService.class);
		try {
			TableIOOptions options = TableIOOptions.options()
					.readColumnHeaders(true)
					.writeColumnHeaders(true)
					.readRowHeaders(true)
					.writeRowHeaders(true)
					.columnDelimiter(' ')
					.rowDelimiter("\r\n")
					.quote('\'')
					.cornerText("CORNER_TEXT")
					.parser(Function.identity())
					.formatter(Object::toString);

			final Table table = openTable(tableSource, tableIO, options);
			assertTableEquals(colHeaders, rowHeaders, content, table);

			options.columnDelimiter(',');
			assertEquals(expected, saveTable(table, tableIO, options));
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			fail(exc.getMessage());
		}
	}

	/**
	 * Tests if samll tables could be opened/saved correctly.
	 */
	@Test
	public void testSmallTables() {
		final String[][] singleRow = { { "Row Header", "   3.1415926   " } };
		final String[][] singleCell = { { "   3.1415926   " } };
		final String[][] singleCol = { { "Col Header" }, { "   3.1415926   " } };
		final String[][] onlyRowHeader = { { "CORNER TEXT" }, { "Row Header" } };
		final String[][] onlyColHeader = { { "CORNER TEXT", "Col Header" } };
		final String[][] full = { { "CORNER TEXT", "Col Header" }, { "Row Header",
			"   3.1415926   " } };

		final String[] singleColHeader = { "Col Header" };
		final String[] singleRowHeader = { "Row Header" };
		final String[] emptyHeader = { null };
		final String[] empty = {};
		final Double[][] content = { { 3.1415926 } };
		final Double[][] emptyContent = { {} };

		final TableIOService tableIO = ctx.service(TableIOService.class);
		try {
			Table table;
			String expected;
			final Function<String, Double> parser = Double::valueOf;
			final Function<Object, String> formatter = val -> String.format("%.3f",
					(Double)val);
			TableIOOptions options = TableIOOptions.options()
					.readColumnHeaders(false)
					.writeColumnHeaders(false)
					.readRowHeaders(true)
					.writeRowHeaders(true)
					.columnDelimiter(',')
					.rowDelimiter("\n")
					.quote('\'')
					.cornerText("CORNER TEXT")
					.parser(parser)
					.formatter(formatter);
			table = openTable(makeTableSource(singleRow, ",", "\n"), tableIO, options);
			assertTableEquals(emptyHeader, singleRowHeader, content, table);
			expected = "Row Header,3.142\n";
			assertEquals(expected, saveTable(table, tableIO, options));

			options.readRowHeaders(false);
			options.writeRowHeaders(false);
			table = openTable(makeTableSource(singleCell, ",", "\n"), tableIO, options);
			assertTableEquals(emptyHeader, emptyHeader, content, table);
			expected = "3.142\n";
			assertEquals(expected, saveTable(table, tableIO, options));

			options.readColumnHeaders(true);
			options.writeColumnHeaders(true);
			table = openTable(makeTableSource(singleCol, ",", "\n"), tableIO, options);
			assertTableEquals(singleColHeader, emptyHeader, content, table);
			expected = "Col Header\n3.142\n";
			assertEquals(expected, saveTable(table, tableIO, options));

			options.readRowHeaders(true);
			table = openTable(makeTableSource(onlyColHeader, ",", "\n"), tableIO, options);
			assertTableEquals(singleColHeader, empty, emptyContent, table);
			expected = "Col Header\n";
			assertEquals(expected, saveTable(table, tableIO, options));

			options.writeColumnHeaders(false);
			options.writeRowHeaders(true);
			table = openTable(makeTableSource(onlyRowHeader, ",", "\n"), tableIO, options);
			assertTableEquals(empty, singleRowHeader, emptyContent, table);
			expected = "Row Header\n";
			assertEquals(expected, saveTable(table, tableIO, options));

			options.writeColumnHeaders(true);
			table = openTable(makeTableSource(full, ",", "\n"), tableIO, options);
			assertTableEquals(singleColHeader, singleRowHeader, content, table);
			expected = "CORNER TEXT,Col Header\nRow Header,3.142\n";
			assertEquals(expected, saveTable(table, tableIO, options));
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			fail(exc.getMessage());
		}

	}

	@Test(expected = IOException.class)
	public void testOpenNonExist() throws IOException {
		final TableIOService tableIO = ctx.service(TableIOService.class);
		tableIO.open("fake.csv");
	}

	// -- helper methods --

	/**
	 * Checks if a table has the expected column/row headers and content.
	 */
	private void assertTableEquals(final String[] colHeaders,
		final String[] rowHeaders, final Object[][] content,
		final Table table)
	{
		assertEquals(colHeaders.length, table.getColumnCount());
		assertEquals(rowHeaders.length, table.getRowCount());
		for (int c = 0; c < colHeaders.length; c++) {
			assertEquals(colHeaders[c], table.getColumnHeader(c));
			for (int r = 0; r < rowHeaders.length; r++) {
				assertEquals(content[r][c], table.get(c, r));
			}
		}
		for (int r = 0; r < rowHeaders.length; r++) {
			assertEquals(rowHeaders[r], table.getRowHeader(r));
		}
	}

	private Table openTable(final String tableSource,
		final TableIOService tableIO, final TableIOOptions options) throws IOException
	{
		final BytesHandle bah = new BytesHandle();
		BytesLocation data = new BytesLocation(tableSource.getBytes(), "table.csv");
		bah.set(data);
		return tableIO.open(bah.get(), options);
	}

	private String saveTable(final Table table,
	                         final TableIOService tableIO, TableIOOptions options) throws IOException
	{
		final BytesHandle bah = new BytesHandle();
		bah.set(new BytesLocation(1024, "table.csv"));
		tableIO.save(table, bah.get(), options);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = bah.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}
		return new String(out.toByteArray(), 0, (int) bah.length());
	}

	private String makeTableSource(final String[][] cells, final String separator,
		final String eol)
	{
		final StringBuilder table = new StringBuilder();
		for (final String[] row : cells) {
			table.append(String.join(separator, row)).append(eol);
		}
		return table.toString();
	}
}
