
package net.imagej.server.modifiers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.util.Collection;

public class SerializerModifier<T> extends ObjectMapperModifier {

	private StdSerializer<T> serializer;

	public SerializerModifier(Class<T> supportedClass,
		Collection<Class<?>> excludedClasses, StdSerializer<T> serializer)
	{
		super(supportedClass, excludedClasses);
		this.serializer = serializer;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void accept(ObjectMapper mapper) {
		SimpleModule mod = new SimpleModule();
		mod.addSerializer((Class<T>) getSupportedClass(), serializer);
		mapper.registerModule(mod);
	}

}
