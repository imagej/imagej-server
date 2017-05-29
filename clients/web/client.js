"use strict";

// default host
var HOST = 'http://localhost:8080';

// Java basic type definitions
var FLOAT_TYPES = new Set(['float', 'double', 'class java.lang.Float',
	'class java.lang.Double', 'class java.math.BigDecimal']);
var INT_TYPES = new Set(['int', 'long', 'short', 'byte',
	'class java.lang.Integer', 'class java.lang.Long', 'class java.lang.Short',
	'class java.lang.Byte', 'class java.math.BigInteger']);
var BOOL_TYPES = new Set(['boolean', 'class java.lang.Boolean']);
var STRING_TYPES = new Set(['char', 'class java.lang.Character', 'class java.lang.String']);
var DATE_TYPES = new Set(['class java.util.Date']);

// global timestamp for refreshing images
var timestamp;

// given the input information of a module, render it with appropriate widget
function showInput(input) {
	let required = input['required'];
	let dtClass = required ? 'required' : 'optional';
	let name = input['name'];
	let label = input['label'] || name;
	let genericType = input['genericType'];
	// the last segment of the type information disregarding the generic type
	let shortType = /([^.<]+)(<.*?>)?$/.exec(genericType)[1];
	let style = input['widgetStyle'];
	
	// the input widget is stored in this top level div
	let div = $('<div class="input_div"></div>');
	
	div.append($(`<label class="${dtClass}">${label}</label>`));
	
	// hover over the short type to show long type
	var timer;
	var delay = 500;
	let typeSpan = $(`<span> (${shortType}): </span>`).hover(function() {
		let self = $(this);
		timer = setTimeout(function() {
			self.text(' (' + genericType + '): ');
		}, delay);
	}, function() {
		let self = $(this);
		clearTimeout(timer);
		self.text(' (' + shortType + '): ');
	});
	div.append(typeSpan);
	
	// select appropriate widget style depending on the specified style or data type
	if (input['choices'] !== null) {
		switch(style) {
		case 'radioButtonHorizontal':
		case 'radioButtonVertical':
			let radioGroup = $('<div></div>');
			for (let choice of input['choices']) {
				let checked = (choice == input['defaultValue'] && !required) ? 'checked' : '';
				let rad = $(`<label><input type="radio" name="${name}" value="${choice}" ${checked}>${choice}</label>`);
				radioGroup.append(rad);
				if (style == 'radioButtonVertical') {
					// add br to make vertical radio buttons
					radioGroup.append($('<br>'));
				}
			}
			div.append(radioGroup);
			break;
		case 'listBox':
		default:
			let selectGroup = $('<select></select>');
			if (required) {
				// add an empty option to force a selection
				selectGroup.append('<option name="${name}" diabled selected style="display: none;"></option>');
			}
			for (let choice of input['choices']) {
				let selected = (!required && choice == input['defaultValue']) ? 'selected' : '';
				let opt = $(`<option value="${choice}" name="${name}" ${selected}>${choice}</option>`);
				selectGroup.append(opt);
			}
			div.append(selectGroup);
			break;
		}
	} else if (INT_TYPES.has(genericType) || FLOAT_TYPES.has(genericType)) {
		let min = input['minimumValue'] === null ? '' : `min="${input['minimumValue']}"`;
		let max = input['maximumValue'] === null ? '' : `max="${input['maximumValue']}"`;
		let softMin = input['softMinimum'] === null ? '' : `min="${input['softMinimum']}"`;
		let softMax= input['softMaximum'] === null ? '' : `max="${input['softMaximum']}"`;
		let defaultStep = INT_TYPES.has(genericType) ? '1' : 'any';
		let step = input['stepSize'] === null ? 'step="${defaultStep}"' : `step="${input['stepSize']}"`;
		let value = (required || input['defaultValue'] === null) ? '' : `value="${input['defaultValue']}"`;
		
		let spinner = $(`<input type="number" name="${name}" ${min} ${max} ${step} ${value}>`);

		if (style == 'slider' || style == 'scroll bar') {
			// bind the range and the spinner together
			let range = $(`<input type="range" ${softMin} ${softMax} ${step} ${value}>`);
			spinner.change(function() {
				range.val(spinner.val());
			});
			range.change(function() {
				spinner.val(range.val());
			});
			div.append(range).append(spinner);
		} else {
			div.append(spinner);
		}
	} else if (BOOL_TYPES.has(genericType)) {
		let value = (required || input['defaultValue'] === null) ? false : `value="${input['defaultValue']}"`;
		let checkbox = $(`<input type="checkbox" name="${name}" ${value}>`);
		checkbox.change(function() {
			$(this).val($(this).prop('checked'));
		});
		div.append(checkbox);
	} else if (STRING_TYPES.has(genericType)) {
		// arbitrary setting of number of columns
		let columns = (input['columnCount'] === null ? 6 : input['columnCount']) * 2;
		let value = (required || input['defaultValue'] === null) ? '' : `value="${input['defaultValue']}"`;

		switch(style) {
		case 'password':
			div.append($(`<input type="password" name="${name}" size="${columns}">`));
			break;
		case 'text area':
			div.append($(`<textarea name="${name}" ${value}></textarea>`));
			break;
		case 'text field':
		default:
			div.append($(`<input type="text" name="${name}" size="${columns}" ${value}>`));
		}
	} else if (DATE_TYPES.has(genericType)) {
		let value = (required || input['defaultValue'] === null) ? '' : `value="${input['defaultValue']}"`;
		div.append($(`<input type="date" name="${name}" ${value}>`));
	} else {
		// if we can't find an appropriate widget, assume image
		// this widget accept drag-and-drop images from the right panel, and actually use the id of that image
		let dropbox = $('<div class="dropbox"></div>');
		dropbox.append($('<span>Drop object from the right</span>'));
		// hidden input box to hold the image id, but the image instead of the id will be shown
		dropbox.append($(`<input type="text" name="${name}" style="display:none">`));
		dropbox.on('dragover', function(ev) {
			ev.preventDefault();
		});
		dropbox.on('drop', function(ev) {
			ev.preventDefault();
			let id = ev.originalEvent.dataTransfer.getData('text');
			if (id == '' || document.getElementById(id) === undefined) {
				alert('Invalid item dropped');
				return;
			}
			$('img', this).remove();
			$('span', this).css('display', 'none');
			$('input', this).val(id);
			let origin = $(document.getElementById(id));
			// clone the dragged image to this widget, and also make it draggable
			let clone = $(`<img class="thumbnail" src="${origin.attr('src')}" draggable="true">`);
			clone.on('dragstart', function(ev) {
				ev.originalEvent.dataTransfer.setData('text', id);				
			})
			clone.on('dragend', function(ev) {
				clone.remove();
				$('span', dropbox).css('display', 'inline-block');
			});
			dropbox.append(clone);
		});
		div.append(dropbox);
	}
	
	// hidden input field for raw JSON input (i.e. array, dictionary, or just strings)
	// usable when "use json" button is toggled
	div.append($(`<br><input class="json-input" type="text" name="${name}" size="10" placeholder="Raw JSON" disabled>`));
	
	$('#inputs').append(div);
}

