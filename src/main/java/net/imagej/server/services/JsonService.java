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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Leon Yang
 */
public interface JsonService {

	/**
	 * Adds a deserializer to a given {@link ObjectMapper} to modify the default
	 * behavior.
	 * 
	 * @param objectMapper
	 */
	void addDeserializerTo(final ObjectMapper objectMapper);

	/**
	 * Parses the given Object.
	 * 
	 * @param obj Object to be parsed.
	 * @return a JSON format String of the parsed Object
	 * @throws JsonProcessingException
	 */
	String parseObject(final Object obj) throws JsonProcessingException;
}
