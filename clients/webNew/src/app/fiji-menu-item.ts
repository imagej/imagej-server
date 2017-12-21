export class FijiMenuItem {
  public level: number;
  public label: string;
  public command: string;
  public children: FijiMenuItem[];

  constructor(level: number, label: string, command: string) {
    this.level = level;
    this.label = label;
    this.command = command;
    this.children = [];
  }

  AddChild(child: FijiMenuItem) {
    this.children.push(child);
  }
}
