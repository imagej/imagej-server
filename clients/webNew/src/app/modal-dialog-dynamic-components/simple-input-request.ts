import { DynamicComponentRequest } from './dynamic-component-request';

export class SimpleInputRequest implements DynamicComponentRequest {
  public readonly requestType = SimpleInputRequest.name;
  public name: string;
  public label: string;
  public value: string;
  public disabled: boolean;

  constructor(name: string, label: string, value: string, disabled: boolean = false) {
    this.name = name;
    this.label = label;
    this.value = value;
    this.disabled = disabled;
  }

  getProcessedValue(): string {
    return this.value;
  }
}
