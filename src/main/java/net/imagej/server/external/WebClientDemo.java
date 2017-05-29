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

package net.imagej.server.external;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import org.scijava.widget.TextWidget;

/**
 * @author Leon Yang
 */
@Plugin(type = Command.class)
public class WebClientDemo implements Command {

	// -- required parameters --

	@Parameter(choices = { "apple", "orange", "strawberry" })
	private String choicesDefaultStyle;

	@Parameter(choices = { "apple", "orange", "strawberry" },
		style = ChoiceWidget.LIST_BOX_STYLE)
	private String choicesListStyle;

	@Parameter(choices = { "apple", "orange", "strawberry" },
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
	private String choicesRadioHorizontalStyle;

	@Parameter(choices = { "apple", "orange", "strawberry" },
		style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE)
	private String choicesRadioVerticalStyle;

	@Parameter(min = "0", max = "100", stepSize = "10",
		style = NumberWidget.SCROLL_BAR_STYLE)
	private int scrollbarInt;

	@Parameter(min = "0", max = "1", stepSize = "0.1",
		style = NumberWidget.SCROLL_BAR_STYLE)
	private int scrollbarDouble;

	@Parameter
	private int primitiveInt;

	@Parameter
	private Integer boxedInt;

	@Parameter
	private long primitiveLong;

	@Parameter
	private Long boxedLong;

	@Parameter
	private float primitiveFloat;

	@Parameter
	private Float boxedFloat;

	@Parameter
	private double primitiveDouble;

	@Parameter
	private Double boxedDouble;

	@Parameter
	private short primitiveShort;

	@Parameter
	private Short boxedShort;

	@Parameter
	private byte primitiveByte;

	@Parameter
	private Byte boxedByte;

	@Parameter
	private BigInteger bigInteger;

	@Parameter
	private BigDecimal bigDecimal;

	@Parameter
	private boolean primitiveBool;

	@Parameter
	private Boolean boxedBool;

	@Parameter
	private String textDefaultStyle;

	@Parameter(style = TextWidget.PASSWORD_STYLE)
	private String textPasswordStyle;

	@Parameter(style = TextWidget.AREA_STYLE)
	private String textAreaStyle;

	@Parameter(style = TextWidget.FIELD_STYLE)
	private String textFieldStyle;

	@Parameter
	private Date date;

	@Override
	public void run() {
		// NB: nothing to do
	}

}
