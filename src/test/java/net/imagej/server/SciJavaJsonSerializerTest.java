/*-
 * #%L
 * ImageJ server for RESTful access to ImageJ.
 * %%
 * Copyright (C) 2013 - 2026 Board of Regents of the University of
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

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.imagej.server.json.SciJavaJsonSerializer;
import net.imagej.server.services.DefaultJsonService;
import net.imagej.server.services.DefaultObjectService;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

public class SciJavaJsonSerializerTest {

	@Test
	public void serializeIntervalUsingSciJavaJsonSerializer() throws Exception {

		final ObjectMapper mapper = Jackson.newObjectMapper();
		final DefaultObjectService objectService = new DefaultObjectService();
		final DefaultJsonService jsonService = new DefaultJsonService(new Context(),
			objectService);
		jsonService.addDeserializerTo(mapper);

		final Interval testInterval = new FinalInterval(new long[] { 0, 1 },
			new long[] { 8, 9 });
		final Map<String, Object> outputs = new HashMap<>();
		outputs.put("testInterval", testInterval);

		final String parsed = jsonService.parseObject(outputs);

		final String expected = mapper.writeValueAsString(mapper.readValue(fixture(
			"fixtures/outputs/finalIntervalType.json"), Map.class));

		assertEquals(expected, parsed);

	}

	@Plugin(type = SciJavaJsonSerializer.class)
	public static class ExampleIntervalJsonSerializer implements
		SciJavaJsonSerializer<Interval>
	{

		@Override
		public boolean isSupportedBy(Class<?> desiredClass) {
			return desiredClass.equals(FinalInterval.class);
		}

		@Override
		public void serialize(Interval interval, JsonGenerator gen,
			SerializerProvider serializers) throws IOException
		{

			gen.writeStartObject();
			gen.writeArrayFieldStart("min");
			for (int i = 0; i < interval.numDimensions(); i++) {
				gen.writeNumber(interval.min(i));
			}
			gen.writeEndArray();
			gen.writeArrayFieldStart("max");
			for (int i = 0; i < interval.numDimensions(); i++) {
				gen.writeNumber(interval.max(i));
			}
			gen.writeEndArray();
			gen.writeEndObject();

		}

		@Override
		public Class<Interval> handleType() {
			return Interval.class;
		}

	}

}
