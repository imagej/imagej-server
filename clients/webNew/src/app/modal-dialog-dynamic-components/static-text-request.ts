import { DynamicComponentRequest } from './dynamic-component-request';

export class StaticTextRequest implements DynamicComponentRequest {
  public readonly requestType = StaticTextRequest.name;
  public name: string;
  public value: string;
  public text: string;

  constructor(text: string) {
    this.name = text;
    this.value = text;
    this.text = text;
  }

  getProcessedValue(): string {
    return this.value;
  }
}
