import { DynamicComponentRequest } from './dynamic-component-request';

export class DropdownSelectionRequest implements DynamicComponentRequest {
  public readonly requestType = DropdownSelectionRequest.name;
  public name: string;
  public label: string;
  public value: string;
  public choices: string[];

  constructor(name: string, label: string, value: string, choices: string[]) {
    this.name = name;
    this.label = label;
    this.value = value;
    this.choices = choices;
  }

  getProcessedValue(): string {
    return this.value;
  }
}
