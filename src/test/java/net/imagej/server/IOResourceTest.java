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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.scif.services.DatasetIOService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import net.imagej.Dataset;
import net.imagej.server.resources.IOResource;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test for {@link IOResource}.
 * 
 * @author Leon Yang
 */
public class IOResourceTest extends AbstractResourceTest {

	@ClassRule
	public static final ResourceTestRule resources = resourcesBuilder.addProvider(
		MultiPartFeature.class).addProvider(IOResource.class).build();

	/**
	 * A integrated test for the workflow using IOResource:<br/>
	 * <li>upload file</li>
	 * <li>request file</li>
	 * <li>retrieve file</li>
	 */
	@Test
	public void ioResource() {
		try {
			// Test uploadFile
			final String imgID = uploadFile("imgs/about4.tif").substring("object:"
				.length());
			assertTrue(objectService.contains(imgID));

			// Test requestFile
			final String filename = requestFile("object:" + imgID, "tiff");
			assertTrue(serving.contains(filename));

			// Test retrieveFile
			final File downloaded = retrieveFile(filename);
			final Dataset ds = ctx.service(DatasetIOService.class).open(downloaded
				.getAbsolutePath());
			final Iterator<?> expectedItr = ((Iterable<?>) objectService.find(imgID))
				.iterator();
			final Iterator<?> actualItr = ds.iterator();
			while (expectedItr.hasNext()) {
				assertTrue(actualItr.hasNext());
				assertEquals(expectedItr.next(), actualItr.next());
			}
			assertTrue(!actualItr.hasNext());
		}
		catch (IOException exc) {
			fail(exc.getMessage());
		}
	}

	/**
	 * Upload file to IOResource
	 * 
	 * @param file
	 * @return the object ID of that file
	 * @throws IOException
	 */
	public String uploadFile(final String file) throws IOException {
		final URL url = this.getClass().getClassLoader().getResource(file);
		try (final FormDataMultiPart multiPart = new FormDataMultiPart()) {
			multiPart.field("file", url.openStream(),
				MediaType.MULTIPART_FORM_DATA_TYPE);
			final String response = resources.client().register(
				MultiPartFeature.class).target("/io/file").request().post(Entity.entity(
					multiPart, multiPart.getMediaType()), String.class);
			final Matcher matcher = Pattern.compile("\\{\"id\":\"([^\"]+)\"\\}")
				.matcher(response);
			assertTrue(matcher.find());
			return matcher.group(1);
		}
	}

	/**
	 * Request download of a file in a specific format
	 * 
	 * @param objectId ID of the file
	 * @param format format of the file to be saved
	 * @return filename token for downloading the requested file
	 */
	public String requestFile(final String objectId, final String format) {
		final String response = resources.client().target("/io/" + objectId).queryParam(
			"format", format).request().post(null, String.class);
		final Matcher matcher = Pattern.compile("\\{\"filename\":\"([^\"]+)\"\\}")
			.matcher(response);
		assertTrue(matcher.find());
		return matcher.group(1);
	}

	/**
	 * Retrieve a file
	 * 
	 * @param filename
	 * @return the downloaded file
	 */
	public File retrieveFile(final String filename) {
		return resources.client().target("/io/" + filename).request().get(
			File.class);
	}
}
