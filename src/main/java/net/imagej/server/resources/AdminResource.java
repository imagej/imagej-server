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

package net.imagej.server.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.dropwizard.setup.Environment;
import net.imagej.server.services.JsonService;

import org.scijava.menu.MenuService;
import org.scijava.menu.ShadowMenu;
import org.scijava.module.ModuleInfo;

/**
 * Resource for administration.
 * 
 * @author Leon Yang
 */
@Path("/admin")
public class AdminResource {
	
	@Inject
	private MenuService menuService;
	
	@Inject
	private JsonService jsonService;
	
	@Inject
	private Environment env;	
	
	@GET
	@Path("menuNew")
	public String menuNew() throws JsonProcessingException {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("Level", 0);
		map.put("Label", "Root");
		map.put("Command", null);
		
		int childrenCounter = 0;
		for (ShadowMenu shadowMenu : menuService.getMenu().getChildren()) {
			map.put("Child"+childrenCounter, getMenuNewRecursively(1, shadowMenu));
			childrenCounter++;
		}
		
		return jsonService.parseObject(map);
	}
	
	private Map<String, Object> getMenuNewRecursively(int level, ShadowMenu shadowMenu) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("Level", level);
		result.put("Label", shadowMenu.getName());
		
		ModuleInfo moduleInfo = shadowMenu.getModuleInfo();
		if (moduleInfo == null) {
			result.put("Command", null);
			int childrenCounter = 0;
			for (ShadowMenu shadowSubMenu : shadowMenu.getChildren()) {
				result.put("Child"+childrenCounter, getMenuNewRecursively(level+1, shadowSubMenu));
				childrenCounter++;
			}
		}
		else {
			result.put("Command", moduleInfo.getDelegateClassName());
		}
		
		return result;
	}

	/**
	 * Stop the imagej-server.
	 * 
	 * @return always OK
	 */
	@Path("stop")
	@DELETE
	public Response stop() {
		// stop the server in a separate thread in case the server hangs waiting for
		// the current thread.
		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					env.getApplicationContext().getServer().stop();
				}
				catch (Exception exc) {}
			}
		});
		try {
			return Response.ok().build();
		}
		finally {
			thread.start();
		}
	}

}