// render the output of a module
function showOutput(output) {
	let required = output['required'];
	let dtClass = required ? 'required' : 'optional';
	let name = output['name'];
	let label = output['label'] || name;
	let genericType = output['genericType'];
	// the last segment of the type information disregarding the generic type
	let shortType = /([^.<]+)(<.*?>)?$/.exec(genericType)[1];

	// the output widget is stored in this top level div
	let div = $('<div class="output_div"></div>');
	
	div.append($(`<label class="${dtClass}">${label}</label>`));
	
	// hover over the short type to show long type
	var timer;
	var delay = 500;
	let typeSpan = $(`<span> (${shortType}): </span>`).hover(function() {
		let self = $(this);
		timer = setTimeout(function() {
			self.text(' (' + genericType + '): ');
		}, delay);
	}, function() {
		let self = $(this);
		clearTimeout(timer);
		self.text(' (' + shortType + '): ');
	});
	div.append(typeSpan);
	
	div.append($(`<span class="value _output_${name}"></span>`));
	
	$('#outputs').append(div);
}

// display a module's information (inputs/outputs) given its id
function showModule(id) {
	$('#module').css('display', 'block');
	$.getJSON(HOST + '/modules/' + id, function(data, status) {
		let split = data['identifier'].split('.');
		let shortName = split[split.length - 1];

		// legend
		$('#module legend').empty().append(
				$(document.createElement('a')).text(shortName)
				.attr('href', HOST + '/modules/' + data['identifier']));

		// inputs
		$('#inputs').empty();
		for (let input of data['inputs']) {
			// populate each input
			showInput(input);
		}

		// outputs
		$('#outputs').empty();
		for (let output of data['outputs']) {
			// populate each output
			showOutput(output);
		}

		// execute
		$('form').off('submit').on('submit', function(ev) {
			ev.preventDefault();
			// harvest inputs
			let jsonInputs = {};
			for (let pair of $('form').serializeArray()) {
				let input = $(`input[name=${pair['name']}]:enabled`);
				// if the input is number or is raw JSON, we parse it (by default they are all just strings)
				if (input.attr('type') == 'number' || input.attr('class') == 'json-input') {
					pair['value'] = JSON.parse(pair['value']);
				}
				jsonInputs[pair['name']] = pair['value'];
			}

			// make POST request for execution
			$.ajax({
				type: 'POST',
				url: HOST + '/modules/' + id,
				data: JSON.stringify(jsonInputs),
				success: function(outputs, status) {
					// populate outputs
					for (let name in outputs) {
						let output = outputs[name];
						// pretty print
						let text = JSON.stringify(output, null, 2);
						let span = $(`.value._output_${name}`)
						// check if the output represents an image
						if ((typeof output) == "string" && output.startsWith('object:')) {
							span.empty();
							let obj_url = HOST + '/objects/' + output;
							span.append($([
								`<a href="${obj_url}" target="_blank">${text}</a>`,
								`<button class="_output_${name}">View As</button>`,
								`<input class="format _output_${name}" type="text" size="5" value="jpg">`
							].join('\n')));
							$(`button._output_${name}`).on('click', function(ev) {
								ev.preventDefault();
								let clazz = $(`button._output_${name}`).attr('class');
								let url = HOST + '/objects/' + output + '/' + $(`.format.${clazz}`).val();
								window.open(url, 'popUpWindow');
							})
						} else {
							span.text(output);
						}
					}
				},
				error: function(xhr, status, err) {
					console.log(err);
				},
				dataType: 'json',
				contentType: 'application/json'
			});
		});
	});
}

