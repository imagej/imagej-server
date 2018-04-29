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

package net.imagej.server.commands;

import java.util.List;

import net.imagej.server.ImageJServer;
import net.imagej.server.ImageJServerService;

import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Starts the ImageJ Server, if one is not already running.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, menuPath = "Plugins > Utilities > Start Server")
public class StartServer implements Command {

	@Parameter
	private ObjectService objectService;

	@Parameter
	private ImageJServerService imagejServerService;

	@Parameter(required = false)
	private UIService ui;

	@Override
	public void run() {
		final List<ImageJServer> servers = //
			objectService.getObjects(ImageJServer.class);
		if (servers.isEmpty()) {
			final ImageJServer server = imagejServerService.start();
			objectService.addObject(server);
			if (ui != null) {
				ui.showDialog("ImageJ Server started successfully!", "ImageJ Server");
			}
		}
	}
}
