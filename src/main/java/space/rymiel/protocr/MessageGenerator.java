package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

import java.util.ArrayList;
import java.util.List;

public final class MessageGenerator extends Generator {
  private final DescriptorProtos.DescriptorProto message;
  private final List<Field> fields;
  private final List<OneOf> oneOfs;
  private final int presenceByteSize;

  MessageGenerator(IndentedWriter content, DescriptorProtos.DescriptorProto message) {
    super(content);
    this.message = message;

    var oneOfMembers = new ArrayList<List<Field>>();
    for (int i = 0; i < message.getOneofDeclCount(); i++) {
      oneOfMembers.add(new ArrayList<>());
    }

    List<Field> fields = new ArrayList<>();
    int compactableCount = 0;
    for (var fieldProto : message.getFieldList()) {
      // need a whole bunch more unsupported operation exceptions lmao
      if (fieldProto.getLabel() != DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
        throw new UnsupportedOperationException(fieldProto.toString());
      }

      ProtoType type = ProtoType.of(fieldProto);
      int cIdx = type.compactable() ? compactableCount++ : -1;
      // I want the array to be immutable but idk how to initialize everything nicely :(
      Field field = new Field(fieldProto.getName(), fieldProto.getNumber(), type, cIdx, new ArrayList<>());
      if (!fieldProto.getProto3Optional() && fieldProto.hasOneofIndex()) {
        var siblings = oneOfMembers.get(fieldProto.getOneofIndex());
        field.oneOfSiblings().addAll(siblings);
        for (Field sibling : siblings) {
          sibling.oneOfSiblings().add(field);
        }
        siblings.add(field);
      }
      fields.add(field);
    }
    this.fields = List.copyOf(fields);

    List<OneOf> oneOfs = new ArrayList<>();
    for (int i = 0; i < oneOfMembers.size(); i++) {
      oneOfs.add(new OneOf(message.getOneofDecl(i).getName(), List.copyOf(oneOfMembers.get(i))));
    }
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
      if (field.cIdx() == -1) {
        append("@%1$s = %1$s\n".formatted(field.name()));
      } else {
        append(String.format("""
            if %1$s.nil?
              @%1$s = %2$s
            else
              @_presence.set(%3$d, true)
              @%1$s = %1$s
            end
            """, field.name(), field.type().defaultEmpty(), field.cIdx()));
      }
    }
    dedent().append("end\n\n");
  }

  private void generateDeserializeConstructor() {
    append("def initialize(r : ::Protocr::Reader)\n").indent();
    for (var field : this.fields) {
      if (field.cIdx() == -1) {
        append("@%s = nil\n".formatted(field.name()));
      } else {
        append("@%s = %s\n".formatted(field.name(), field.type().defaultEmpty()));
      }
    }
    if (this.presenceByteSize != 0) {
      append("@_presence = ::Protocr::StaticBitset(%d).new\n".formatted(presenceByteSize));
    }
    append("loop do\n").indent();
    append("field, wire_type = r.read_tag\n");
    append("case field\n");
    append("when 0 then break\n");
    for (var field : this.fields) {
      append("when %d\n".formatted(field.number())).indent();
      if (field.cIdx() == -1) {
        // TODO: merge messages? apparently valid
        append("@%s = r.%s\n".formatted(field.name(), field.type().readerMethod()));
      } else {
        append(String.format("""
            @%2$s = r.%3$s.not_nil!
            @_presence.set(%4$d, true)
            """, field.number(), field.name(), field.type().readerMethod(), field.cIdx()));
      }
      for (var sibling : field.oneOfSiblings()) {
        if (sibling == field) continue;
        generateClear(sibling);
      }
      dedent();
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
      if (field.cIdx() == -1) {
        append(String.format("""
            if !(v = @%1$s).nil?
              w.write_tag(%2$d, ::Protocr::WireType::%3$s)
              w.%4$s(v)
            end
            """, field.name(), field.number(), field.type().wireType(), field.type().writerMethod()));
      } else {
        append(String.format("""
            if @_presence.test(%5$d)
              w.write_tag(%2$d, ::Protocr::WireType::%3$s)
              w.%4$s(@%1$s)
            end
            """, field.name(), field.number(), field.type().wireType(), field.type().writerMethod(), field.cIdx()));
      }
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
    append("def_equals_and_hash ");
    for (var field : this.fields) {
      // TODO: can technically be more efficient: if has_value? is false for both, no need to compare the actual values
      append("has_%1$s?, %1$s".formatted(field.name()));
    }
    append("\n");
  }

  private void generateClear(Field field) {
    if (field.cIdx() == -1) {
      append("@%s = nil\n".formatted(field.name()));
    } else {
      append("@_presence.set(%d, false)\n".formatted(field.cIdx()));
    }
  }

  private void generateProperty(Field field) {
    if (field.cIdx() == -1) {
      // TODO: modifying the value returned by the getter if it was nil will not modify the message, which is likely unintuitive,
      //       but then, should the mere act of trying to access the field cause it to become the active oneof, if it's part of one?
      //       I think the spec wants me to do that, but that seems confusing too. Crystal just doesn't have a notion of mutability in that way.
      //       Of course, I can also consider changing these into structs. I guess that's sort of how the java implementation gets
      //       away with mutability stuff, by having separate classes for mutable and immutable instances, but that fits into Java
      //       and doesn't really fit into Crystal.
      append(String.format("""
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
          """, field.name(), field.type().crystalType(), field.type().defaultEmpty()));
    } else {
      append(String.format("""
          @%1$s : %2$s
          
          def %1$s : %2$s
            @%1$s
          end
          def has_%s? : Bool
            @_presence.test(%3$d)
          end
          def %1$s=(value : %2$s) : Nil
            @%1$s = value
            @_presence.set(%3$d, true)
          end
          """, field.name(), field.type().crystalType(), field.cIdx()));
    }

    append("def clear_%1$s! : Nil\n".formatted(field.name())).indent();
    generateClear(field);
    dedent().append("end\n\n");
  }

  private void generatePresence() {
    if (this.presenceByteSize != 0)
      append("@_presence : ::Protocr::StaticBitset(%1$d)\n".formatted(this.presenceByteSize));
  }

  public void run() {
    append("class ").append(this.message.getName()).append("\n").indent();

    generatePresence();

    for (var field : this.fields) {
      generateProperty(field);
    }

    generateCanonicalConstructor();
    generateDeserializeConstructor();
    generateSerializer();
    generateEquality();

    dedent().append("end\n\n");
  }
}
