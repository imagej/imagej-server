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

import io.scif.io.ByteArrayHandle;
import io.scif.services.LocationService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import net.imagej.server.external.DefaultTableIOPlugin;
import net.imagej.table.GenericTable;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.io.IOPlugin;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.util.ClassUtils;

/**
 * Tests for {@link DefaultTableIOPlugin}.
 * 
 * @author Leon Yang
 */
public class DefaultTableIOPluginTest {

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

		final IOPlugin<GenericTable> tableIO = ctx.service(IOService.class)
			.getInstance(DefaultTableIOPlugin.class);
		try {
			final Function<String, Double> parser = Double::valueOf;
			final Function<Double, String> formatter = val -> String.format("%.3f",
				val);
			setValues(tableIO, new String[] { "readColHeaders", "writeColHeaders",
				"readRowHeaders", "writeRowHeaders", "separator", "eol", "quote",
				"cornerText", "parser", "formatter" }, new Object[] { true, true, false,
					true, "\t", "\n", "\"", "\\", parser, formatter });

			final GenericTable table = openTable(tableSource, tableIO);
			assertTableEquals(colHeaders, rowHeaders, content, table);
			assertEquals(expected, saveTable(table, tableIO));
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

		final IOPlugin<GenericTable> tableIO = ctx.service(IOService.class)
			.getInstance(DefaultTableIOPlugin.class);
		try {
			setValues(tableIO, new String[] { "readColHeaders", "writeColHeaders",
				"readRowHeaders", "writeRowHeaders", "separator", "eol", "quote",
				"cornerText", "parser", "formatter" }, new Object[] { true, true, true,
					true, " ", "\r\n", '\'', "CORNER_TEXT", Function.identity(), Function
						.identity() });

			final GenericTable table = openTable(tableSource, tableIO);
			assertTableEquals(colHeaders, rowHeaders, content, table);

			setValues(tableIO, new String[] { "separator" }, new Object[] { ',' });
			assertEquals(expected, saveTable(table, tableIO));
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

		final IOPlugin<GenericTable> tableIO = ctx.service(IOService.class)
			.getInstance(DefaultTableIOPlugin.class);
		try {
			GenericTable table;
			String expected;
			final Function<String, Double> parser = Double::valueOf;
			final Function<Double, String> formatter = val -> String.format("%.3f",
				val);
			setValues(tableIO, new String[] { "readColHeaders", "writeColHeaders",
				"readRowHeaders", "writeRowHeaders", "separator", "eol", "quote",
				"cornerText", "parser", "formatter" }, new Object[] { false, true, true,
					true, ",", "\n", "'", "CORNER TEXT", parser, formatter });
			table = openTable(makeTableSource(singleRow, ",", "\n"), tableIO);
			assertTableEquals(emptyHeader, singleRowHeader, content, table);
			expected = "Row Header,3.142\n";
			assertEquals(expected, saveTable(table, tableIO));

			setValues(tableIO, new String[] { "readRowHeaders" }, new Object[] {
				false });
			table = openTable(makeTableSource(singleCell, ",", "\n"), tableIO);
			assertTableEquals(emptyHeader, emptyHeader, content, table);
			expected = "3.142\n";
			assertEquals(expected, saveTable(table, tableIO));

			setValues(tableIO, new String[] { "readColHeaders" }, new Object[] {
				true });
			table = openTable(makeTableSource(singleCol, ",", "\n"), tableIO);
			assertTableEquals(singleColHeader, emptyHeader, content, table);
			expected = "Col Header\n3.142\n";
			assertEquals(expected, saveTable(table, tableIO));

			setValues(tableIO, new String[] { "readRowHeaders" }, new Object[] {
				true });
			table = openTable(makeTableSource(onlyColHeader, ",", "\n"), tableIO);
			assertTableEquals(singleColHeader, empty, emptyContent, table);
			expected = "Col Header\n";
			assertEquals(expected, saveTable(table, tableIO));

			table = openTable(makeTableSource(onlyRowHeader, ",", "\n"), tableIO);
			assertTableEquals(empty, singleRowHeader, emptyContent, table);
			expected = "Row Header\n";
			assertEquals(expected, saveTable(table, tableIO));

			table = openTable(makeTableSource(full, ",", "\n"), tableIO);
			assertTableEquals(singleColHeader, singleRowHeader, content, table);
			expected = "CORNER TEXT,Col Header\nRow Header,3.142\n";
			assertEquals(expected, saveTable(table, tableIO));
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			fail(exc.getMessage());
		}

	}

	@Test(expected = IOException.class)
	public void testOpenNonExist() throws IOException {
		final IOPlugin<GenericTable> tableIO = ctx.service(IOService.class)
			.getInstance(DefaultTableIOPlugin.class);
		tableIO.open("fake.csv");
	}

	// -- helper methods --

	/**
	 * Checks if a table has the expected column/row headers and content.
	 */
	private void assertTableEquals(final String[] colHeaders,
		final String[] rowHeaders, final Object[][] content,
		final GenericTable table)
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

	private GenericTable openTable(final String tableSource,
		final IOPlugin<GenericTable> tableIO) throws IOException
	{
		final ByteArrayHandle bah = new ByteArrayHandle(tableSource.getBytes());
		ctx.service(LocationService.class).mapFile("table.txt", bah);
		return tableIO.open("table.txt");
	}

	private String saveTable(final GenericTable table,
		final IOPlugin<GenericTable> tableIO) throws IOException
	{
		final ByteArrayHandle bah = new ByteArrayHandle();
		ctx.service(LocationService.class).mapFile("table.txt", bah);
		tableIO.save(table, "table.txt");
		return new String(bah.getBytes(), 0, (int) bah.length());
	}

	private void setValues(final Object instance, final String[] fieldNames,
		final Object[] values) throws SecurityException
	{
		final Class<?> cls = instance.getClass();
		final List<Field> fields = ClassUtils.getAnnotatedFields(cls,
			Parameter.class);
		final HashMap<String, Field> fieldMap = new HashMap<>();
		for (final Field field : fields) {
			fieldMap.put(field.getName(), field);
		}
		for (int i = 0; i < fieldNames.length; i++) {
			ClassUtils.setValue(fieldMap.get(fieldNames[i]), instance, values[i]);
		}
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
