import { DynamicComponentRequest } from './dynamic-component-request';

export class ThumbnailDisplayRequest implements DynamicComponentRequest {
  public readonly requestType = ThumbnailDisplayRequest.name;
  public name: string;
  public label: string;
  public imageSrc: string;
  public value: string;

  constructor(name: string, label: string, imageSrc: string, value: string) {
    this.name = name;
    this.label = label;
    this.imageSrc = imageSrc;
    this.value = value;
  }

  getProcessedValue(): string {
    return this.value;
  }
}