// show the list of all available modules
function showModules() {
	$.getJSON(HOST + '/modules', function(data, status) {
		let optgroups = {}; // for now, we only have "command"
		for (let module of data) {
			let firstcolon = module.indexOf(':');
			let lastdot = module.lastIndexOf('.');

			let type = module.slice(0, firstcolon);
			let clazz = module.slice(lastdot + 1);
			let source = module.slice(firstcolon + 1, lastdot);

			if (!(type in optgroups)) {
				optgroups[type] = [];
			}

			let text = clazz + ' (' + source + ')';
			optgroups[type].push($(document.createElement('option'))
					.text(text)
					.attr({value: module})
					.addClass('module'));
		}

		for (let type in optgroups) {
			let optgroup = $(document.createElement('optgroup')).attr({
				label : type
			});
			$('#modules').append(optgroup);
			optgroups[type].sort(function(a, b) {
				return a.text() < b.text() ? -1 : 1;
			});
			optgroup.append(optgroups[type]);
		}

		$('#modules').attr({
			size : Math.min(10, data.length)
		}).change(function() {
			showModule(this.value);
		});
	});
}

// refresh the images on the right panel by updating the timestamp
function refresh() {
	timestamp = (new Date()).getTime();
	$.getJSON(HOST + '/objects', function(data) {
		$('#objs-panel-frame').html('');
		for (let id of data) {
			let src = `${HOST}/objects/${id}/jpg?timestamp=${timestamp}`;
			let img = $(`<img class="thumbnail" src="${src}" id="${id}" alt="click to show details" draggable="true">`);
			img.on('dragstart', function (ev) {
				ev.originalEvent.dataTransfer.setData('text', ev.target.id);
			});
			let link = $(`<a href="${HOST}/objects/${id}" target="_blank"></a>`);
			link.append(img);
			$('#objs-panel-frame').append($(`<div class="img-box"></div>`).append(link));
			// TODO: add alt
		}
		$('#default-objs-msg').css('display', $('.img-box').length == 0 ? 'inline' : 'none');
	})
}

// upload a file to the imagej-server
function upload() {
	let files = $('#objs-panel-file').prop('files');
	if (files.length != 1) {
		alert(`Need exactly 1 file to upload (has ${files.length})`);
		return;
	}
	let data = new FormData();
	data.append('file', files[0]);
	$.ajax({
		url: HOST + '/objects/upload',
		type: 'POST',
		data: data,
		cache: false,
		contentType: false,
		processData: false,
		success: function(rtn, status, xhr) {
			refresh();
		},
		error: function(xhr, status, err) {
			// TODO: better error handling
			console.log(err);
		}
	})
};

// toggle to enable/disable the JSON input fields
// TODO: when enabled, should we populate the non-JSON inputs into the JSON input fields?
$('#use-json').click(function() {
	if (this.checked) {
		$('form').find('input[name]').prop('disabled', true);
		$('form').find('input[class=json-input]').prop('disabled', false).css('display', 'inline-block');
	} else {
		$('form').find('input[name]').prop('disabled', false);
		$('form').find('input[class=json-input]').prop('disabled', true).css('display', 'none');
	}
})

window.onload = showModules;
$('#objs-panel-refresh').click(refresh);
$('#objs-panel-upload').click(upload);
refresh();