/*
 * #%L
 * ImageJ server for RESTful access to ImageJ.
 * %%
 * Copyright (C) 2013 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

package net.imagej.server.mixins;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

/**
 * Jackson MixIns for some specific types in order to produce better output
 * format.
 *
 * @author Leon Yang
 */
public class Mixins {

	private Mixins() {}

	@JsonAutoDetect(getterVisibility = Visibility.NONE)
	public static abstract class ComplexTypeMixIn<T extends ComplexTypeMixIn<T>>
		implements ComplexType<T>
	{

		@Override
		@JsonProperty(value = "real")
		public abstract double getRealDouble();

		@Override
		@JsonProperty(value = "imaginary")
		public abstract double getImaginaryDouble();
	}

	public static abstract class RealTypeMixIn<T extends RealTypeMixIn<T>>
		implements RealType<T>
	{

		@Override
		@JsonValue
		public abstract double getRealDouble();
	}

	public static abstract class IntegerTypeMixIn<T extends IntegerTypeMixIn<T>>
		implements IntegerType<T>
	{

		@Override
		@JsonValue
		public abstract long getIntegerLong();
	}

	public static void registerMixIns(final ObjectMapper mapper) {
		mapper.addMixIn(ComplexType.class, ComplexTypeMixIn.class);
		mapper.addMixIn(RealType.class, RealTypeMixIn.class);
		mapper.addMixIn(IntegerType.class, IntegerTypeMixIn.class);
	}
}
