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

package net.imagej.server.converter;

import net.imglib2.type.numeric.NumericType;

import org.scijava.convert.AbstractConverter;

/**
 * Abstract class for Converters that convert a Number to a NumericType.
 * Consider migrating to scijava.
 *
 * @param <I> input type
 * @param <O> output type
 * @author Leon Yang
 */
public abstract class NumberToNumericTypeConverter<I extends Number, O extends NumericType<O>>
	extends AbstractConverter<I, O>
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		return (T) convert((I) src);
	}

	protected abstract O convert(final I src);

}
