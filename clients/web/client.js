"use strict";

var HOST = 'http://localhost:8080';

var FLOAT_TYPES = new Set(['float', 'double', 'class java.lang.Float',
	'class java.lang.Double', 'class java.math.BigDecimal']);
var INT_TYPES = new Set(['int', 'long', 'short', 'byte',
	'class java.lang.Integer', 'class java.lang.Long', 'class java.lang.Short',
	'class java.lang.Byte', 'class java.math.BigInteger']);
var BOOL_TYPES = new Set(['boolean', 'class java.lang.Boolean']);
var STRING_TYPES = new Set(['char', 'class java.lang.Character', 'class java.lang.String']);
var DATE_TYPES = new Set(['class java.util.Date']);

function showInput(input) {
	let required = input['required'];
	let dtClass = required ? 'required' : 'optional';
	let name = input['name'];
	let label = input['label'] || name;
	let genericType = input['genericType'];
	let shortType = genericType.slice(genericType.lastIndexOf('.') + 1);
	let style = input['widgetStyle'];
	
	let div = $('<div class="input_div"></div>');
	
	div.append($(`<label class="${dtClass}">${label}</label>`));
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
	
	if (input['choices'] !== null) {
		switch(style) {
		case 'radioButtonHorizontal':
		case 'radioButtonVertical':
			let radioGroup = $('<div></div>');
			for (let choice of input['choices']) {
				let checked = (choice == input['defaultValue'] && !required) ? 'checked' : '';
				radioGroup.append($(`<label><input type="radio" name="${name}" value="${choice}" ${checked}>${choice}</label>`));
				if (style == 'radioButtonVertical') {
					radioGroup.append($('<br>'));
				}
			}
			div.append(radioGroup);
			break;
		case 'listBox':
		default:
			let selectGroup = $('<select></select>');
			if (required) {
				selectGroup.append('<option name="${name}" diabled selected style="display: none;"></option>');
			}
			for (let choice of input['choices']) {
				let selected = (!required && choice == input['defaultValue']) ? 'selected' : '';
				selectGroup.append($(`<option value="${choice}" name="${name}" ${selected}>${choice}</option>`));
			}
			div.append(selectGroup);
			break;
		}
	} else if (INT_TYPES.has(genericType) || FLOAT_TYPES.has(genericType)) {
		let min = input['minimumValue'] === null ? '' : `min="${input['minimumValue']}"`;
		let max = input['maximumValue'] === null ? '' : `max="${input['maximumValue']}"`;
		let softMin = input['softMinimum'] === null ? '' : `min="${input['softMinimum']}"`;
		let softMax= input['softMaximum'] === null ? '' : `max="${input['softMaximum']}"`;
		let step = input['stepSize'] === null ? '' : `step="${input['stepSize']}"`;
		let value = (required || input['defaultValue'] === null) ? '' : `value="${input['defaultValue']}"`;
		
		let spinner = $(`<input type="number" name="${name}" ${min} ${max} ${step} ${value}>`);

		if (style == 'slider' || style == 'scroll bar') {
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
	}
	
	$('#inputs').append(div);
}

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
		let getInput = function(name, dt_class, java_type) {
			// template for "inputs"
			return $([
				`<dt>`,
					`<span class="${dt_class}">${name}</span> <span>(${java_type})</span>`,
				`</dt>`,
				`<dd>`,
					`<span class="input active">JSON: <input class="_input_${name}" type="text" size="10"></span>`,
					`<span> or </span>`,
					`<span class="input">File: <input class="_input_${name}" type="file"></span>`,
				`</dd>`
			].join('\n'));
		};
		for (let input of data['inputs']) {
			showInput(input);
// $('#inputs').append(getInput(input['name'],
// input['required'] ? 'required' : 'optional',
// input['genericType']));
		}
		$('.input input').change(function() {
			$('.' + $(this).attr('class')).parent().removeClass('active');
			$(this).parent().addClass('active');
		});

		// outputs
		$('#outputs').empty();
		let genOutput = function(name, java_type) {
			// template for "outputs"
			return $([
				`<dt>`,
					`<span>${name}</span> <span>(${java_type})</span>`,
				`</dt>`,
				`<dd>`,
					`<span>Value: <span class="value _output_${name}"></span></span>`,
				`</dd>`,
			].join('\n'));
		}
		for (let output of data['outputs']) {
			$('#outputs').append(genOutput(output['name'], output['genericType']));
		}

		// execute
		$('form').off('submit').on('submit', function(event) {
			event.preventDefault();
			let jsonInputs = {};
			let deferreds = [];
			$('.active input').each(function(index, element) {
				let inputName = $(element).attr('class').substr('_input_'.length);
				if ($(element).attr('type') == 'file') { // upload file
					let data = new FormData();
					data.append('file', $(element).prop('files')[0]);
					deferreds.push($.ajax({
						async: false,
						url: HOST + '/objects/upload',
						type: 'POST',
						data: data,
						cache: false,
						contentType: false,
						processData: false,
						success: function(rtn, status, xhr) {
							jsonInputs[inputName] = rtn['id'];
						},
						error: function(xhr, status, err) {
							console.log(err);
						}
					}));
				} else {
					jsonInputs[inputName] = JSON.parse($(element).val());
				}
			});
			// wait for all ajax (upload file) to complete before execution
			$.when(...deferreds).then(function() {
				$.ajax({
					type: 'POST',
					url: HOST + '/modules/' + id,
					data: JSON.stringify(jsonInputs),
					success: function(outputs, status) {
						// populate outputs
						for (let name in outputs) {
							let output = outputs[name];
							let text = JSON.stringify(output, null, 2); // pretty
																		// print
																		// JSON
							let span = $(`.value._output_${name}`)
							if ((typeof output) == "string" && output.startsWith('object:')) {
								span.empty();
								let obj_url = HOST + '/objects/' + output;
								span.append($([
									`<a href="${obj_url}" target="_blank">${text}</a>`,
									`<button class="_output_${name}">View As</button>`,
									`<input class="format _output_${name}" type="text" size="5" value="png">`
								].join('\n')));
								$(`button._output_${name}`).on('click', function() {
									let clazz = $(this).attr('class');
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
				})
			});
		});
	});
}

function showModules() {
	$.getJSON(HOST + '/modules', function(data, status) {
		let optgroups = {}; // for now, we only have "command"
		for ( let module of data) {
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

window.onload = showModules;