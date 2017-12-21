import { DynamicComponentRequest } from './dynamic-component-request';

export class CheckboxSelectionRequest implements DynamicComponentRequest {
  public readonly requestType = CheckboxSelectionRequest.name;
  public name: string;
  public label: string;
  public value: string;

  constructor(name: string, label: string, value: string) {
    this.name = name;
    this.label = label;
    this.value = value;
  }

  getProcessedValue(): string {
    return this.value;
  }
}
