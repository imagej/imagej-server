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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.dropwizard.setup.Environment;

/**
 * Resource for administration.
 * 
 * @author Leon Yang
 */
@Path("/admin")
public class AdminResource {
	
	@Inject
	private Environment env;	

	/**
	 * Stop the imagej-server.
	 * 
	 * @return always OK
	 */
	@Path("stop")
	@DELETE
	public Response stop() {
		// stop the server in a separate thread in case the server hangs waiting for
		// the current thread.
		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					env.getApplicationContext().getServer().stop();
				}
				catch (Exception exc) {}
			}
		});
		try {
			return Response.ok().build();
		}
		finally {
			thread.start();
		}
	}

}
