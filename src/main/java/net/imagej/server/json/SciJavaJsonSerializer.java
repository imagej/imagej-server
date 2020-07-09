
package net.imagej.server.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

import org.scijava.plugin.SciJavaPlugin;

public interface SciJavaJsonSerializer<T> extends SciJavaPlugin

{

	void serialize(T value, JsonGenerator gen,
		SerializerProvider serializers)
		throws IOException;

	Class<T> handleType();

	default boolean isSupportedBy(Class<?> desiredClass) {
		return handleType().isAssignableFrom(desiredClass);
	}

	default public void register(ObjectMapper mapper) {
		SimpleModule mod = new SimpleModule();
		mod.addSerializer(new JsonSerializerAdapter<>(this));
		mapper.registerModule(mod);
	}
}
