import { Component, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal/bs-modal-ref.service';
import { DynamicComponentRequest } from './dynamic-component-request';
import { Subject } from 'rxjs/Subject';

@Component({
  selector: 'app-modal-content',
  styleUrls: ['./modal.component.css'],
  templateUrl: './modal.component.html'
})
export class ModalContentComponent implements OnInit {
  header: string;
  componentRequests: DynamicComponentRequest[];
  public onClose: Subject<Object>;

  constructor(public bsModalRef: BsModalRef) {}

  ngOnInit(): void {
    this.onClose = new Subject();
  }

  confirm(): void {
    const processedInputs = new Object();
    this.componentRequests.forEach(cr => { processedInputs[cr.name] = cr.getProcessedValue(); });
    this.onClose.next(processedInputs);
    this.bsModalRef.hide();
  }

  cancel(): void {
    this.onClose.next(null);
    this.bsModalRef.hide();
  }

  setFileAsValue(event) {
    const correspondingRequest = this.componentRequests.find(cr => cr.name === event.currentTarget.name);
    if (correspondingRequest !== null) {
      correspondingRequest.value = event.target.files[0];
    }
  }
}
