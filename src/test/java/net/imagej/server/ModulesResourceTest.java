/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.server;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.dropwizard.testing.junit.ResourceTestRule;

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
import net.imagej.server.resources.ModulesResource.RunSpec;
import net.imagej.server.services.DefaultJsonService;
import net.imagej.server.services.DefaultObjectService;
import net.imagej.server.services.JsonService;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.scijava.Context;
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
public class ModulesResourceTest {

	private static final Context ctx = new Context();

	private static final JsonService jsonService = new DefaultJsonService(
		new DefaultObjectService());

	@ClassRule
	public static final ResourceTestRule resources = ResourceTestRule.builder()
		.addProvider(new AbstractBinder()
	{

			@Override
			protected void configure() {
				bind(ctx).to(Context.class);
				bind(jsonService).to(JsonService.class);
			}
		}).addProvider(ModulesResource.class).build();

	@BeforeClass
	public static void setup() {
		jsonService.addDeserializerTo(resources.getObjectMapper());
	}

	@Test
	public void retrieveModules() {
		final List<?> res = resources.client().target("/modules").request().get(
			List.class);

		final HashSet<?> ids = new HashSet<>(res);
		final List<ModuleInfo> mInfos = ctx.getService(ModuleService.class)
			.getModules();

		for (final ModuleInfo mInfo : mInfos) {
			if (!(mInfo instanceof Identifiable)) continue;
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
				assertEquals(expected, response);
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
			final String createImg = fixture("fixtures/script/createImg.py");

			final HashMap<String, Object> inputs = new HashMap<>();
			inputs.put("language", "python");
			inputs.put("in1", createImg);

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
			inputs.put("in2", scriptInputs);

			final String result = runModule(id, inputs);

			// expect JSON output: {"out":{"out":"IMG_ID"}}
			final Matcher matcher = Pattern.compile(
				"\\{\"out\":\\{\"out\":\"([^\"]+)\"\\}\\}").matcher(result);
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
		final RunSpec spec = new RunSpec();
		spec.inputs = inputs;
		return resources.client().target("/modules/" + id).request().post(Entity
			.entity(spec, MediaType.APPLICATION_JSON), String.class);
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
