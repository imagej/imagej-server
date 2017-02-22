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

import java.util.Set;

/**
 * @author Leon Yang
 */
public interface ObjectService {

	/**
	 * Gets a set of all IDs.
	 * 
	 * @return a set of all IDs
	 */
	Set<String> getIds();

	/**
	 * Registers an Object if it does not exist yet.
	 * 
	 * @param object the Object to be registered.
	 * @param createdBy information about where the object is created
	 * @return the ID of the registered Object.
	 */
	String register(final Object object, final String createdBy);

	/**
	 * Removes an Object with the given ID.
	 * 
	 * @param id
	 * @return true if the process was successful
	 */
	boolean remove(final String id);

	/**
	 * Retrieves the Object with the given ID.
	 * 
	 * @param id
	 * @return Object with the given ID
	 */
	ObjectInfo find(final String id);

	/**
	 * Checks if exists an Object with the given ID.
	 * 
	 * @param id
	 * @return true if such Object exists.
	 */
	boolean contains(final String id);
}
