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

package net.imagej.server.console;

import java.util.LinkedList;

import net.imagej.server.ImageJServer;
import net.imagej.server.ImageJServerService;

import org.scijava.console.AbstractConsoleArgument;
import org.scijava.console.ConsoleArgument;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.startup.StartupService;
import org.scijava.ui.UIService;

/**
 * Handles the {@code --server} argument to signal that an ImageJ Server
 * should be started immediately when ImageJ launches.
 *
 * @author Curtis Rueden
 */
@Plugin(type = ConsoleArgument.class)
public class ServerArgument extends AbstractConsoleArgument {

	@Parameter(required = false)
	private ImageJServerService imagejServerService;

	@Parameter(required = false)
	private ObjectService objectService;

	@Parameter(required = false)
	private UIService uiService;

	@Parameter(required = false)
	private StartupService startupService;

	@Parameter(required = false)
	private LogService log;

	// -- Constructor --

	public ServerArgument() {
		super(1, "--server");
	}

	// -- ConsoleArgument methods --

	@Override
	public void handle(final LinkedList<String> args) {
		if (!supports(args)) return;

		args.removeFirst(); // --server

		final ImageJServer server = imagejServerService.start();
		objectService.addObject(server);

		if (startupService != null) {
			startupService.addOperation(() -> {
				// In headless mode, block until server shuts down.
				if (uiService == null || !uiService.isHeadless()) return;
				try {
					server.join();
				}
				catch (final InterruptedException exc) {
					if (log != null) log.error(exc);
				}
			});
		}
	}

	// -- Typed methods --

	@Override
	public boolean supports(final LinkedList<String> args) {
		return imagejServerService != null && objectService != null &&
			super.supports(args);
	}
}
