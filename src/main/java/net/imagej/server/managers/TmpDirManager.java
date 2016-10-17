/*
 * #%L
 * ImageJ server for RESTful access to ImageJ.
 * %%
 * Copyright (C) 2013 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

package net.imagej.server.managers;

import io.dropwizard.lifecycle.Managed;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lifecycle manager that cleans up the temp directory at server shutdown.
 *
 * @author Leon Yang
 */
public class TmpDirManager implements Managed {

	private final String tmpDir;

	public TmpDirManager(final String tmpDir) {
		this.tmpDir = tmpDir;
	}

	@Override
	public void start() throws Exception {
		new File(tmpDir).mkdirs();
	}

	@Override
	public void stop() throws Exception {
		cleanDir(new File(tmpDir));
	}

	public String getTmpDir() {
		return tmpDir;
	}

	public Path getFilePath(final String filename) {
		return Paths.get(tmpDir, filename);
	}

	// NB: following methods should be in some util class

	private static final String alphanumeric_lower =
		"0123456789abcdefghijklmnopqrstuvwxyz";

	public String randomString(final int len) {
		return randomString("", len, "", alphanumeric_lower);
	}

	public String randomString(final String prefix, final int len,
		final String suffix)
	{
		return randomString(prefix, len, suffix, alphanumeric_lower);
	}

	/**
	 * Generate a random string of length {@code len} from the given
	 * {@code alphabet}, then prepended with prefix and appended with suffix.
	 */
	public String randomString(final String prefix, final int len,
		final String suffix, final String alphabet)
	{
		final StringBuilder sb = new StringBuilder(len);
		final Random random = ThreadLocalRandom.current();
		for (int i = 0; i < len; i++)
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		return prefix + sb.toString() + suffix;
	}

	/**
	 * Removes all directory and files of the given directory recursively
	 *
	 * @param dir directory to clean
	 * @return true if all files and sub-directories are removed successfully
	 */
	private boolean cleanDir(final File dir) {
		boolean status = true;
		for (final File file : dir.listFiles()) {
			status &= file.isDirectory() ? cleanDir(file) : file.delete();
		}
		return status;
	}

}
