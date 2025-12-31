package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MessageGenerator extends Generator {
  private final DescriptorProtos.DescriptorProto message;
  private final List<Field> fields;
  private final List<OneOf> oneOfs;
  private final int presenceByteSize;

  MessageGenerator(IndentedWriter content, DescriptorProtos.DescriptorProto message) {
    super(content);
    this.message = message;

    List<OneOf> oneOfs = new ArrayList<>();
    for (var o : message.getOneofDeclList()) {
      oneOfs.add(new OneOf(o.getName(), new ArrayList<>()));
    }

    List<Field> fields = new ArrayList<>();
    int compactableCount = 0;
    for (var fp : message.getFieldList()) {
      // need a whole bunch more unsupported operation exceptions lmao
      if (fp.getLabel() != DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
        throw new UnsupportedOperationException(fp.toString());
      }

      ProtoType type = ProtoType.of(fp);
      int cIdx = type.compactable() ? compactableCount++ : -1;
      OneOf oneOf = null;
      if (!fp.getProto3Optional() && fp.hasOneofIndex()) {
        oneOf = oneOfs.get(fp.getOneofIndex());
      }

      SimpleField field = new SimpleField(fp, type, cIdx, oneOf);

      fields.add(field);
      if (oneOf != null) oneOf.members().add(field);
    }

    // Remove empty oneofs. This removes synthetic oneofs
    oneOfs.removeIf(o -> o.members().isEmpty());

    // Find combinable oneofs
//    for (Iterator<OneOf> iterator = oneOfs.iterator(); iterator.hasNext(); ) {
//      var o = iterator.next();
//      if (o.members().stream().map(Field::type).allMatch(MessageProtoType.class::isInstance)) {
//        fields.removeAll(o.members());
//        iterator.remove();
//      }
//    }

    this.fields = List.copyOf(fields);
    this.oneOfs = List.copyOf(oneOfs);

    this.presenceByteSize = (compactableCount + 7) / 8;
  }

  private void generateCanonicalConstructor() {
    append("def initialize(");
    for (var field : this.fields) {
      append("%s : %s? = nil, ".formatted(field.name(), field.type().crystalType()));
    }
    append(")\n").indent();
    if (presenceByteSize != 0) {
      append("@_presence = ::Protocr::StaticBitset(%d).new\n".formatted(presenceByteSize));
    }
    for (var field : this.fields) {
      field.generateAssignNilable(this.content);
    }
    dedent().append("end\n\n");
  }

  private void generateDeserializeConstructor() {
    append("def initialize(r : ::Protocr::Reader)\n").indent();
    for (var field : this.fields) {
      field.generateAssignEmpty(this.content);
    }
    if (this.presenceByteSize != 0) {
      append("@_presence = ::Protocr::StaticBitset(%d).new\n".formatted(presenceByteSize));
    }
    append("loop do\n").indent();
    append("field, wire_type = r.read_tag\n");
    append("case field\n");
    append("when 0 then break\n");
    for (var field : this.fields) {
      field.generateWhenFieldNumber(this.content);
    }
    append("else r.skip wire_type\n");
    append("end\n").dedent().append("end\n");
    dedent().append("end\n");

    append("""
        def self.from_protobuf(io : ::IO)
          self.new(::Protocr::Reader.new io.getb_to_end)
        end
        def self.from_protobuf(bytes : ::Bytes)
          self.new(::Protocr::Reader.new bytes)
        end
        """);
  }

  private void generateSerializer() {
    append("def to_protobuf(io : ::IO)\n").indent();
    append("w = ::Protocr::Writer.new io\n");
    for (var field : this.fields) {
      field.generateWriteSerialized(this.content);
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

  private void generateEquality() {
    append("def ==(other : self)\n").indent();
    append("return true if same?(other)\n");
    for (var field : this.fields) {
      field.generateCheckEquality(this.content);
    }
    append("return true\n");
    dedent().append("end\n");
  }

  private void generateOneOfGetter(OneOf oneOf) {
    append("def %s : ::Union(".formatted(oneOf.name()));
    for (Field field : oneOf.members()) {
      append(field.type().crystalType()).append(", ");
    }
    append("Nil)\n").indent();
    append("case\n");
    for (Field field : oneOf.members()) {
      append("when has_%1$s? then %1$s\n".formatted(field.name()));
    }
    append("else nil\nend\n");
    dedent().append("end\n");
  }

  private void generatePresence() {
    if (this.presenceByteSize != 0)
      append("@_presence : ::Protocr::StaticBitset(%1$d)\n".formatted(this.presenceByteSize));
  }

  public void run() {
    append("class ").append(this.message.getName()).append("\n").indent();

    generatePresence();

    for (var field : this.fields) {
      field.generateProperty(this.content);
    }

    for (var oneOf : this.oneOfs) {
      generateOneOfGetter(oneOf);
    }

    generateCanonicalConstructor();
    generateDeserializeConstructor();
    generateSerializer();
    generateEquality();

    dedent().append("end\n\n");
  }
}
