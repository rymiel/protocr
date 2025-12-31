package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

public record MessageProtoType(DescriptorProtos.FieldDescriptorProto field) implements ProtoType {
  @Override
  public String crystalType() {
    return StringUtil.crystalTypeName(field.getTypeName());
  }

  @Override
  public String wireType() {
    return "Len";
  }

  @Override
  public String readerMethod() {
    return "len_subreader(%s)".formatted(crystalType());
  }

  @Override
  public String writerMethod() {
    return "write_message";
  }

  @Override
  public boolean compactable() {
    return false;
  }

  @Override
  public String defaultValueFor(String value) {
    if (value != null) throw new UnsupportedOperationException("Message types cannot be defaulted");
    return crystalType() + ".new()";
  }
}
