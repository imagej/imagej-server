import { Injectable } from '@angular/core';

import { JsonService } from './json.service';
import { NotificationService } from './notification.service';

import { BsModalService } from 'ngx-bootstrap';
import { BsModalRef } from 'ngx-bootstrap/modal/bs-modal-ref.service';

import { FijiModule } from '../fiji-module';
import { FijiModuleDetails } from '../fiji-module-details';
import { FijiModuleIO } from '../fiji-module-io';
import { FijiObject } from '../fiji-object';

import { DynamicComponentRequest } from '../modal-dialog-dynamic-components/dynamic-component-request';
import { DropdownSelectionRequest } from '../modal-dialog-dynamic-components/dropdown-selection-request';
import { SpinnerSelectionRequest } from '../modal-dialog-dynamic-components/spinner-selection-request';
import { SimpleInputRequest } from '../modal-dialog-dynamic-components/simple-input-request';
import { ThumbnailDisplayRequest } from '../modal-dialog-dynamic-components/thumbnail-display-request';
import { StaticTextRequest } from '../modal-dialog-dynamic-components/static-text-request';
import { CheckboxSelectionRequest } from '../modal-dialog-dynamic-components/checkbox-selection-request';
import { FileInputRequest } from '../modal-dialog-dynamic-components/file-input-request';

import { ModalContentComponent } from '../modal-dialog-dynamic-components/modal.component';

import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/Rx';

@Injectable()
export class ModuleService {

  private modulesUrl = 'http://localhost:8080/modules';

  bsModalRef: BsModalRef;

  constructor(
    private jsonService: JsonService,
    private notificationService: NotificationService,
    private modalService: BsModalService) {  }

  fetchModules(): void {
    const availableModules: FijiModule[] = [];
    this.jsonService.getJsonData(this.modulesUrl)
      .subscribe((results: string[]) => {
        const service = this;
        results.forEach((result: string, iteration: number) => {
          availableModules.push(new FijiModule(iteration, result));
        });
        service.notificationService.modulesRetrieved(availableModules);
      });
  }

  private async fetchModuleDetails(module: FijiModule): Promise<FijiModule> {
    const details: string[] = await this.jsonService.getJsonData(this.modulesUrl + '/' + module.rawName).toPromise();
    const inputs: FijiModuleIO[] = this.parseIO(details['inputs']);
    const outputs: FijiModuleIO[] = this.parseIO(details['outputs']);
    return module.addDetails(new FijiModuleDetails((module.id), inputs, outputs));
  }

  private parseIO(inputArray: Object[]): FijiModuleIO[] {
    const outputArray: FijiModuleIO[] = [];

    inputArray.forEach(i => {
      outputArray.push(new FijiModuleIO(
        i['choices'],
        i['defaultValue'],
        i['genericType'],
        i['label'],
        i['maximumValue'],
        i['minimumValue'],
        i['name'],
        i['required'],
        i['softMaximum'],
        i['softMinimum'],
        i['stepSize'],
        i['widgetStyle']));
    });

    return outputArray;
  }

  async executeModule(module: FijiModule, activeObject: FijiObject, label: string): Promise<Observable<Object>> {

    // Get module details
    const detailedModule = await this.fetchModuleDetails(module);
    if (!detailedModule.hasDetails()) {
      alert('Module details are not available!');
      return;
    }

    // Get module inputs via a modal dialog, if needed
    if (detailedModule.details.inputs) {

      // Put together a collection of widgets for the dialog and display it
      const service = this;
      let componentRequests: DynamicComponentRequest[];
      try {
        componentRequests = this.evaluateInputs(detailedModule.details.inputs, activeObject);
      } catch (error) {
        alert(error.message);
        return;
      }

      this.bsModalRef = this.modalService.show(ModalContentComponent);
      this.bsModalRef.content.header = label;
      this.bsModalRef.content.componentRequests = componentRequests;
      return this.bsModalRef.content.onClose
        .flatMap(processedInputs => {
          return (processedInputs !== null) ?
            service.jsonService.postObject(`${this.modulesUrl}/${module.rawName}`, processedInputs) :
            Observable.empty();
        })
        .map(outputs => {
          // TODO: Handle outputs properly
          if (outputs !== null && outputs.length === 1 && outputs['dataset'] === null) { outputs = null; }
        });
    }

    // TODO: Not tested yet as all the tested modules needed some sort of inputs
    return this.jsonService.postObject(`${this.modulesUrl}/${module.rawName}`, null);
  }

