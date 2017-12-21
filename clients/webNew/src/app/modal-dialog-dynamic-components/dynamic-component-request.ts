export interface DynamicComponentRequest {
  requestType: string;
  name: string;
  value: any;

  getProcessedValue(): any;
}
