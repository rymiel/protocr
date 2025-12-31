package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    int presenceBits = 0;
    for (var fp : message.getFieldList()) {
      // need a whole bunch more unsupported operation exceptions lmao
      if (fp.getLabel() != DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
        throw new UnsupportedOperationException(fp.toString());
      }

      ProtoType type = ProtoType.of(fp);
      OneOf oneOf = null;
      if (!fp.getProto3Optional() && fp.hasOneofIndex()) {
        oneOf = oneOfs.get(fp.getOneofIndex());
      }

      SimpleField field = type.presence() ? new PresenceField(fp, type, oneOf, presenceBits++) : new SimpleField(fp, type, oneOf);

      fields.add(field);
      if (oneOf != null) oneOf.members().add(field);
    }

    // Remove empty oneofs. This removes synthetic oneofs
    oneOfs.removeIf(o -> o.members().isEmpty());

    // Find combinable oneofs: all are classes and unique
    for (Iterator<OneOf> iterator = oneOfs.iterator(); iterator.hasNext(); ) {
      var o = iterator.next();
      boolean canCombine = true;
      Set<String> usedTypes = new HashSet<>();
      for (SimpleField simpleField : o.members()) {
        ProtoType type = simpleField.type();
        if (!(type instanceof MessageProtoType) || usedTypes.contains(type.crystalType())) {
          canCombine = false;
          break;
        }
        usedTypes.add(type.crystalType());
      }
      if (canCombine) {
        fields.removeAll(o.members());
        fields.add(new UnionField(o.name(), o.members()));
        iterator.remove();
      }
    }

    this.fields = List.copyOf(fields);
    this.oneOfs = List.copyOf(oneOfs);

    this.presenceByteSize = (presenceBits + 7) / 8;
  }

  private void generateCanonicalConstructor() {
    append("def initialize(");
    for (var field : this.fields) {
      field.generateParameter(this.content);
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
    for (SimpleField field : oneOf.members()) {
      append(field.getCrystalType()).append(", ");
    }
    append("Nil)\n").indent();
    append("case\n");
    for (SimpleField field : oneOf.members()) {
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
