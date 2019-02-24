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

package net.imagej.server.mixins;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * Jackson MixIns for some specific types in order to produce better output
 * format.
 *
 * @author Leon Yang
 */
public class Mixins {

	private static final Class<?>[] SUPPORT = { ComplexType.class,
		ModuleInfo.class, ModuleItem.class };

	private Mixins() {}

	/**
	 * Checks if a given class will be supported, i.e. affected, by the registered
	 * MixIn types.
	 * 
	 * @param beanClass class to be checked.
	 * @return true if the given class is supported.
	 */
	public static boolean support(Class<?> beanClass) {
		return Arrays.stream(SUPPORT).anyMatch(clazz -> clazz.isAssignableFrom(
			beanClass));
	}

	@JsonAutoDetect(getterVisibility = Visibility.NONE)
	protected static abstract class ToStringMixIn {

		@JsonValue
		@Override
		public abstract String toString();
	}

	protected static abstract class RealTypeMixIn<T extends RealType<T>>
		implements RealType<T>
	{

		@JsonValue(value = false)
		@Override
		public abstract String toString();

		@JsonValue
		@Override
		public abstract double getRealDouble();
	}

	protected static abstract class IntegerTypeMixIn<T extends IntegerType<T>>
		extends RealTypeMixIn<T> implements IntegerType<T>
	{

		@JsonValue(value = false)
		@Override
		public abstract double getRealDouble();

		@JsonValue
		@Override
		public abstract long getIntegerLong();
	}

	@JsonAutoDetect(getterVisibility = Visibility.NONE,
		isGetterVisibility = Visibility.NONE)
	protected static abstract class ModuleInfoMixIn implements ModuleInfo {

		@Override
		@JsonProperty
		public abstract String getIdentifier();

		@Override
		@JsonProperty("inputs")
		public abstract List<ModuleItem<?>> inputs();

		@Override
		@JsonProperty("outputs")
		public abstract List<ModuleItem<?>> outputs();
	}

	@JsonAutoDetect(getterVisibility = Visibility.NONE,
		isGetterVisibility = Visibility.NONE)
	protected static abstract class ModuleItemMixIn<T> implements ModuleItem<T> {

		@Override
		@JsonProperty
		public abstract String getName();

		@Override
		@JsonProperty
		public abstract String getLabel();

		@Override
		@JsonProperty
		public abstract boolean isRequired();

		@Override
		@JsonProperty
		public abstract Type getGenericType();

		@Override
		@JsonProperty
		public abstract String getWidgetStyle();

		@Override
		@JsonProperty
		public abstract T getDefaultValue();

		@Override
		@JsonProperty
		public abstract T getMinimumValue();

		@Override
		@JsonProperty
		public abstract T getMaximumValue();

		@Override
		@JsonProperty
		public abstract T getSoftMinimum();

		@Override
		@JsonProperty
		public abstract T getSoftMaximum();

		@Override
		@JsonProperty
		public abstract Number getStepSize();

		@Override
		@JsonProperty
		public abstract int getColumnCount();

		@Override
		@JsonProperty
		public abstract List<T> getChoices();
	}

	protected static abstract class TypeMixIn implements Type {

		@JsonValue
		@Override
		public abstract String toString();
	}

	protected static abstract class OutOfBoundsFactoryMixIn<T, F> implements
		OutOfBoundsFactory<T, F>
	{

		@JsonValue
		@Override
		public abstract String toString();
	}

	public static void registerMixIns(final ObjectMapper mapper) {
		mapper.addMixIn(ComplexType.class, ToStringMixIn.class);
		mapper.addMixIn(RealType.class, RealTypeMixIn.class);
		mapper.addMixIn(IntegerType.class, IntegerTypeMixIn.class);
		mapper.addMixIn(ModuleInfo.class, ModuleInfoMixIn.class);
		mapper.addMixIn(ModuleItem.class, ModuleItemMixIn.class);
		mapper.addMixIn(Type.class, TypeMixIn.class);
		mapper.addMixIn(OutOfBoundsFactory.class, OutOfBoundsFactoryMixIn.class);
	}
}
