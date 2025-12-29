package space.rymiel.protocr;

enum SimpleProtoType implements ProtoType {
  STRING("String", "Len", "\"\"", "read_string", "write_string"),
  BYTES("Bytes", "Len", "::Bytes.empty", "read_bytes", "write_bytes"),
  UINT_64("UInt64", "VarInt", "UInt64.zero", "read_varint_u64", "write_varint_u64");

  public final String crystalType;
  public final String wireType;
  public final String defaultEmpty;
  public final String readerMethod;
  public final String writerMethod;

  SimpleProtoType(String crystalType, String wireType, String defaultEmpty, String readerMethod, String writerMethod) {
    this.crystalType = crystalType;
    this.wireType = wireType;
    this.defaultEmpty = defaultEmpty;
    this.readerMethod = readerMethod;
    this.writerMethod = writerMethod;
  }

  @Override
  public String crystalType() {
    return this.crystalType;
  }

  @Override
  public String wireType() {
    return this.wireType;
  }

  @Override
  public String defaultEmpty() {
    return this.defaultEmpty;
  }

  @Override
  public String readerMethod() {
    return this.readerMethod;
  }

  @Override
  public String writerMethod() {
    return this.writerMethod;
  }
}
