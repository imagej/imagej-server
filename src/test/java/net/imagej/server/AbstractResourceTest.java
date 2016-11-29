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

package net.imagej.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.dropwizard.testing.junit.ResourceTestRule.Builder;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.imagej.server.managers.TmpDirManager;
import net.imagej.server.services.DefaultJsonService;
import net.imagej.server.services.DefaultObjectService;
import net.imagej.server.services.JsonService;
import net.imagej.server.services.ObjectService;

import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.scijava.Context;

/**
 * Abstract class for resource tests. Initialize necessary variables.
 * 
 * @author Leon Yang
 */
public abstract class AbstractResourceTest {

	protected static final Context ctx = new Context();

	protected static final ObjectService objectService =
		new DefaultObjectService();

	protected static final JsonService jsonService = new DefaultJsonService(
		objectService);

	protected static final TmpDirManager tmpDirManager = new TmpDirManager(
		"/tmp");

	protected static final Set<String> serving = Collections.newSetFromMap(
		new ConcurrentHashMap<>());

	protected static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		jsonService.addDeserializerTo(objectMapper);
	}

	protected static final AbstractBinder binder = new AbstractBinder() {

		@Override
		protected void configure() {
			bind(ctx).to(Context.class);
			bind(objectService).to(ObjectService.class);
			bind(jsonService).to(JsonService.class);
			bind(tmpDirManager).to(TmpDirManager.class);
			bind(serving).to(new TypeLiteral<Set<String>>() {}).named("SERVING");
		}
	};

	protected static final Builder resourcesBuilder = ResourceTestRule.builder()
		.addProvider(binder).setMapper(objectMapper);

}
