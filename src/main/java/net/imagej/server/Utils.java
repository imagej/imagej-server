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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Leon Yang
 */
public class Utils {

	private Utils() {
		// NB: prevent instantiation of utility class.
	}

	public static final String alphanumeric_lower =
		"0123456789abcdefghijklmnopqrstuvwxyz";

	public static final String alphanumeric_both =
		"01234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	/**
	 * Generates a lowercase alphanumeric random String.
	 * 
	 * @param len length of String to be generated
	 * @return random generated String
	 */
	public static String randomString(final int len) {
		return randomString(len, alphanumeric_lower);
	}

	/**
	 * Generate a random string of length {@code len} from the given
	 * {@code alphabet}.
	 */
	public static String randomString(final int len, final String alphabet) {
		final StringBuilder sb = new StringBuilder(len);
		final Random random = ThreadLocalRandom.current();
		for (int i = 0; i < len; i++)
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		return sb.toString();
	}

	/**
	 * Converts the current time in milliseconds to an 8-digit lowercase
	 * alphanumeric String.
	 * 
	 * @return 8-digit String encoding the current timestamp
	 */
	public static String timestampString() {
		return Long.toUnsignedString(System.currentTimeMillis(), 36);
	}

	/**
	 * Generates a (8+n) bit alphanumeric lowercase random ID. The first eight
	 * digits encode the timestamp.
	 * 
	 * @return a timestamped random ID
	 */
	public static String timestampedId(final int n) {
		return timestampString() + randomString(n);
	}

}
