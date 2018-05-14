export class FijiMenuItem {
  public label: string;
  public command: string;
  public children: FijiMenuItem[];

  constructor(label: string, command: string) {
    this.label = label;
    this.command = command;
    this.children = [];
  }

  AddChild(child: FijiMenuItem) {
    this.children.push(child);
  }
}
