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

import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;

/**
 * Op that Evaluate arbitrary scripts.
 * 
 * @author Leon Yang
 */
@Plugin(type = Ops.Eval.class)
public class ScriptEval extends
	AbstractBinaryFunctionOp<String, Map<String, Object>, Map<String, Object>>
	implements Ops.Eval
{

	@Parameter
	private ScriptService scriptService;

	@Parameter
	private String language;

	@Parameter(required = false)
	private boolean process = true;

	// HACKs
	private String fakePath;

	@Override
	public void initialize() {
		fakePath = "foo." + scriptService.getLanguageByName(language)
			.getExtensions().get(0);
	}

	@Override
	public Map<String, Object> calculate(String input1,
		Map<String, Object> input2)
	{
		try {
			return scriptService.run(fakePath, input1, process, input2).get()
				.getOutputs();
		}
		catch (InterruptedException | ExecutionException exc) {
			throw new IllegalArgumentException(exc);
		}
	}

}
