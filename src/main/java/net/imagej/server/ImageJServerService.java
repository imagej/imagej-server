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

import net.imagej.ImageJService;

/**
 * Service for ImageJ Server operations.
 *
 * @author Curtis Rueden
 */
public interface ImageJServerService extends ImageJService {

	/**
	 * Launches an instance of the ImageJ Server.
	 * 
	 * @param args Arguments to pass to the server.
	 * @throws RuntimeException if something goes wrong launching the server.
	 */
	ImageJServer start(String... args);
}
