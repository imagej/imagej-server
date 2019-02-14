
package net.imagej.server.modifiers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.scijava.plugin.SciJavaPlugin;

public abstract class ObjectMapperModifier implements SciJavaPlugin,
	Consumer<ObjectMapper>
{

	private Class<?> supportedClass;
	private Set<Class<?>> excludedClasses;

	public ObjectMapperModifier(Class<?> supportedClass,
		Collection<Class<?>> excludedClasses)
	{
		super();
		this.supportedClass = supportedClass;
		this.excludedClasses = new HashSet<>(excludedClasses);
	}

	protected Class<?> getSupportedClass() {
		return supportedClass;
	}

	protected Set<Class<?>> getExcludedClasses() {
		return excludedClasses;
	}

	public boolean isSupportedBy(Class<?> desiredClass) {
		return getSupportedClass().isAssignableFrom(desiredClass) &&
			getExcludedClasses().stream().noneMatch(v -> v.isAssignableFrom(
				desiredClass));
	}
}
