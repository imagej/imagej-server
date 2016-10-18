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

import java.util.concurrent.ConcurrentHashMap;

import net.imagej.server.managers.TmpDirManager;

/**
 * Service that handles concurrent Object registration and retrieval using UUID
 * Strings.
 * 
 * @author Leon Yang
 */
public class ObjectService {

	final private ConcurrentHashMap<String, Object> id2obj;
	final private ConcurrentHashMap<Object, String> obj2id;

	public ObjectService() {
		id2obj = new ConcurrentHashMap<>();
		obj2id = new ConcurrentHashMap<>();
	}

	/**
	 * Registers an Object if it does not exist yet.
	 * 
	 * @param obj the Object to be registered.
	 * @return the UUID of the registered Object.
	 */
	public String register(final Object obj) {
		// NB: not actually UUID, but assume 16-bit random String could avoid
		// collision. See implementation of randomString method for details.
		final String uuid = TmpDirManager.randomString(16);
		final String prev = obj2id.putIfAbsent(obj, uuid);

		if (prev != null) return prev;

		id2obj.put(uuid, obj);
		return uuid;
	}

	/**
	 * Retrieves the Object with the given UUID.
	 * 
	 * @param uuid
	 * @return Object with the given UUID
	 */
	public Object find(final String uuid) {
		return id2obj.get(uuid);
	}

	/**
	 * Checks if exists an Object with the given UUID.
	 * 
	 * @param uuid
	 * @return true if such Object exists.
	 */
	public boolean contains(final String uuid) {
		return id2obj.containsKey(uuid);
	}
}
