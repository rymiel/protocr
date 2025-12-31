package space.rymiel.protocr;

import java.util.List;

record Field(String name, int number, String defaultValue, ProtoType type, int cIdx, List<Field> oneOfSiblings) {
  public String defaultValue() {
    return this.type.defaultValueFor(this.defaultValue);
  }
}
