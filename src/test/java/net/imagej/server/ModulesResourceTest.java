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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.testing.junit.ResourceTestRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCI;
import net.imagej.ops.stats.DefaultSum;
import net.imagej.server.resources.ModulesResource;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.ClassRule;
import org.junit.Test;
import org.scijava.Identifiable;
import org.scijava.ItemIO;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Test for {@link ModulesResource}.
 * 
 * @author Leon Yang
 */
public class ModulesResourceTest extends AbstractResourceTest {

	@ClassRule
	public static final ResourceTestRule resources = resourcesBuilder.addProvider(
		ModulesResource.class).build();

	@Test
	public void retrieveModules() {
		final List<?> res = resources.client().target("/modules").request().get(
			List.class);

		final HashSet<?> ids = new HashSet<>(res);
		final List<ModuleInfo> mInfos = ctx.getService(ModuleService.class)
			.getModules();

		for (final ModuleInfo mInfo : mInfos) {
			final String id = ((Identifiable) mInfo).getIdentifier();
			assertTrue(ids.contains(id));
			ids.remove(id);
		}
		assertTrue(ids.isEmpty());
	}

	@Test
	public void getWidget() {
		final String[] ids = { "command:net.imagej.server.external.ScriptEval",
		"command:net.imagej.server.ModulesResourceTest$Bar" };
		for (final String id : ids) {
			final String response = resources.client().target("/modules/" + id)
				.request().get(String.class);
			String expected;
			try {
				expected = jsonService.parseObject(ctx.getService(ModuleService.class)
					.getModuleById(id));
				if (!expected.equals(response)) {
					System.out.println("Expected:" + expected);
					System.out.println("Response:" + response);
				}

				// Convert JSON strings to Java collections, for easier interrogation.
				final Map<?, ?> expectedMap = jsonToMap(expected);
				final Map<?, ?> responseMap = jsonToMap(response);

				// Check that extra information is present in actual response.
				// Remove the extra fields, so that we can compare more easily.
				final String[] extraFields = {"isResolved", "startingValue"};
				for (final Object input : (Iterable<?>) responseMap.get("inputs")) {
					final Map<?, ?> inputMap = (Map<?, ?>) input;
					for (final String field : extraFields) {
						assertTrue(inputMap.containsKey(field));
						inputMap.remove(field);
					}
				}

				// Assert JSON objects are now equal.
				assertEquals(expectedMap, responseMap);
			}
			catch (JsonProcessingException exc) {
				fail(exc.getMessage());
			}

		}
	}

	@Test
	public void runModule() {
		final String imgId;
		final ArrayImg<FloatType, FloatArray> img;
		// create random Img using ScriptEval
		{
			final String id = "command:net.imagej.server.external.ScriptEval";
			final String createImg = "" + //
				"#@ float[] arr\n" + //
				"#@ long[] dims\n" + //
				"#@output Img out\n" + //
				"from net.imglib2.img.array import ArrayImgs\n" + //
				"out = ArrayImgs.floats(list(arr), list(dims))\n";

			final HashMap<String, Object> inputs = new HashMap<>();
			inputs.put("language", "python");
			inputs.put("script", createImg);

			final HashMap<String, Object> scriptInputs = new HashMap<>();
			// generate content of random Img
			final float[] array = new float[100];
			final Random rnd = new Random(0xcaffee1234567890L);
			for (int i = 0; i < 100; i++) {
				array[i] = rnd.nextFloat();
			}

			img = ArrayImgs.floats(array, 10, 10);
			scriptInputs.put("arr", array);
			scriptInputs.put("dims", new long[] { 10, 10 });
			inputs.put("args", scriptInputs);

			final String result = runModule(id, inputs);

			// expect JSON output: {"outputs":{"out":"IMG_ID"}}
			final Matcher matcher = Pattern.compile(
				"\\{\"outputs\":\\{\"out\":\"([^\"]+)\"\\}\\}").matcher(result);
			assertTrue(matcher.find());
			imgId = matcher.group(1);
		}

		final double sum;
		// find the sum of pixels of the created Img
		{
			final String id = "command:net.imagej.ops.stats.DefaultSum";

			final HashMap<String, Object> inputs = new HashMap<>();
			inputs.put("in", imgId);

			final String result = runModule(id, inputs);

			// expect JSON output: {"out":SUM_OF_PIXELS}
			final Matcher matcher = Pattern.compile("\\{\"out\":([0-9.]+)\\}")
				.matcher(result);
			assertTrue(matcher.find());
			sum = Double.valueOf(matcher.group(1));
		}

		final double expectedSum = ((DoubleType) ctx.service(OpService.class).run(
			DefaultSum.class, img)).get();
		assertEquals(expectedSum, sum, 1e-8);
	}

	// -- helper methods --

	private String runModule(final String id, final Map<String, Object> inputs) {
		return resources.client().target("/modules/" + id).request().post(Entity
			.entity(inputs, MediaType.APPLICATION_JSON), String.class);
	}

	private static Map<?, ?> jsonToMap(final String json) {
		try {
			return new ObjectMapper().readValue(json, Map.class);
		}
		catch (final IOException exc) {
			throw new IllegalArgumentException(exc);
		}
	}

	// -- helper classes --

	public static interface Foo extends Op {
		// NB: Marker interface.
	}

	@Plugin(type = Foo.class, name = "test.bar")
	public static class Bar<I, O extends I> extends AbstractUnaryHybridCI<I, O> {

		@Parameter(required = false, choices = { "Apple", "Orange", "Pear" },
			label = "Fake", description = "The fruit I like.", type = ItemIO.BOTH)
		private String fruit;

		@Parameter(min = "0", max = "100", type = ItemIO.OUTPUT)
		private int score = 60;

		@Override
		public void compute(I input, O output) {
			// TODO Auto-generated method stub

		}
	}
}
