package net.imagej.server.converter;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;

/**
 * A converter that converts a DatasetView to a Dataset
 *
 * @author Petr Bainar
 */
@Plugin(type = Converter.class)
public class DatasetViewToDatasetConverter extends AbstractConverter<DatasetView, Dataset> {

	@Override
	public Class<Dataset> getOutputType() {
		return Dataset.class;
	}

	@Override
	public Class<DatasetView> getInputType() {
		return DatasetView.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		return (T) ((DatasetView) src).getData();
	}
}
