export class FijiModuleIO {
  constructor(
    public choices: string[],
    public defaultValue: any,
    public genericType: string,
    public label: string,
    public maximumValue: string,
    public minimumValue: string,
    public name: string,
    public required: boolean,
    public softMaximum: string,
    public softMinimum: string,
    public stepSize: string,
    public widgetStyle: string) {
  }
}
