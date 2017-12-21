export class FijiObject {
  public id: string;
  public src: string;

  constructor(objectUrl: string, id: string, timestamp: number) {
    this.id = id;
    this.src = `${objectUrl}/${id}/jpg?timestamp=${timestamp}`;
  }
}
