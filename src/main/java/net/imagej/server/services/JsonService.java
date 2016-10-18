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

package net.imagej.server.services;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.Arrays;

import net.imagej.server.mixins.Mixins;
import net.imglib2.EuclideanSpace;

/**
 * Service that handle customized JSON serialization and deserialization.
 * 
 * @author Leon Yang
 */
public class JsonService {

	private static final Class<?>[] NOT_SERIALIZED = { EuclideanSpace.class };

	/**
	 * Customized ObjectMapper for serializing unsupported Objects into UUID using
	 * ObjectService.
	 */
	private final ObjectMapper objToIdMapper;

	/**
	 * Customized deserializer on top of the Jackson default deserializer for
	 * untyped Objects. It replaces Strings in specific format with Objects in the
	 * ObjectService. Could be injected into different ObjectMappers.
	 */
	private final UntypedObjectDeserializer idToObjDeserializer;

	/**
	 * Constructs and initializes a JsonService with an {@link ObjectService}.
	 * 
	 * @param objectService
	 */
	public JsonService(final ObjectService objectService) {

		idToObjDeserializer = new UntypedObjectDeserializer(null, null) {

			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException
			{
				final Object obj = super.deserialize(p, ctxt);
				if (!(obj instanceof String && ((String) obj).startsWith("_obj_")))
					return obj;
				final String uuid = ((String) obj).substring(5);
				if (!objectService.contains(uuid)) {
					throw new JsonMappingException(p, "Object does not exist");
				}
				return objectService.find(uuid);
			}
		};

		final JsonSerializer<Object> objToIdSerializer =
			new JsonSerializer<Object>()
		{

				@Override
				public void serialize(Object value, JsonGenerator gen,
					SerializerProvider serializers) throws IOException,
					JsonProcessingException
			{
					gen.writeString("_obj_" + objectService.register(value));
				}

			};

		final SimpleModule objToIdModule = new SimpleModule();
		objToIdModule.setSerializerModifier(new BeanSerializerModifier() {

			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config,
				BeanDescription beanDesc, JsonSerializer<?> serializer)
			{
				// If the serialized class is unknown (i.e. serialized using the general
				// BeanSerializer) or should not be serialized (i.e. complicated class
				// implemented interfaces such as Iterable), would be serialized as an
				// UUID.
				if (serializer instanceof BeanSerializer) return objToIdSerializer;
				if (notSerialized(beanDesc.getBeanClass())) return objToIdSerializer;
				return serializer;

			}
		});
		objToIdMapper = new ObjectMapper();
		objToIdMapper.registerModule(objToIdModule);

		// register Jackson MixIns to obtain better json output format for some
		// specific types
		Mixins.registerMixIns(objToIdMapper);
	}

	/**
	 * Adds a deserializer for UUID Strings to a given {@link ObjectMapper}. The
	 * deserializer would substitute the UUID with its corresponding Object using
	 * {@link ObjectService}.
	 * 
	 * @param objectMapper
	 */
	public void addDeserializerTo(final ObjectMapper objectMapper) {
		final SimpleModule module = new SimpleModule();
		module.addDeserializer(Object.class, idToObjDeserializer);
		objectMapper.registerModule(module);
	}

	/**
	 * Parses the given Object using by substituting all of its nested unknown
	 * components (i.e. no Jackson Annotation and no dedicated serializer) with an
	 * UUID String. The UUID is obtained by registering the Object in
	 * {@link ObjectService} and can be used to retrieve the Object.
	 * 
	 * @param obj Object to be parsed.
	 * @return a JSON format String of the parsed Object
	 * @throws JsonProcessingException
	 */
	public String parseObject(final Object obj) throws JsonProcessingException {
		return objToIdMapper.writeValueAsString(obj);
	}

	public boolean notSerialized(final Class<?> target) {
		return Arrays.stream(NOT_SERIALIZED).anyMatch(clazz -> clazz
			.isAssignableFrom(target));
	}

}
