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

package net.imagej.server.external;

import net.imagej.server.ImageJServer;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default implementation of {@link ServerService}.
 * 
 * @author Leon Yang
 */
@Plugin(type = Service.class)
public class DefaultServerService extends AbstractService implements
	ServerService
{

	private ImageJServer serverApp;

	@Override
	public void launch(final boolean blocking) {
		if (serverApp != null) return;
		serverApp = new ImageJServer(getContext());
		try {
			serverApp.run("server", "imagej-server.yml");
			if (blocking) serverApp.join();
		}
		catch (Exception exc) {
			log().error("Fail to launch server", exc);
			serverApp = null;
		}
	}

	@Override
	public void stop() {
		if (serverApp == null) return;
		try {
			serverApp.stop();
		}
		catch (Exception exc) {
			log().error("Fail to stop server", exc);
			return;
		}
		serverApp = null;
	}

	@Override
	public void dispose() {
		stop();
	}

}
