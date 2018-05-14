package net.imagej.server.utilities;

import java.util.LinkedHashMap;
import java.util.Map;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuService;
import org.scijava.menu.ShadowMenu;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Plugin that provides ImageJ2 menu structure, making use of SciJava {@link MenuService}.
 * 
 * @author Petr Bainar
 * @author Jan Kozusznik
 */
@Plugin(type = Command.class)
public class MenuProvider extends ContextCommand {

	@Parameter
	private MenuService menuService;
	
	@Parameter(type = ItemIO.OUTPUT)
	private Map<String, Object> mappedMenuItems;
	
	@Override
	public void run() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("Label", "Root");
		map.put("Command", null);
		
		int childrenCounter = 0;
		for (ShadowMenu shadowMenu : menuService.getMenu().getChildren()) {
			map.put("Child"+childrenCounter, getMenuNewRecursively(1, shadowMenu));
			childrenCounter++;
		}
		mappedMenuItems = map;
	}
	
	private Map<String, Object> getMenuNewRecursively(int level, ShadowMenu shadowMenu) {
		Map<String, Object> result = new LinkedHashMap<>();
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
}
