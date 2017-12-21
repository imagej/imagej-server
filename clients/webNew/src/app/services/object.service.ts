import { Injectable } from '@angular/core';

import { JsonService } from './json.service';
import { NotificationService } from './notification.service';

import { FijiObject } from '../fiji-object';

@Injectable()
export class ObjectService {

  private objectsUrl = 'http://localhost:8080/objects';
  private objectsUrlUpload = 'http://localhost:8080/objects/upload';

  constructor(
    private jsonService: JsonService,
    private notificationService: NotificationService) {  }

  uploadObject(file: File): void {
    const formData = new FormData();
    formData.append('file', file);
    this.jsonService.postFormData(this.objectsUrlUpload, formData)
      .subscribe(null, null, () => this.fetchObjects());
  }

  fetchObjects(): void {
    const uploadedObjects: FijiObject[] = [];
    const timestamp = new Date().getTime();
    this.jsonService.getJsonData(this.objectsUrl)
      .subscribe((results: string[]) => {
        const service = this;
        results.forEach((result: string) => {
          uploadedObjects.push(new FijiObject(service.objectsUrl, result, timestamp));
        });
        service.notificationService.objectsRetrieved(uploadedObjects);
      });
  }
}
