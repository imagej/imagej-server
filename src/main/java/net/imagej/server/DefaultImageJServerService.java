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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.scijava.io.ByteArrayByteBank;
import org.scijava.io.ByteBank;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.FileUtils;

/**
 * Default implementation of {@link ImageJServerService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultImageJServerService extends AbstractService implements
	ImageJServerService
{

	/**
	 * Collection of servers that have been doled out.
	 * <p>
	 * We keep these here so we can stop them all upon dispose.
	 * </p>
	 */
	private Set<ImageJServer> servers = new HashSet<>();

	@Parameter
	private LogService log;

	private String configFilePath;

	@Override
	public ImageJServer start(final String... args) {
		final String[] arguments = args == null || args.length == 0 ? //
			new String[] { "server", configFilePath() } : args;
		final ImageJServer app = new ImageJServer(context()) {
			@Override
			public void stop() throws Exception {
				servers.remove(this);
				super.stop();
			}
		};
		try {
			app.run(arguments);
		}
		catch (final Exception exc) {
			throw new RuntimeException(exc);
		}
		servers.add(app);
		return app;
	}

	@Override
	public void dispose() {
		for (final ImageJServer server : servers) {
			try {
				server.stop();
			}
			catch (final Exception exc) {
				log.error(exc);
			}
		}
	}

	// -- Helper methods --

	private String configFilePath() {
		if (configFilePath == null) initConfigFilePath();
		return configFilePath;
	}

	private synchronized void initConfigFilePath() {
		if (configFilePath != null) return;

		try {
			final InputStream in = getClass().getResourceAsStream("imagej-server.yml");
			final byte[] bytes = readStreamFully(in);

			final File configFile = File.createTempFile("imagej-server", ".yml");
			FileUtils.writeFile(configFile, bytes);
			configFile.deleteOnExit();
			configFilePath = configFile.getAbsolutePath();
		}
		catch (final IOException exc) {
			log.error(exc);
		}
	}

	// TODO: Move this to SciJava Common.
	private byte[] readStreamFully(final InputStream in) throws IOException {
		final ByteBank bank = new ByteArrayByteBank();
		byte[] buf = new byte[256 * 1024];
		while (true) {
			final int r = in.read(buf);
			if (r <= 0) break;
			bank.appendBytes(buf, r);
		}
		return bank.toByteArray();
	}
}
