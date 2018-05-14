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

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import net.imagej.server.health.ImageJServerHealthCheck;
import net.imagej.server.resources.AdminResource;
import net.imagej.server.resources.ModulesResource;
import net.imagej.server.resources.ObjectsResource;
import net.imagej.server.services.DefaultJsonService;
import net.imagej.server.services.DefaultObjectService;
import net.imagej.server.services.JsonService;
import net.imagej.server.services.ObjectService;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.scijava.Context;

/**
 * Entry point to imagej-server.
 *
 * @author Leon Yang
 */
public class ImageJServer extends
	Application<ImageJServerConfiguration>
{

	private final Context ctx;

	private final ObjectService objectService;

	private final JsonService jsonService;
	
	private Environment env;

	public ImageJServer(final Context ctx) {
		this.ctx = ctx;
		objectService = new DefaultObjectService();
		jsonService = new DefaultJsonService(objectService);
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
		// Enable CORS headers
		final FilterRegistration.Dynamic cors = environment.servlets().addFilter(
			"CORS", CrossOriginFilter.class);

		// Configure CORS parameters
		cors.setInitParameter("allowedOrigins", "*");
		cors.setInitParameter("allowedHeaders",
			"X-Requested-With,Content-Type,Accept,Origin");
		cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

		// Add URL mapping
		cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true,
			"/*");

		env = environment;

		// NB: not implemented yet
		final ImageJServerHealthCheck healthCheck = new ImageJServerHealthCheck();
		environment.healthChecks().register("imagej-server", healthCheck);

		environment.jersey().register(MultiPartFeature.class);

		// -- resources --

		environment.jersey().register(AdminResource.class);

		environment.jersey().register(ModulesResource.class);

		environment.jersey().register(ObjectsResource.class);

		// -- context dependencies injection --

		environment.jersey().register(new AbstractBinder() {

			@Override
			protected void configure() {
				bind(ctx).to(Context.class);
				bind(env).to(Environment.class);
				bind(objectService).to(ObjectService.class);
				bind(jsonService).to(JsonService.class);
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
