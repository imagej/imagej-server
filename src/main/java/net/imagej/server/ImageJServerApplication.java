/*
 * #%L
 * ImageJ server for RESTful access to ImageJ.
 * %%
 * Copyright (C) 2013 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImageJService;
import net.imagej.ops.OpService;
import net.imagej.server.health.ImageJServerHealthCheck;
import net.imagej.server.managers.TmpDirManager;
import net.imagej.server.mixins.Mixins;
import net.imagej.server.resources.IOResource;
import net.imagej.server.resources.ModulesResource;

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
		new ImageJServerApplication().run(args);
	}

	private ImageJ ij;

	/**
	 * A list storing datasets uploaded by the clients or created during runtime.
	 * This list should be thread-safe as it could be accessed and modified by
	 * multiple API requests concurrently.
	 */
	private List<Dataset> datasetRepo;

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
		datasetRepo = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run(final ImageJServerConfiguration configuration,
		final Environment environment)
	{
		// NB: not implemented yet
		final ImageJServerHealthCheck healthCheck = new ImageJServerHealthCheck();
		environment.healthChecks().register("imagej-server", healthCheck);

		// register Jackson MixIns to obtain better json output format for some
		// specific types
		Mixins.registerMixIns(environment.getObjectMapper());
		environment.jersey().register(MultiPartFeature.class);

		// -- lifecycle managers --

		final TmpDirManager tmpFileManager = new TmpDirManager(configuration
			.getTmpDir());
		environment.lifecycle().manage(tmpFileManager);

		// -- resources --

		final ModulesResource modulesResource = new ModulesResource(ij,
			datasetRepo);
		environment.jersey().register(modulesResource);

		final IOResource ioResource = new IOResource(ij, datasetRepo,
			tmpFileManager);
		environment.jersey().register(ioResource);
	}
}
