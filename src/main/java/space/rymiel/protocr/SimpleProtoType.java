package space.rymiel.protocr;

record SimpleProtoType(String crystalType, String wireType, String defaultEmpty, String readerMethod,
                       String writerMethod, boolean compactable) implements ProtoType {
  public static SimpleProtoType STRING = new SimpleProtoType("String", "Len", "\"\"", "read_string", "write_string", false);
  public static SimpleProtoType BYTES = new SimpleProtoType("Bytes", "Len", "::Bytes.empty", "read_bytes", "write_bytes", true);
  public static SimpleProtoType UINT_64 = new SimpleProtoType("UInt64", "VarInt", "UInt64.zero", "read_varint_u64", "write_varint_u64", true);
  public static SimpleProtoType UINT_32 = new SimpleProtoType("UInt32", "VarInt", "UInt32.zero", "read_varint_u32", "write_varint_u32", true);
}
