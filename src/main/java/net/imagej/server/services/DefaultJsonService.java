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
public class DefaultJsonService implements JsonService {

	private static final Class<?>[] NOT_SERIALIZED = { EuclideanSpace.class };

	/**
	 * Customized ObjectMapper for serializing unsupported Objects into ID using
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
	public DefaultJsonService(final ObjectService objectService) {

		idToObjDeserializer = new UntypedObjectDeserializer(null, null) {

			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException
			{
				final Object obj = super.deserialize(p, ctxt);
				if (!(obj instanceof String && ((String) obj).startsWith("object:")))
					return obj;
				final String id = (String) obj;
				if (!objectService.contains(id)) {
					throw new JsonMappingException(p, "Object does not exist");
				}
				final ObjectInfo info = objectService.find(id);
				info.updateLastUsed();
				return info.getObject();
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
					gen.writeString(objectService.register(value, "DefaultJsonService"));
				}

			};

		final SimpleModule objToIdModule = new SimpleModule();
		objToIdModule.setSerializerModifier(new BeanSerializerModifier() {

			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config,
				BeanDescription beanDesc, JsonSerializer<?> serializer)
			{
				if (Mixins.support(beanDesc.getBeanClass())) return serializer;
				// If the serialized class is unknown (i.e. serialized using the general
				// BeanSerializer) or should not be serialized (i.e. complicated class
				// implemented interfaces such as Iterable), would be serialized as an
				// ID.
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

	@Override
	public void addDeserializerTo(final ObjectMapper objectMapper) {
		final SimpleModule module = new SimpleModule();
		module.addDeserializer(Object.class, idToObjDeserializer);
		objectMapper.registerModule(module);
	}

	@Override
	public String parseObject(final Object obj) throws JsonProcessingException {
		return objToIdMapper.writeValueAsString(obj);
	}

	public boolean notSerialized(final Class<?> target) {
		return Arrays.stream(NOT_SERIALIZED).anyMatch(clazz -> clazz
			.isAssignableFrom(target));
	}

}