  private evaluateInputs(inputs: FijiModuleIO[], activeObject: FijiObject): DynamicComponentRequest[] {
    const BOOL_TYPES = new Set(['boolean', 'class java.lang.Boolean']);
    const STRING_TYPES = new Set(['char', 'class java.lang.Character', 'class java.lang.String']);
    const FLOAT_TYPES = new Set(['float', 'double', 'class java.lang.Float',
      'class java.lang.Double', 'class java.math.BigDecimal']);
    const INT_TYPES = new Set(['int', 'long', 'short', 'byte',
      'class java.lang.Integer', 'class java.lang.Long', 'class java.lang.Short',
      'class java.lang.Byte', 'class java.math.BigInteger']);
    const FILE_TYPES = new Set(['class java.io.File']);
    const INJECTABLE_TYPES = new Set(['class org.scijava.Context', 'interface net.imagej.DatasetService',
      'interface net.imagej.display.ImageDisplayService', 'interface net.imagej.display.OverlayService',
      'interface org.scijava.command.CommandService', 'interface net.imagej.display.ZoomService',
      'interface org.scijava.display.DisplayService', 'interface org.scijava.event.EventService',
      'interface org.scijava.thread.ThreadService', 'interface org.scijava.log.LogService',
      'interface net.imagej.autoscale.AutoscaleService', 'interface org.scijava.prefs.PrefService',
      'interface org.scijava.module.ModuleService', 'interface io.scif.services.DatasetIOService',
      'interface net.imagej.types.DataTypeService', 'interface org.scijava.io.IOService',
      'interface org.scijava.ui.UIService']);
    const DATASET_TYPES = new Set(['interface net.imagej.Dataset', 'interface net.imagej.display.ImageDisplay']);

    // TODO: Make use of the server-side conversion service
    const NEEDS_CONVERSION = new Set(['interface net.imagej.display.DatasetView']);

    const componentRequests: DynamicComponentRequest[] = [];
    inputs.forEach(i => {
      const label = i.label || i.name;
      if (i.choices !== null) {
        switch (i.widgetStyle) {
          case 'listBox':
          default: {
            const defaultValue = i.defaultValue === null ? '' : i.defaultValue;
            componentRequests.push(new DropdownSelectionRequest(i.name, label, defaultValue, i.choices));
            break;
          }
        }
      } else if (INT_TYPES.has(i.genericType) || FLOAT_TYPES.has(i.genericType)) {
        const minimumValue = i.minimumValue === null ? '' : i.minimumValue;
        const maximumValue = i.maximumValue === null ? '' : i.maximumValue;
        const softMinimum = i.softMinimum === null ? '' : i.softMinimum;
        const softMaximum = i.softMaximum === null ? '' : i.softMaximum;
        const defaultStep = INT_TYPES.has(i.genericType) ? '1' : 'any';
        const stepSize = i.stepSize === null ? defaultStep : i.stepSize;
        const defaultValue = (i.required || i.defaultValue === null) ? 0 : <number> i.defaultValue;
        componentRequests.push(new SpinnerSelectionRequest(
          i.name, label, minimumValue, maximumValue, stepSize, defaultValue
        ));
      } else if (BOOL_TYPES.has(i.genericType)) {
        const defaultValue = (i.required || i.defaultValue === null) ? false : i.defaultValue;
        componentRequests.push(new CheckboxSelectionRequest(i.name, label, defaultValue));
      } else if (STRING_TYPES.has(i.genericType)) {
        const defaultValue = i.defaultValue === null ? null : i.defaultValue;
        componentRequests.push(new SimpleInputRequest(i.name, label, defaultValue));
      } else if (FILE_TYPES.has(i.genericType)) {
        componentRequests.push(new FileInputRequest(i.name, label, null));
      } else if (INJECTABLE_TYPES.has(i.genericType)) {
        componentRequests.push(new SimpleInputRequest(i.name, label, null, true));
      } else if (DATASET_TYPES.has(i.genericType)) {
        // We need to have an active object, if we don't have it, return null as error
        if (!activeObject) {
          throw(new Error('No active image selected!'));
        }
        componentRequests.push(new ThumbnailDisplayRequest(i.name, label, activeObject.src, activeObject.id));
      } else {
        componentRequests.push(new StaticTextRequest(
          'Not implemented input widget for class: ' + i.genericType));
      }
    });
    return componentRequests;
  }
}
