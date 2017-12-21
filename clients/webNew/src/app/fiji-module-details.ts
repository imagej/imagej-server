import { FijiModuleIO } from './fiji-module-io';

export class FijiModuleDetails {
  public inputs: FijiModuleIO[];
  public outputs: Object[];

  constructor(id: number, inputs: FijiModuleIO[], outputs: Object[]) {
    this.inputs = inputs;
    this.outputs = outputs;
  }
}
