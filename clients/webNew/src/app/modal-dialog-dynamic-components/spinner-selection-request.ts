import { DynamicComponentRequest } from './dynamic-component-request';

export class SpinnerSelectionRequest implements DynamicComponentRequest {
  public readonly requestType = SpinnerSelectionRequest.name;
  public name: string;
  public label: string;
  public minimumValue: string;
  public maximumValue: string;
  public stepSize: string;
  public value: number;

  constructor(name: string, label: string, minimumValue: string, maximumValue: string, stepSize: string, value: number) {
    this.name = name;
    this.label = label;
    this.minimumValue = minimumValue;
    this.maximumValue = maximumValue;
    this.stepSize = stepSize;
    this.value = value;
  }

  getProcessedValue(): number {
    return this.value;
  }
}
