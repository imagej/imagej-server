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

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.imagej.server.health.ImageJServerHealthCheck;
import net.imagej.server.managers.TmpDirManager;
import net.imagej.server.resources.AdminResource;
import net.imagej.server.resources.IOResource;
import net.imagej.server.resources.ModulesResource;
import net.imagej.server.services.JsonService;
import net.imagej.server.services.ObjectService;

import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.scijava.Context;

/**
 * Entry point to imagej-server.
 *
 * @author Leon Yang
 */
public class ImageJServerApplication extends
	Application<ImageJServerConfiguration>
{

	private final Context ctx;

	private final ObjectService objectService;

	private final JsonService jsonService;

	private Environment env;

	public ImageJServerApplication(final Context ctx) {
		this.ctx = ctx;
		objectService = new ObjectService();
		jsonService = new JsonService(objectService);
	}

	@Override
	public String getName() {
		return "ImageJ";
	}

	@Override
	public void initialize(final Bootstrap<ImageJServerConfiguration> bootstrap) {
		jsonService.addDeserializerTo(bootstrap.getObjectMapper());
	}

	@Override
	public void run(final ImageJServerConfiguration configuration,
		final Environment environment)
	{
		env = environment;

		// NB: not implemented yet
		final ImageJServerHealthCheck healthCheck = new ImageJServerHealthCheck();
		environment.healthChecks().register("imagej-server", healthCheck);

		environment.jersey().register(MultiPartFeature.class);

		// -- lifecycle managers --

		final TmpDirManager tmpFileManager = new TmpDirManager(configuration
			.getTmpDir());
		environment.lifecycle().manage(tmpFileManager);

		// -- resources --
		
		environment.jersey().register(AdminResource.class);

		environment.jersey().register(ModulesResource.class);

		environment.jersey().register(IOResource.class);

		// -- context dependencies injection --

		environment.jersey().register(new AbstractBinder() {

			@Override
			protected void configure() {
				bind(ctx).to(Context.class);
				bind(env).to(Environment.class);
				bind(objectService).to(ObjectService.class);
				bind(jsonService).to(JsonService.class);
				bind(tmpFileManager).to(TmpDirManager.class);
				bind(Collections.newSetFromMap(
					new ConcurrentHashMap<String, Boolean>())).to(
						new TypeLiteral<Set<String>>()
				{}).named("SERVING");
			}

		});
	}

	public void stop() throws Exception {
		if (env == null) return;
		env.getApplicationContext().getServer().stop();
	}

	public void join() throws InterruptedException {
		if (env == null) return;
		env.getApplicationContext().getServer().join();
	}
}
