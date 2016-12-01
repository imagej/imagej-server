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

package net.imagej.server.converter;

import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converters that convert a Number to a NumericType. Consider migrating to
 * scijava.
 *
 * @author Leon Yang
 */
public class NumberToNumericTypeConverters {

	private NumberToNumericTypeConverters() {}

	@Plugin(type = Converter.class)
	public static class DoubleToDoubleTypeConverter extends
		NumberToNumericTypeConverter<Double, DoubleType>
	{

		@Override
		public Class<DoubleType> getOutputType() {
			return DoubleType.class;
		}

		@Override
		public Class<Double> getInputType() {
			return Double.class;
		}

		@Override
		protected DoubleType convert(final Double src) {
			return new DoubleType(src);
		}

	}

	@Plugin(type = Converter.class)
	public static class IntegerToIntTypeConverter extends
		NumberToNumericTypeConverter<Integer, IntType>
	{

		@Override
		public Class<IntType> getOutputType() {
			return IntType.class;
		}

		@Override
		public Class<Integer> getInputType() {
			return Integer.class;
		}

		@Override
		protected IntType convert(final Integer src) {
			return new IntType(src);
		}

	}

	@Plugin(type = Converter.class)
	public static class LongToLongTypeConverter extends
		NumberToNumericTypeConverter<Long, LongType>
	{

		@Override
		public Class<LongType> getOutputType() {
			return LongType.class;
		}

		@Override
		public Class<Long> getInputType() {
			return Long.class;
		}

		@Override
		protected LongType convert(final Long src) {
			return new LongType(src);
		}

	}

	@Plugin(type = Converter.class)
	public static class FloatToFloatTypeConverter extends
		NumberToNumericTypeConverter<Float, FloatType>
	{

		@Override
		public Class<FloatType> getOutputType() {
			return FloatType.class;
		}

		@Override
		public Class<Float> getInputType() {
			return Float.class;
		}

		@Override
		protected FloatType convert(final Float src) {
			return new FloatType(src);
		}

	}

}
