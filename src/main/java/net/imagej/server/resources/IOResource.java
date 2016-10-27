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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.scif.config.SCIFIOConfig;
import io.scif.services.DatasetIOService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.inject.Named;
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
import net.imagej.DatasetService;
import net.imagej.server.managers.TmpDirManager;
import net.imagej.server.services.ObjectService;
import net.imglib2.img.Img;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

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

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private DatasetIOService datasetIOService;

	@Inject
	private ObjectService objectService;

	/** Thread-safe set of name of files that are currently served. */
	@Inject
	@Named("SERVING")
	private Set<String> serving;

	@Inject
	private TmpDirManager tmpDirManager;

	private static final JsonNodeFactory factory = JsonNodeFactory.instance;

	/**
	 * Initialize resource by injection. Should not be called directly.
	 * 
	 * @param ctx
	 */
	@Inject
	public void initialize(final Context ctx) {
		ctx.inject(this);
	}

	/**
	 * Reads the user-uploaded file into the imagej runtime. Currently only
	 * support images. An UUID representing the data is returned.
	 *
	 * @param fileInputStream file stream of the uploaded file
	 * @return JSON string with format {"uuid":"_obj_{UUID}"}
	 */
	@POST
	@Path("file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Timed
	public JsonNode uploadFile(
		@FormDataParam("file") final InputStream fileInputStream)
	{
		final String filename = TmpDirManager.randomString(8);
		final java.nio.file.Path tmpFile = tmpDirManager.getFilePath(filename);

		Dataset ds;
		try {
			Files.copy(fileInputStream, tmpFile);
			ds = datasetIOService.open(tmpFile.toString());
		}
		catch (final IOException exc) {
			throw new WebApplicationException(exc, Status.CONFLICT);
		}
		finally {
			tmpFile.toFile().delete();
		}

		final String uuid = objectService.register(ds);

		return factory.objectNode().set("uuid", factory.textNode("_obj_" + uuid));
	}

	/**
	 * Handles requests for downloading an image. A dataset is first stored on
	 * disk for serving, and then the file name is returned so that the client can
	 * download the image in a separate API call.
	 *
	 * @param id dataset ID
	 * @param ext extension of the dataset to be saved with
	 * @param config optional config for saving the image
	 * @return JSON node with format {"filename":"{FILENAME}.{ext}"}
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Path("{id}")
	@Timed
	public JsonNode requestFile(@PathParam("id") final String id,
		@QueryParam("ext") @NotEmpty final String ext, final SCIFIOConfig config)
	{
		if (!id.startsWith("_obj_")) {
			throw new WebApplicationException("ID must start with \"_obj_\"",
				Status.BAD_REQUEST);
		}

		final String uuid = id.substring(5);

		final Object obj = objectService.find(uuid);
		if (obj == null) {
			throw new WebApplicationException("Image does not exist");
		}
		if (!(obj instanceof Img)) {
			throw new WebApplicationException("Object is not an image");
		}

		final Dataset ds;
		if (obj instanceof Dataset) {
			ds = (Dataset) obj;
		}
		else {
			@SuppressWarnings({ "rawtypes" })
			final Img img = (Img) obj;
			ds = datasetService.create(img);
		}

		final String filename = String.format("%s.%s", TmpDirManager.randomString(
			8), ext);
		final java.nio.file.Path filePath = tmpDirManager.getFilePath(filename);

		try {
			datasetIOService.save(ds, filePath.toString(), config);
		}
		catch (final IOException exc) {
			filePath.toFile().delete();
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
	public Response retrieveFile(@PathParam("filename") final String filename) {
		// Only allow downloading files we are currently serving
		if (!serving.contains(filename)) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		final File file = tmpDirManager.getFilePath(filename).toFile();
		final String mt = new MimetypesFileTypeMap().getContentType(file);
		return Response.ok(file, mt).build();
	}
}