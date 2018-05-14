import { FijiModuleDetails } from './fiji-module-details';

export class FijiModule {
  public id: number;
  public type: string;
  public clazz: string;
  public source: string;
  public rawName: string;
  public details: FijiModuleDetails;

  constructor(id: number, rawResult: string) {
    const firstColon = rawResult.indexOf(':');
    const lastDot = rawResult.lastIndexOf('.');

    this.id = id;
    this.type = rawResult.slice(0, firstColon);
    this.clazz = rawResult.slice(lastDot + 1);
    this.source = rawResult.slice(firstColon + 1, lastDot);
    this.rawName = rawResult;
  }

  addDetails(details: FijiModuleDetails): FijiModule {
    this.details = details;
    return this;
  }

  hasDetails(): boolean {
    return this.details !== null;
  }
}
