
package net.imagej.server;

import java.lang.reflect.Field;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModuleItem;

public class WebCommandModuleItem<T> extends CommandModuleItem<T> {

	public boolean isResolved;
	public T startingValue;

	public WebCommandModuleItem(final CommandInfo commandInfo,
		final Field field)
	{
		super(commandInfo, field);
	}
}
