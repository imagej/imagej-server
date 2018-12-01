package net.imagej.server;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModuleItem;

public class WebCommandModuleItem<T> extends CommandModuleItem<T> {

	public boolean isResolved;
	public Object startingValue;

	public WebCommandModuleItem (CommandInfo commandInfo, CommandModuleItem<T> commandModuleItem) {
		super(commandInfo, commandModuleItem.getField());
	}
}
