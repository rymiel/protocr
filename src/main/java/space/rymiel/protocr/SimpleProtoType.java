package space.rymiel.protocr;

import java.util.Objects;

record SimpleProtoType(String crystalType, String wireType, ValueTransformer transformer, String readerMethod,
                       String writerMethod, boolean presence) implements ProtoType {
  private static final ValueTransformer STRING_VALUE = (s) -> s == null ? "\"\"" : StringUtil.stringify(s);
  private static final ValueTransformer BYTES_VALUE = (s) -> s == null ? "::Bytes.empty" : StringUtil.stringify(s) + ".to_slice";
  private static final ValueTransformer UINT_64_VALUE = new IntSuffixTransformer("u64");
  private static final ValueTransformer UINT_32_VALUE = new IntSuffixTransformer("u32");

  public static SimpleProtoType STRING = new SimpleProtoType("String", "Len", STRING_VALUE, "read_string", "write_string", false);
  public static SimpleProtoType BYTES = new SimpleProtoType("Bytes", "Len", BYTES_VALUE, "read_bytes", "write_bytes", true);
  public static SimpleProtoType UINT_64 = new SimpleProtoType("UInt64", "VarInt", UINT_64_VALUE, "read_varint_u64", "write_varint_u64", true);
  public static SimpleProtoType UINT_32 = new SimpleProtoType("UInt32", "VarInt", UINT_32_VALUE, "read_varint_u32", "write_varint_u32", true);

  @Override
  public String defaultValueFor(String value) {
    return this.transformer.transform(value);
  }

  @FunctionalInterface
  public interface ValueTransformer {
    String transform(String def);
  }

  private record IntSuffixTransformer(String suffix) implements ValueTransformer {
    @Override
    public String transform(String def) {
      return Objects.requireNonNullElse(def, "0") + suffix;
    }
  }
}
