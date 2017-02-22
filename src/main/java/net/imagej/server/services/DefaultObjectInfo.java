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

import java.util.Date;

import net.imagej.server.Utils;

/**
 * Default implementation of {@link ObjectInfo}.
 * 
 * @author Leon Yang
 */
public class DefaultObjectInfo implements ObjectInfo {

	private final String id;
	private final Object object;
	private final String createdAt;
	private final String createdBy;
	private String lastUsed;

	public DefaultObjectInfo(final Object object, final String createdBy) {
		final Date now = new Date();
		createdAt = now.toString();
		this.id = "object:" + Long.toUnsignedString(now.getTime(), 36) + Utils
			.randomString(8);
		this.object = object;
		this.createdBy = createdBy;
		this.lastUsed = null;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Object getObject() {
		return this.object;
	}

	@Override
	public String getCreatedAt() {
		return this.createdAt;
	}

	@Override
	public String getCreatedBy() {
		return this.createdBy;
	}

	@Override
	public String getLastUsed() {
		return this.lastUsed;
	}

	@Override
	public void updateLastUsed() {
		this.lastUsed = new Date().toString();
	}

}
