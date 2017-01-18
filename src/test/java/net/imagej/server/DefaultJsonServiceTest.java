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

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.jackson.Jackson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.imagej.ops.create.img.Imgs;
import net.imagej.server.services.DefaultJsonService;
import net.imagej.server.services.ObjectService;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.numeric.complex.ComplexDoubleType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

import org.junit.Before;
import org.junit.Test;

/**
 * Test deserialization and serialization using DefaultJsonService.
 * 
 * @author Leon Yang
 */
public class DefaultJsonServiceTest {

	private final ObjectMapper mapper = Jackson.newObjectMapper();
	private ObjectMapper modifiedMapper;
	private ListObjectService objectService;
	private DefaultJsonService jsonService;

	@Before
	public void setup() {
		modifiedMapper = Jackson.newObjectMapper();
		objectService = new ListObjectService();
		jsonService = new DefaultJsonService(objectService);
		jsonService.addDeserializerTo(modifiedMapper);
	}

	@Test
	public void deserializeBasicTypes() throws Exception {
		final LinkedHashMap<String, Object> inputs = new LinkedHashMap<>();
		inputs.put("integer", 1);
		inputs.put("double", 1.5);
		inputs.put("string", "Hello world");
		inputs.put("listOfOnes", Arrays.asList(1, 1, 1, 1, 1));
		inputs.put("simpleMap", Collections.singletonMap("key", "value"));

		@SuppressWarnings("unchecked")
		final Map<String, Object> deserialized = modifiedMapper.readValue(fixture(
			"fixtures/inputs/basicTypes.json"), Map.class);

		assertEquals(deserialized, inputs);
	}

	@Test
	public void deserializeSpecialTypes() throws Exception {
		final LinkedHashMap<String, Object> inputs = new LinkedHashMap<>();
		final Img<ByteType> img0 = Imgs.create(new ArrayImgFactory<>(), Intervals
			.createMinMax(0, 10, 0, 10), new ByteType());
		final Img<ByteType> img1 = Imgs.create(new PlanarImgFactory<>(), Intervals
			.createMinMax(0, 10, 0, 10), new ByteType());
		final Foo foo = new Foo("test string");
		inputs.put("img0", img0);
		inputs.put("img1", img1);
		inputs.put("foo", foo);

		objectService.register(img0);
		objectService.register(img1);
		objectService.register(foo);

		@SuppressWarnings("unchecked")
		final Map<String, Object> deserialized = modifiedMapper.readValue(fixture(
			"fixtures/inputs/specialTypes.json"), Map.class);

		assertEquals(deserialized, inputs);
	}

	@Test
	public void serializeBasicTypes() throws Exception {
		final LinkedHashMap<String, Object> outputs = new LinkedHashMap<>();
		outputs.put("int", 1);
		outputs.put("byte", (byte) 1);
		outputs.put("short", (short) 1);
		outputs.put("long", 1L);
		outputs.put("BigInteger", new BigInteger("123456789012345678901234567890"));
		outputs.put("float", 1.5f);
		outputs.put("double", 1.5d);
		outputs.put("char", 'c');
		outputs.put("String", "Hello world");
		outputs.put("boolean", true);
		outputs.put("list", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0));

		final String normalized = mapper.writeValueAsString(mapper.readValue(
			fixture("fixtures/outputs/basicTypes.json"), Object.class));

		assertEquals(jsonService.parseObject(outputs), normalized);
	}

	@Test
	public void serializeSpecialTypes() throws Exception {
		// MixIns and special types are tested together here. Could separate them if
		// needed in the future.
		final LinkedHashMap<String, Object> outputs = new LinkedHashMap<>();
		final IntType intType = new IntType(1);
		final ByteType byteType = new ByteType((byte) 1);
		final ShortType shortType = new ShortType((short) 1);
		final LongType longType = new LongType(1L);
		final FloatType floatType = new FloatType(1.5f);
		final DoubleType doubleType = new DoubleType(1.5d);
		final ComplexDoubleType complexDoubleType = new ComplexDoubleType(1.5, 2.5);
		final Img<ByteType> img0 = Imgs.create(new ArrayImgFactory<>(), Intervals
			.createMinMax(0, 10, 0, 10), new ByteType());
		final Img<ByteType> img1 = Imgs.create(new PlanarImgFactory<>(), Intervals
			.createMinMax(0, 10, 0, 10), new ByteType());
		final Foo foo = new Foo("test string");
		outputs.put("intType", intType);
		outputs.put("byteType", byteType);
		outputs.put("shortType", shortType);
		outputs.put("longType", longType);
		outputs.put("floatType", floatType);
		outputs.put("doubleType", doubleType);
		outputs.put("complexDoubleType", complexDoubleType);
		outputs.put("img0", img0);
		outputs.put("img1", img1);
		outputs.put("foo", foo);

		final String normalized = mapper.writeValueAsString(mapper.readValue(
			fixture("fixtures/outputs/specialTypes.json"), Object.class));

		assertEquals(jsonService.parseObject(outputs), normalized);
	}

	// -- helper class --

	/**
	 * ObjectService for testing that generates predictable IDs when registering
	 * an object. However, the same object should not be registered twice,
	 * otherwise different IDs will be returned.
	 * 
	 * @author Leon Yang
	 */
	private static class ListObjectService implements ObjectService {

		private final ArrayList<Object> list = new ArrayList<>();

		@Override
		public String register(Object obj) {
			list.add(obj);
			return String.valueOf(list.size() - 1);
		}

		@Override
		public Object find(String id) {
			return list.get(Integer.valueOf(id));
		}

		@Override
		public boolean contains(String id) {
			return Integer.valueOf(id) < list.size();
		}

	}

	public static class Foo {

		private final String fooString;

		public Foo(final String fooString) {
			this.fooString = fooString;
		}

		public String getFooString() {
			return fooString;
		}
	}

}
