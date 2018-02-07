package net.imagej.server.converter;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;

/**
 * A converter that converts a DatasetView to an ImageDisplay
 *
 * @author Petr Bainar
 */
@Plugin(type = Converter.class)
public class DatasetViewToImageDisplayConverter extends AbstractConverter<DatasetView, ImageDisplay> {

	@Parameter
	private DisplayService displayService;

	@Override
	public Class<ImageDisplay> getOutputType() {
		return ImageDisplay.class;
	}

	@Override
	public Class<DatasetView> getInputType() {
		return DatasetView.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		return (T) displayService.createDisplay((DatasetView) src);
	}
}
