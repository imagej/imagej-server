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

package net.imagej.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import net.imagej.ops.Initializable;
import net.imagej.server.WebCommandInfo;
import net.imagej.server.services.JsonService;

import org.scijava.Context;
import org.scijava.Identifiable;
import org.scijava.Priority;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.ModulePreprocessor;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.widget.InputHarvester;

/**
 * Server resource that manages module operations.
 *
 * @author Leon Yang
 */
@Path("/modules")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ModulesResource {

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private PluginService pluginService;

	@Inject
	private JsonService jsonService;

	private LinkedHashMap<String, ModuleInfo> moduleCache;

	/**
	 * Initialize resource by injection. Should not be called directly.
	 * 
	 * @param ctx
	 */
	@Inject
	public void initialize(final Context ctx) {
		ctx.inject(this);
		updateModuleCache();
	}

	private void updateModuleCache() {
		LinkedHashMap<String, ModuleInfo> tmp = new LinkedHashMap<>();
		for (final ModuleInfo module : moduleService.getModules()) {
			tmp.put(((Identifiable) module).getIdentifier(), module);
		}
		// atomic update
		moduleCache = tmp;
	}

	/**
	 * @return a list of module identifiers
	 */
	@GET
	@Timed
	public Set<String> retrieveModules() {
		updateModuleCache();
		return moduleCache.keySet();
	}

	/**
	 * Gets more detailed information of a module with the given ID.
	 *
	 * @param id ID of the module
	 * @return More detailed information of the module with the given ID
	 * @throws JsonProcessingException
	 */
	@GET
	@Path("{id}")
	public String getWidget(@PathParam("id") final String id)
		throws JsonProcessingException
	{
		final ModuleInfo info = moduleCache.getOrDefault(id, null);
		if (info == null) {
			final String msg = String.format("Module %s does not exist", id);
			throw new WebApplicationException(msg, Status.NOT_FOUND);
		}

		// Check if we're dealing with Command information
		if (!(info instanceof CommandInfo)) {
			// TODO - decide what to do if this happens.
			throw new IllegalArgumentException("Object is not an instance of " + CommandInfo.class.getName());
		}

		// Create a transient instance of the module, so we can do some
		// selective preprocessing. This is necessary to determine which
		// inputs are still unresolved at the time of user input harvesting,
		// as well as what their current starting values are.
		final Module module = moduleService.createModule(info);

		// Get the complete list of preprocessors.
		final List<PluginInfo<PreprocessorPlugin>> allPPs =
			pluginService.getPluginsOfType(PreprocessorPlugin.class);

		// Filter the list to only those which run _before_ input harvesting.
		final List<PluginInfo<PreprocessorPlugin>> goodPPs = allPPs.stream() //
			.filter(ppInfo -> ppInfo.getPriority() > InputHarvester.PRIORITY) //
			.collect(Collectors.toList());

		// Execute all of these "good" preprocessors to prep the module correctly.
		for (final ModulePreprocessor p : pluginService.createInstances(goodPPs)) {
			p.process(module);
			if (p.isCanceled()) {
				// TODO - decide what to do if this happens.
			}
		}

		// Create a WebCommandInfo instance and parse it (resolved inputs will be identified during the process)
		return jsonService.parseObject(new WebCommandInfo((CommandInfo)info, module));
	}

	/**
	 * Executes a module with given ID.
	 *
	 * @param id ID of the module to execute
	 * @param inputs inputs to the execution
	 * @param process true if the execution should be pre/post processed
	 * @return a map of outputs
	 */
	@POST
	@Path("{id}")
	public String runModule(@PathParam("id") final String id,
		final Map<String, Object> inputs,
		@DefaultValue("true") @QueryParam("process") final boolean process)
	{
		final ModuleInfo info = moduleCache.getOrDefault(id, null);
		if (info == null) {
			final String msg = String.format("Module %s does not exist", id);
			throw new WebApplicationException(msg, Status.NOT_FOUND);
		}

		final Module m;
		try {
			m = moduleService.run(info, process, inputs).get();
		}
		catch (final InterruptedException exc) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
		catch (final IllegalArgumentException | ExecutionException exc) {
			throw new WebApplicationException(exc, Status.BAD_REQUEST);
		}
		final Map<String, Object> outputs = m.getOutputs();

		try {
			return jsonService.parseObject(outputs);
		}
		catch (final JsonProcessingException exc) {
			throw new WebApplicationException("Fail to parse outputs", exc,
				Status.INTERNAL_SERVER_ERROR);
		}
	}

	// -- Helper classes --

	// HACK: Initialize op when run as module
	@Plugin(type = PreprocessorPlugin.class, priority = Priority.HIGH - 1)
	public static class InitializablePreprocessor extends
		AbstractPreprocessorPlugin
	{

		@Override
		public void process(final Module module) {
			if (module.getDelegateObject() instanceof Initializable) {
				((Initializable) module.getDelegateObject()).initialize();
			}
		}
	}
}
