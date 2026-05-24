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

package net.imagej.server.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

import org.scijava.plugin.SciJavaPlugin;

public interface SciJavaJsonSerializer<T> extends SciJavaPlugin

{

	void serialize(T value, JsonGenerator gen,
		SerializerProvider serializers)
		throws IOException;

	Class<T> handleType();

	default boolean isSupportedBy(Class<?> desiredClass) {
		return handleType().isAssignableFrom(desiredClass);
	}

	default public void register(ObjectMapper mapper) {
		SimpleModule mod = new SimpleModule();
		mod.addSerializer(new JsonSerializerAdapter<>(this));
		mapper.registerModule(mod);
	}
}
