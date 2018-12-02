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

import static org.junit.Assume.assumeTrue;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

import io.scif.io.ByteArrayHandle;
import io.scif.services.LocationService;

import java.io.IOException;

import net.imagej.server.external.DefaultTableIOPlugin;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.scijava.Context;
import org.scijava.io.IOPlugin;
import org.scijava.io.IOService;
import org.scijava.table.GenericTable;
import org.scijava.util.MersenneTwisterFast;

/**
 * @author Leon Yang
 */
@BenchmarkOptions(benchmarkRounds = 20, warmupRounds = 1)
public class DefaultTableIOPluginBenchmark {

	private boolean benchmarkTestsEnabled = "enabled".equals(System.getProperty(
		"imagej.server.benchmark.tests"));

	@Before
	public void skipBenchmarksByDefault() {
		assumeTrue(benchmarkTestsEnabled);
	}

	@BeforeClass
	public static void prepare() {
		final StringBuilder sb = new StringBuilder(10 * 1024 * 1024);
		for (int i = 0; i < 1023; i++) {
			sb.append(String.format("%09d,", i));
		}
		sb.append(String.format("%08d\r\n", 1023));
		final MersenneTwisterFast r = new MersenneTwisterFast();
		for (int i = 0; i < 1023; i++) {
			for (int j = 0; j < 1023; j++) {
				sb.append(String.format("%.7f,", r.nextFloat()));
			}
			sb.append(String.format("%.6f\r\n", r.nextFloat()));
		}
		final ByteArrayHandle bah = new ByteArrayHandle(sb.toString().getBytes());
		ctx.getService(LocationService.class).mapFile("large.csv", bah);
	}

	/** Needed for JUnit-Benchmarks */
	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	private static final Context ctx = new Context();

	@Test
	public void openLarge() {
		final IOPlugin<GenericTable> tableIO = ctx.service(IOService.class)
			.getInstance(DefaultTableIOPlugin.class);
		try {
			tableIO.open("large.csv");
		}
		catch (IOException exc) {
			exc.printStackTrace();
		}
	}
}
