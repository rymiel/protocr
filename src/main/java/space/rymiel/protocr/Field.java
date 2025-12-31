package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import java.util.List;
import java.util.Locale;

record Field(FieldDescriptorProto protoField, ProtoType type, int cIdx, List<Field> oneOfSiblings) {
  public String defaultName() {
    return this.name().toUpperCase(Locale.ROOT) + "_DEFAULT";
  }

  public String name() {
    return this.protoField.getName();
  }

  public int number() {
    return this.protoField.getNumber();
  }

  public String generateDefaultValue() {
    String source = protoField.hasDefaultValue() ? protoField().getDefaultValue() : null;
    return type.defaultValueFor(source);
  }
}
