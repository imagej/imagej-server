
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

import net.imagej.Dataset;
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
			return SciJavaJsonSerializer.super.isSupportedBy(desiredClass) &&
				!Dataset.class.isAssignableFrom(desiredClass);
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
