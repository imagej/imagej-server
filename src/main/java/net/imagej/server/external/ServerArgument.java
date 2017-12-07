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

import java.util.HashSet;
import java.util.LinkedList;

import org.scijava.console.AbstractConsoleArgument;
import org.scijava.console.ConsoleArgument;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Handles the {@code --server} argument to run server in
 * {@link ServerService}.
 * 
 * <p>Optional parameters include:</p>
 * <ul>
 * <li>{@code -no-headless}: Run server with UI.</li>
 * <li>{@code -no-blocking}: Run server without blocking the main thread.</li>
 * </ul>
 *
 * @author Leon Yang
 */
@Plugin(type = ConsoleArgument.class)
public class ServerArgument extends AbstractConsoleArgument {

	@Parameter(required = false)
	private UIService uiService;

	@Parameter(required = false)
	private ServerService serverService;

	// -- Constructor --

	public ServerArgument() {
		super(1, "--server");
	}

	// -- ConsoleArgument methods --

	@Override
	public void handle(final LinkedList<String> args) {
		if (!supports(args)) return;

		args.removeFirst();

		final HashSet<String> params = new HashSet<>();
		while (getParam(args) != null) {
			params.add(args.removeFirst());
		}
		final boolean noHeadless = params.contains("-no-headless");
		final boolean noBlocking = params.contains("-no-blocking");

		if (uiService == null) {
			log().warn("UIService unavailable");
		}
		else {
			// server run headlessly by default
			uiService.setHeadless(!noHeadless);
		}

		if (!noBlocking && !args.isEmpty()) {
			log().warn("Arguments after --server will be blocked");
		}

		// server blocks by default
		serverService.launch(!noBlocking);
	}

	// -- Typed methods --

	@Override
	public boolean supports(final LinkedList<String> args) {
		return serverService != null && super.supports(args);
	}

}
