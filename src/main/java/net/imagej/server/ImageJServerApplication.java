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
import io.scif.SCIFIOService;

import net.imagej.ImageJ;
import net.imagej.ImageJService;
import net.imagej.ops.OpService;
import net.imagej.server.health.ImageJServerHealthCheck;
import net.imagej.server.managers.TmpDirManager;
import net.imagej.server.resources.IOResource;
import net.imagej.server.resources.ModulesResource;
import net.imagej.server.services.JsonService;
import net.imagej.server.services.ObjectService;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.scijava.Context;
import org.scijava.service.SciJavaService;

/**
 * Entry point to imagej-server.
 *
 * @author Leon Yang
 */
public class ImageJServerApplication extends
	Application<ImageJServerConfiguration>
{

	public static void main(final String[] args) throws Exception {
		final String[] arguments = args == null || args.length == 0 ? //
			new String[] { "server", "imagej-server.yml" } : args;
		new ImageJServerApplication().run(arguments);
	}

	private ImageJ ij;

	private final ObjectService objectService;

	private final JsonService jsonService;

	public ImageJServerApplication() {
		objectService = new ObjectService();
		jsonService = new JsonService(objectService);
	}

	@Override
	public String getName() {
		return "ImageJ";
	}

	@Override
	public void initialize(final Bootstrap<ImageJServerConfiguration> bootstrap) {
		ij = new ImageJ(new Context(SciJavaService.class, SCIFIOService.class,
			ImageJService.class, OpService.class));
		// HACK: better way to set imagej headless?
		ij.ui().setHeadless(true);
		jsonService.addDeserializerTo(bootstrap.getObjectMapper());
	}

	@Override
	public void run(final ImageJServerConfiguration configuration,
		final Environment environment)
	{
		// NB: not implemented yet
		final ImageJServerHealthCheck healthCheck = new ImageJServerHealthCheck();
		environment.healthChecks().register("imagej-server", healthCheck);

		environment.jersey().register(MultiPartFeature.class);

		// -- lifecycle managers --

		final TmpDirManager tmpFileManager = new TmpDirManager(configuration
			.getTmpDir());
		environment.lifecycle().manage(tmpFileManager);

		// -- resources --

		final ModulesResource modulesResource = new ModulesResource(ij,
			jsonService);
		environment.jersey().register(modulesResource);

		final IOResource ioResource = new IOResource(ij, objectService,
			tmpFileManager);
		environment.jersey().register(ioResource);
	}
}
