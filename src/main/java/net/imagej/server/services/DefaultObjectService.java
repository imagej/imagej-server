/*
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

package net.imagej.server.services;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that handles concurrent Object registration and retrieval using IDs.
 * 
 * @author Leon Yang
 */
public class DefaultObjectService implements ObjectService {

	final private ConcurrentHashMap<String, ObjectInfo> id2obj;
	final private ConcurrentHashMap<Object, String> obj2id;

	public DefaultObjectService() {
		id2obj = new ConcurrentHashMap<>();
		obj2id = new ConcurrentHashMap<>();
	}

	@Override
	public Set<String> getIds() {
		return Collections.unmodifiableSet(id2obj.keySet());
	}

	@Override
	public String register(final Object object, final String createdBy) {
		final DefaultObjectInfo info = new DefaultObjectInfo(object, createdBy);
		final String prev = obj2id.putIfAbsent(object, info.getId());

		if (prev != null) return prev;

		id2obj.put(info.getId(), info);
		return info.getId();
	}

	@Override
	public boolean remove(final String id) {
		if (!id2obj.containsKey(id)) return false;
		final ObjectInfo info = id2obj.get(id);
		return id2obj.remove(id, info) && obj2id.remove(info.getObject(), id);
	}

	@Override
	public ObjectInfo find(final String id) {
		return id2obj.get(id);
	}

	@Override
	public boolean contains(final String id) {
		return id2obj.containsKey(id);
	}
}
