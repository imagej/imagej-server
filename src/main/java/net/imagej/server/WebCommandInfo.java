
package net.imagej.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModuleItem;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;

public class WebCommandInfo extends CommandInfo {

	private final Module relatedModule;

	public WebCommandInfo(CommandInfo commandInfo, Module module) {
		super(commandInfo);
		relatedModule = module;
	}

	@Override
	public Iterable<ModuleItem<?>> inputs() {

		final List<WebCommandModuleItem<?>> checkedInputs = new ArrayList<>();
		for (final ModuleItem<?> input : super.inputs()) {
			if (input instanceof CommandModuleItem) {
				final WebCommandModuleItem<Object> webCommandModuleItem =
					new WebCommandModuleItem<>(this, ((CommandModuleItem<?>) input)
						.getField());
				final String name = input.getName();

				final boolean isResolved = relatedModule.isInputResolved(name);

				// Include resolved status in the JSON feed.
				// This is handy for judiciously overwriting already-resolved inputs,
				// particularly the "active image" inputs, which will be reported as
				// resolved, but not necessarily match what's selected on the client
				// side.
				webCommandModuleItem.isResolved = isResolved;

				// If the input is not resolved, include startingValue in the JSON feed.
				// This is useful for populating the dialog.
				webCommandModuleItem.startingValue = (!isResolved) ? relatedModule
					.getInput(name) : null;

				checkedInputs.add(webCommandModuleItem);
			}
		}

		return Collections.unmodifiableList(checkedInputs);
	}
}
