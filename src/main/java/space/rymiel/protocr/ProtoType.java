package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

sealed interface ProtoType permits MessageProtoType, SimpleProtoType {
  String crystalType();

  String wireType();

  String defaultEmpty();

  String readerMethod();

  String writerMethod();

  static ProtoType of(DescriptorProtos.FieldDescriptorProto field) {
    return switch (field.getType()) {
      case TYPE_DOUBLE -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_FLOAT -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_INT64 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_UINT64 -> SimpleProtoType.UINT_64;
      case TYPE_INT32 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_FIXED64 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_FIXED32 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_BOOL -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_STRING -> SimpleProtoType.STRING;
      case TYPE_GROUP -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_MESSAGE -> new MessageProtoType(field);
      case TYPE_BYTES -> SimpleProtoType.BYTES;
      case TYPE_UINT32 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_ENUM -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_SFIXED32 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_SFIXED64 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_SINT32 -> throw new UnsupportedOperationException(field.getType().toString());
      case TYPE_SINT64 -> throw new UnsupportedOperationException(field.getType().toString());
    };
  }
}
