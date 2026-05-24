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
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

class JsonSerializerAdapter<T> extends StdSerializer<T> {

	private SciJavaJsonSerializer<T> serializedDelegate;

	public JsonSerializerAdapter(
		SciJavaJsonSerializer<T> serializedDelegate)
	{
		super(serializedDelegate.handleType());
		this.serializedDelegate = serializedDelegate;
	}


	@Override
	public void serialize(T value, JsonGenerator gen,
		SerializerProvider provider) throws IOException
	{
		serializedDelegate.serialize(value, gen, provider);
	}


}
