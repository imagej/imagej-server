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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;

/**
 * Command that evaluates arbitrary scripts.
 * 
 * @author Leon Yang
 * @author Curtis Rueden
 */
@Plugin(type = Command.class)
public class ScriptEval implements Command {

	@Parameter
	private ScriptService scriptService;

	@Parameter
	private String language;

	@Parameter
	private String script;

	@Parameter(required = false)
	private Map<String, Object> args;

	@Parameter(required = false)
	private boolean process = true;

	@Parameter(type = ItemIO.OUTPUT)
	private Map<String, Object> outputs;

	@Override
	public void run() {
		// Make an honest effort to figure out what language they mean. :-)
		final String ext;
		final ScriptLanguage langByExt = //
			scriptService.getLanguageByExtension(language);
		if (langByExt != null) ext = language;
		else {
			final ScriptLanguage langByName = scriptService.getLanguageByName(
				language);
			if (langByName != null) ext = langByName.getExtensions().get(0);
			else throw new IllegalArgumentException("Unknown language: " + language);
		}
		final String fakePath = "script." + ext;

		try {
			final Map<String, Object> safeArgs = //
				args == null ? Collections.emptyMap() : args;
			outputs = scriptService.run(fakePath, script, process, safeArgs).get()
				.getOutputs();
		}
		catch (final InterruptedException | ExecutionException exc) {
			throw new IllegalArgumentException(exc);
		}
	}

}
