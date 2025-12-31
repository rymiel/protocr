package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import java.util.List;

record Field(FieldDescriptorProto protoField, ProtoType type, int cIdx, List<Field> oneOfSiblings) {
  public String defaultValue() {
    String source = protoField.hasDefaultValue() ? protoField().getDefaultValue() : null;
    return type.defaultValueFor(source);
  }

  public String name() {
    return this.protoField.getName();
  }

  public int number() {
    return this.protoField.getNumber();
  }
}
