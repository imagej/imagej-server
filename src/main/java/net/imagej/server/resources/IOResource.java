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

package net.imagej.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.scif.config.SCIFIOConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.server.managers.TmpDirManager;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Server resource for managing I/O operations, including:
 * <li>upload image</li>
 * <li>request image download</li>
 * <li>download image</li> </br>
 *
 * @author Leon Yang
 */
@Path("/io")
@Produces(MediaType.APPLICATION_JSON)
public class IOResource {

	private final ImageJ ij;

	/** Thread-safe list of datasets usable by the imagej runtime */
	private final List<Dataset> datasets;

	/** Thread-safe set of name of files that are currently served. */
	private final Set<String> serving;

	private final TmpDirManager tmpDirManager;

	private static final JsonNodeFactory factory = JsonNodeFactory.instance;

	public IOResource(final ImageJ ij, final List<Dataset> datasets,
		final TmpDirManager tmpDirManager)
	{
		this.ij = ij;
		this.datasets = datasets;
		this.tmpDirManager = tmpDirManager;
		serving = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	/**
	 * Store user-uploaded file to temp directory. Currently always assume the
	 * file to be image.
	 *
	 * @param fileInputStream file stream of the uploaded file
	 * @return json node with format {"id":"_img_{IMG_ID}"}
	 */
	@POST
	@Path("file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Timed
	public JsonNode uploadFile(
		@FormDataParam("file") final InputStream fileInputStream)
	{
		final String filename = tmpDirManager.randomString("io_upload", 10, "");
		final java.nio.file.Path tmpFile = tmpDirManager.getFilePath(filename);

		Dataset ds;
		try {
			// NB: Can we read the filestream into a dataset without saving it?
			Files.copy(fileInputStream, tmpFile);
			ds = ij.scifio().datasetIO().open(tmpFile.toString());
		}
		catch (final IOException exc) {
			tmpFile.toFile().delete();
			throw new WebApplicationException(exc, Status.CONFLICT);
		}

		datasets.add(ds);
		// not using size() for concurrency concern
		final int idx = datasets.lastIndexOf(ds);

		return factory.objectNode().set("id", factory.textNode("_img_" + idx));
	}

	/**
	 * Handles requests for downloading an image. A dataset is first stored into
	 * disk, and then the file name is returned so that the client can download
	 * the image in a separate API call.
	 *
	 * @param id dataset ID
	 * @param ext extension of the dataset to be saved with
	 * @param config optional config for saving the image
	 * @return json node with format {"filename":"{FILENAME}.{ext}"}
	 */
	@POST
	@Path("{id}")
	@Timed
	public JsonNode requestDataset(@PathParam("id") final String id,
		@QueryParam("ext") @NotEmpty final String ext, final SCIFIOConfig config)
	{
		if (!id.startsWith("_img_")) {
			throw new WebApplicationException("ID must start with \"_img_\"",
				Status.BAD_REQUEST);
		}

		final int idx = Integer.parseInt(id.substring("_img_".length()));

		final Dataset ds = datasets.get(idx);
		final String filename = tmpDirManager.randomString("", 10, '.' + ext);
		try {
			ij.scifio().datasetIO().save(ds, tmpDirManager.getFilePath(filename)
				.toString(), config);
		}
		catch (final IOException exc) {
			throw new WebApplicationException(exc, Status.CONFLICT);
		}

		serving.add(filename);
		return factory.objectNode().set("filename", factory.textNode(filename));
	}

	/**
	 * Verify the file user requested to download is valid and start the download.
	 *
	 * @param filename name of the file to be downloaded
	 * @return Response with the file as entity
	 */
	@GET
	@Path("{filename}")
	@Produces("image/*")
	@Timed
	public Response retrieveDataset(
		@PathParam("filename") final String filename)
	{
		// Only allow downloading files we are currently serving
		if (!serving.contains(filename)) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		final File file = new File(tmpDirManager.getFilePath(filename).toString());
		final String mt = new MimetypesFileTypeMap().getContentType(file);
		return Response.ok(file, mt).build();
	}
}
