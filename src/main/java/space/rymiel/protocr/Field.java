package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

record Field(String name, int number, ProtoType type) {
  Field(DescriptorProtos.FieldDescriptorProto field) {
    this(field.getName(), field.getNumber(), ProtoType.of(field));
    // need a whole bunch more unsupported operation exceptions lmao
    if (field.getLabel() != DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
      throw new UnsupportedOperationException(field.toString());
    }
  }
}
