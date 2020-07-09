package net.imagej.server.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

class JsonSerializerAdapter<T> extends StdSerializer<T> {

	private SciJavaJsonSerializer<T> serializedDelegate;

	public JsonSerializerAdapter(
		SciJavaJsonSerializer<T> serializedDelegate)
	{
		super(serializedDelegate.handleType());
		this.serializedDelegate = serializedDelegate;
	}


	@Override
	public void serialize(T value, JsonGenerator gen,
		SerializerProvider provider) throws IOException
	{
		serializedDelegate.serialize(value, gen, provider);
	}


}