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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import net.imagej.ImageJ;
import net.imagej.ops.Initializable;
import net.imagej.server.services.JsonService;

import org.scijava.Identifiable;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;

/**
 * Server resource that manages module operations.
 *
 * @author Leon Yang
 */
@Path("/modules")
@Produces(MediaType.APPLICATION_JSON)
public class ModulesResource {

	private final ImageJ ij;

	private final List<MInfo> mInfos = new ArrayList<>();

	private final JsonService jsonService;

	public ModulesResource(final ImageJ ij, final JsonService jsonService) {
		this.ij = ij;
		this.jsonService = jsonService;

		int index = 0;
		for (final ModuleInfo info : ij.module().getModules()) {
			final MInfo mInfo = new MInfo();
			mInfo.info = info;
			mInfo.index = index++;
			if (info instanceof Identifiable) {
				mInfo.identifier = ((Identifiable) info).getIdentifier();
			}
			mInfos.add(mInfo);
		}
	}

	/**
	 * @return a list of {@link MInfo}s
	 */
	@GET
	@Timed
	public List<MInfo> retrieveModules() {
		return mInfos;
	}

	/**
	 * Retrieves the information of a module given its id. The id can be its index
	 * in the mInfos list, or its identifier if any.
	 *
	 * @param id ID of the module
	 * @return ModuleInfo of the module with the given ID, or null if not such
	 *         module exists
	 */
	private ModuleInfo getModule(final String id) {
		final ModuleInfo info = ij.module().getModuleById(id);
		if (info != null) return info;
		try {
			final int index = Integer.parseInt(id) - 1;
			return mInfos.get(index).info;
		}
		catch (final NumberFormatException exc) {
			// NB: No action needed.
		}
		return null;
	}

	/**
	 * Gets more detailed information of a module with the given ID.
	 *
	 * @param id ID of the module
	 * @return More detailed information of the module with the given ID
	 */
	@GET
	@Path("{id}")
	public MInfoLong getWidget(@PathParam("id") final String id) {
		final ModuleInfo info = getModule(id);
		if (info == null) {
			final String msg = String.format("Module %s does not exist", id);
			throw new WebApplicationException(msg, Status.NOT_FOUND);
		}
		return new MInfoLong(info);
	}

	/**
	 * Executes a module with given ID.
	 *
	 * @param id ID of the module to execute
	 * @param runSpec specification of this execution
	 * @return a map of outputs
	 */
	@POST
	@Path("{id}")
	public String runModule(@PathParam("id") final String id,
		final RunSpec runSpec)
	{
		final ModuleInfo info = getModule(id);
		if (info == null) {
			final String msg = String.format("Module %s does not exist", id);
			throw new WebApplicationException(msg, Status.NOT_FOUND);
		}

		final Module m;
		try {
			m = ij.module().run(info, runSpec.process, runSpec.inputMap).get();
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

	public static class RunSpec {

		public boolean process = true;
		public Map<String, Object> inputMap;
	}

	public static class MInfo {

		public transient ModuleInfo info;
		public long index;
		public String identifier;
	}

	public static class MInfoLong {

		public String identifier;
		public String name;
		public String label;
		public List<MItem> inputs = new ArrayList<>();
		public List<MItem> outputs = new ArrayList<>();

		public MInfoLong(final ModuleInfo info) {
			identifier = info instanceof Identifiable ? //
				((Identifiable) info).getIdentifier() : null;
			name = info.getName();
			label = info.getLabel();
			for (final ModuleItem<?> input : info.inputs()) {
				inputs.add(new MItem(input));
			}
			for (final ModuleItem<?> output : info.outputs()) {
				outputs.add(new MItem(output));
			}
		}
	}

	public static class MItem {

		public String name;
		public String label;
		public List<?> choices;

		public MItem(final ModuleItem<?> item) {
			name = item.getName();
			label = item.getLabel();
			choices = item.getChoices();
		}
	}

	// HACK: Initialize op when run as module
	@Plugin(type = PreprocessorPlugin.class, priority = Priority.HIGH_PRIORITY -
		1)
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
