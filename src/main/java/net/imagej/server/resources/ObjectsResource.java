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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.scif.config.SCIFIOConfig;
import io.scif.io.ByteArrayHandle;
import io.scif.services.DatasetIOService;
import io.scif.services.LocationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplayService;
import net.imagej.server.Utils;
import net.imagej.server.services.ObjectInfo;
import net.imagej.server.services.ObjectService;
import net.imagej.table.Table;
import net.imglib2.img.Img;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;

/**
 * Server resource for managing data structures that could not be easily handled
 * with JSON,
 *
 * @author Leon Yang
 */
@Path("/objects")
@Produces(MediaType.APPLICATION_JSON)
public class ObjectsResource {

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private DatasetIOService datasetIOService;
	
	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LocationService locationService;

	@Parameter
	private IOService ioService;

	@Inject
	private ObjectService objectService;

	private static final JsonNodeFactory factory = JsonNodeFactory.instance;

	/**
	 * Initialize resource by injection. Should not be called directly.
	 * 
	 * @param ctx
	 */
	@Inject
	public void initialize(final org.scijava.Context ctx) {
		ctx.inject(this);
	}

	/**
	 * Lists all object IDs on the imagej-server.
	 * 
	 * @return a list of object IDs
	 */
	@GET
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
	@Path("{id}")
	public ObjectInfo getObjectInfo(@PathParam("id") final String id) {
		if (!objectService.contains(id)) {
			throw new WebApplicationException("ID does not exist", Status.NOT_FOUND);
		}
		return objectService.find(id);
	}

	/**
	 * Removes one object from ObjectService.
	 * 
	 * @param id object ID to remove
	 * @return response
	 */
	@DELETE
	@Path("{id}")
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
	 * support images and tables in text. An ID representing the data is returned.
	 * <p> 
	 * If no hint for format is provided, filename would be used to guess the file
	 * format.
	 * </p>
	 *
	 * @param fileInputStream file stream of the uploaded file
	 * @param fileDetail "Content-Disposition" header
	 * @param typeHint optional hint for file type
	 * @return JSON string with format {"id":"object:{ID}"}
	 */
	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Timed
	public JsonNode uploadFile(
		@FormDataParam("file") final InputStream fileInputStream,
		@FormDataParam("file") final FormDataContentDisposition fileDetail,
		@QueryParam("type") final String typeHint)
	{
		final ByteArrayHandle bah;
		try {
			bah = readFileInputStream(fileInputStream);
		}
		catch (IOException exc) {
			throw new WebApplicationException(exc, Status.BAD_REQUEST);
		}

		// Maps a filename to a byte array in memory
		final String filename = Utils.randomString(8) + "_" + fileDetail
			.getFileName();
		locationService.mapFile(filename, bah);

		final String type;
		if (typeHint != null && typeHint.length() != 0) {
			type = typeHint.toLowerCase();
		}
		else {
			final String mt = Utils.getMimetype(filename);
			type = mt.substring(0, mt.indexOf('/'));
		}

		final ArrayNode arrayNode = factory.arrayNode();
		try {
			final String createdByString = "uploadFile:filename=" + fileDetail.getFileName();
			switch (type) {
				case "image":					
					// Create a Dataset based on the selected file
					final Dataset dataset = datasetIOService.open(filename);
					final String datasetId = objectService.register(dataset, createdByString);
					arrayNode.add(factory.objectNode().set("id", factory.textNode(datasetId)));					
					// Create a DatasetView based on the just created Dataset
					final DatasetView datasetView = (DatasetView) imageDisplayService.createDataView(dataset);
					datasetView.rebuild(); // Force re-initialization
					final String datasetViewId = objectService.register(datasetView, createdByString);
					arrayNode.add(factory.objectNode().set("id", factory.textNode(datasetViewId)));					
					break;
				case "text":
					// Create an Object based on the selected file
					Object obj = ioService.open(filename);
					final String id = objectService.register(obj, createdByString);
					arrayNode.add(factory.objectNode().set("id", factory.textNode(id)));
					break;
				default:
					throw new WebApplicationException("Unrecognized format",
						Status.BAD_REQUEST);
			}
		}
		catch (final WebApplicationException exc) {
			throw exc;
		}
		catch (final IOException exc) {
			throw new WebApplicationException(exc, Status.CONFLICT);
		}
		finally {
			locationService.getIdMap().remove(filename, bah);
		}
		
		return arrayNode;
	}

	/**
	 * Retrieves an object in a specific format.
	 *
	 * @param id object ID
	 * @param format format of the object to be saved into
	 * @param uriInfo used for obtaining query parameters for config
	 * @return Response with the object as content
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("{id}/{format}")
	@Timed
	public Response getObject(@PathParam("id") final String id,
		@PathParam("format") final String format, @Context final UriInfo uriInfo)
	{
		if (!objectService.contains(id)) {
			throw new WebApplicationException("Object does not exist",
				Status.NOT_FOUND);
		}

		final String filename = String.format("%s.%s", Utils.timestampedId(8),
			format);
		final ByteArrayHandle bah = new ByteArrayHandle();
		locationService.mapFile(filename, bah);

		try {
			final Object obj = objectService.find(id).getObject();
			if (obj instanceof DatasetView) {
				// TODO: Should we make use of other DatasetView parameters?
				final Dataset ds = ((DatasetView) obj).getData();
				// TODO: inject query parameters into config
				final SCIFIOConfig config = new SCIFIOConfig();
				datasetIOService.save(ds, filename, config);
			} else if (obj instanceof Img) {
				final Dataset ds;
				if (obj instanceof Dataset) {
					ds = (Dataset) obj;
				}
				else {
					@SuppressWarnings({ "rawtypes" })
					final Img img = (Img) obj;
					ds = datasetService.create(img);
				}
				// TODO: inject query parameters into config
				final SCIFIOConfig config = new SCIFIOConfig();
				datasetIOService.save(ds, filename, config);
			}
			else if (obj instanceof Table) {
				// TODO: inject query parameters into IOPlugin (DefaultTableIOPlugin)
				ioService.save(obj, filename);
			}
			else {
				final String type = obj == null ? "null" : obj.getClass().getName();
				throw new WebApplicationException(
					"Retrival for Object type not supported yet: " + type,
					Status.BAD_REQUEST);
			}

			final StreamingOutput so = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException,
					WebApplicationException
				{
					output.write(bah.getBytes(), 0, (int) bah.length());
				}
			};
			final String mt = Utils.getMimetype(filename);
			return Response.ok(so, mt).header("Content-Length", bah.length()).build();
		}
		catch (final WebApplicationException exc) {
			throw exc;
		}
		catch (final IOException exc) {
			throw new WebApplicationException(exc, Status.CONFLICT);
		}
		finally {
			locationService.getIdMap().remove(filename, bah);
		}
	}

	// -- helper methods --

	private ByteArrayHandle readFileInputStream(final InputStream fileInputStream)
		throws IOException
	{
		final ByteArrayHandle bah = new ByteArrayHandle();

		// Reads input file into memory
		final int BUFFER_SIZE = 8192;
		final byte[] buffer = new byte[BUFFER_SIZE];
		int n = 0;
		while ((n = fileInputStream.read(buffer)) != -1) {
			bah.write(buffer, 0, n);
		}
		return bah;
	}
}
