package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

import java.util.List;

public final class MessageGenerator extends Generator {
  private final DescriptorProtos.DescriptorProto message;
  private final List<Field> fields;

  MessageGenerator(IndentedWriter content, DescriptorProtos.DescriptorProto message) {
    super(content);
    this.message = message;
    this.fields = message.getFieldList().stream().map(Field::new).toList();
  }

  private void generateCanonicalConstructor() {
    append("def initialize(");
    for (var field : this.fields) {
      append("@%s = nil, ".formatted(field.name()));
    }
    append(")\nend\n\n");
  }

  private void generateDeserializeConstructor() {
    append("def initialize(r : ::Protocr::Reader)\n").indent();
    for (var field : this.fields) {
      append("@%s = nil\n".formatted(field.name()));
    }
    append("loop do\n").indent();
    append("field, wire_type = r.read_tag\n");
    append("case field\n");
    append("when 0 then break\n");
    for (var field : this.fields) {
      append("when %d then @%s = r.%s\n".formatted(field.number(), field.name(), field.type().readerMethod()));
    }
    append("else r.skip wire_type\n");
    append("end\n").dedent().append("end\n");
    dedent().append("end\n");

    append("""
        def self.from_protobuf(io : ::IO)
          self.new(::Protocr::Reader.new io)
        end
        """);
  }

  private void generateSerializer() {
    append("def to_protobuf(io : ::IO)\n").indent();
    append("w = ::Protocr::Writer.new io\n");
    for (var field : this.fields) {
      append("""
          if !(v = @%1$s).nil?
            w.write_tag(%2$d, ::Protocr::WireType::%3$s)
            w.%4$s(v)
          end
          """.formatted(field.name(), field.number(), field.type().wireType(), field.type().writerMethod()));
    }
    dedent().append("end\n");

    append("""
        def to_protobuf : Bytes
          io = ::IO::Memory.new
          self.to_protobuf(io)
          io.to_slice
        end
        """);
  }

  private void generateProperty(Field field) {
    append(("""
        @%1$s : %2$s?
        
        def %1$s : %2$s
          @%1$s.nil? ? %3$s : @%1$s.not_nil!
        end
        def has_%s? : Bool
          !@%1$s.nil?
        end
        def %1$s=(value : %2$s) : Nil
          @%1$s = value
        end
        
        """).formatted(field.name(), field.type().crystalType(), field.type().defaultEmpty()));
  }

  public void run() {
    append("class ").append(this.message.getName()).append("\n").indent();

    for (var field : this.fields) {
      generateProperty(field);
    }

    generateCanonicalConstructor();
    generateDeserializeConstructor();
    generateSerializer();

    dedent().append("end\n\n");
  }
}
