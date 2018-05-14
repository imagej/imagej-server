import { DynamicComponentRequest } from './dynamic-component-request';

export class FileInputRequest implements DynamicComponentRequest {
  public readonly requestType = FileInputRequest.name;
  public name: string;
  public label: string;
  public value: File;

  constructor(name: string, label: string, value: File) {
    this.name = name;
    this.label = label;
    this.value = value;
  }

  getProcessedValue(): File {
    return this.value;
  }
}
