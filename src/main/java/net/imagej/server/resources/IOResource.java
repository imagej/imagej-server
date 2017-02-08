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
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.scif.config.SCIFIOConfig;
import io.scif.io.ByteArrayHandle;
import io.scif.services.DatasetIOService;
import io.scif.services.LocationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.StreamingOutput;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.server.Utils;
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

	@Parameter
	private LocationService locationService;

	@Inject
	private ObjectService objectService;

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
	 * Lists all object IDs on the imagej-server.
	 * 
	 * @return a list of object IDs
	 */
	@GET
	@Path("objects")
	public Set<String> getIds() {
		return objectService.getIds();
	}

	/**
	 * Shows the information of an object.
	 * 
	 * @param id object ID
	 * @return a JSON node containing the object information
	 */
	@GET
	@Path("objects/{id}")
	public JsonNode getObjectInfo(@PathParam("id") final String id) {
		if (!objectService.contains(id)) {
			throw new WebApplicationException("ID does not exist", Status.NOT_FOUND);
		}

		final Object obj = objectService.find(id);
		final String classStr = obj == null ? "null" : obj.getClass().getName();

		final long createAt = Long.valueOf(id.substring("object:".length(),
			"object:".length() + 8), 36);
		final String createAtStr = new Date(createAt).toString();

		final ObjectNode response = factory.objectNode();
		response.set("class", factory.textNode(classStr));
		response.set("created_at", factory.textNode(createAtStr));
		return response;
	}

	/**
	 * Removes one object from ObjectService.
	 * 
	 * @param id object ID to remove
	 * @return response
	 */
	@DELETE
	@Path("objects/{id}")
	public Response removeObject(@PathParam("id") final String id) {
		if (!objectService.contains(id)) {
			throw new WebApplicationException("ID does not exist", Status.NOT_FOUND);
		}
		if (!objectService.remove(id)) {
			throw new WebApplicationException("Fail to remove ID");
		}
		return Response.ok().build();
	}

	/**
	 * Reads the user-uploaded file into the imagej runtime. Currently only
	 * support images. An ID representing the data is returned.
	 *
	 * @param fileInputStream file stream of the uploaded file
	 * @return JSON string with format {"id":"object:{ID}"}
	 */
	@POST
	@Path("file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Timed
	public JsonNode uploadFile(
		@FormDataParam("file") final InputStream fileInputStream)
	{
		// Maps a filename to a byte array in memory
		final String filename = Utils.randomString(8);
		final ByteArrayHandle bah = new ByteArrayHandle();
		try {
			locationService.mapFile(filename, bah);

			// Reads input file into memory
			final int BUFFER_SIZE = 8192;
			final byte[] buffer = new byte[BUFFER_SIZE];
			int n = 0;
			try {
				while ((n = fileInputStream.read(buffer)) != -1) {
					bah.write(buffer, 0, n);
				}
			}
			catch (IOException exc) {
				throw new WebApplicationException(exc, Status.BAD_REQUEST);
			}

			// Loads file as dataset
			Dataset ds;
			try {
				ds = datasetIOService.open(filename);
			}
			catch (final IOException exc) {
				throw new WebApplicationException(exc, Status.CONFLICT);
			}

			final String id = objectService.register(ds);

			return factory.objectNode().set("id", factory.textNode(id));
		}
		finally {
			// Removes mapping for GC to free memory
			locationService.getIdMap().remove(filename, bah);
		}
	}

	/**
	 * Retrieves an object in a specific format.
	 *
	 * @param objectId object ID
	 * @param format format of the object to be saved into
	 * @param config optional config for saving the object (not tested)
	 * @return Response with the object as content
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Path("file/{id}")
	@Timed
	public Response retrieveFile(@PathParam("id") final String objectId,
		@QueryParam("format") @NotEmpty final String format,
		final SCIFIOConfig config)
	{
		final Object obj = objectService.find(objectId);
		if (obj == null) {
			throw new WebApplicationException("File does not exist",
				Status.NOT_FOUND);
		}
		if (!(obj instanceof Img)) {
			throw new WebApplicationException("Object is not an image",
				Status.BAD_REQUEST);
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

		// Maps a filename to a byte array in memory
		final String filename = String.format("%s.%s", Utils.timestampedId(8),
			format);
		final ByteArrayHandle bah = new ByteArrayHandle();
		try {
			locationService.mapFile(filename, bah);

			try {
				datasetIOService.save(ds, filename, config);
			}
			catch (final IOException exc) {
				locationService.getIdMap().remove(filename, bah);
				throw new WebApplicationException(exc, Status.CONFLICT);
			}

			final String mt = URLConnection.guessContentTypeFromName(filename);
			final StreamingOutput so = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException,
					WebApplicationException
				{
					output.write(bah.getBytes(), 0, (int) bah.length());
				}
			};
			return Response.ok(so, mt).header("Content-Length", bah.length()).build();
		}
		finally {
			// Removes mapping for GC to free memory
			locationService.getIdMap().remove(filename, bah);
		}
	}
}
