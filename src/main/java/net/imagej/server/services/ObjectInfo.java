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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for an Object with some metadata.
 * 
 * @author Leon Yang
 */
@JsonIgnoreProperties
public interface ObjectInfo {

	@JsonProperty
	String getId();

	@JsonIgnore
	Object getObject();

	@JsonProperty("class")
	default String getObjectClass() {
		return getObject().getClass().getName();
	}

	@JsonProperty("created_at")
	String getCreatedAt();

	@JsonProperty("created_by")
	String getCreatedBy();

	/** Gets the timestamp when the object is last used in module execution */
	@JsonProperty("last_used")
	String getLastUsed();

	void updateLastUsed();

}
