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
    for (var fp : message.getFieldList()) {
      // need a whole bunch more unsupported operation exceptions lmao
      if (fp.getLabel() != DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
        throw new UnsupportedOperationException(fp.toString());
      }

      ProtoType type = ProtoType.of(fp);
      int cIdx = type.compactable() ? compactableCount++ : -1;

      // I want the array to be immutable but idk how to initialize everything nicely :(
      Field field = new Field(fp, type, cIdx, new ArrayList<>());

      if (!fp.getProto3Optional() && fp.hasOneofIndex()) {
        var siblings = oneOfMembers.get(fp.getOneofIndex());
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
      if (oneOfMembers.get(i).isEmpty()) continue;
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
            """, field.name(), field.defaultName(), field.cIdx()));
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
        append("@%s = %s\n".formatted(field.name(), field.defaultName()));
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
      for (Field sibling : field.oneOfSiblings()) {
        append("clear_%s!\n".formatted(sibling.name()));
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
      append(String.format("""
          if has_%1$s?
            w.write_tag(%2$d, ::Protocr::WireType::%3$s)
            w.%4$s(@%1$s.not_nil!)
          end
          """, field.name(), field.number(), field.type().wireType(), field.type().writerMethod()));
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
      if (field.cIdx() != -1) {
        append("return false unless @%1$s == other.@%1$s\n".formatted(field.name()));
      } else {
        // TODO: can technically be more efficient: if has_value? is false for both, no need to compare the actual values
        append("return false unless self.has_%1$s? == other.has_%1$s?\n".formatted(field.name()));
        append("return false unless self.%1$s == other.%1$s\n".formatted(field.name()));
      }
    }
    append("return true\n");
    dedent().append("end\n");
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
          def clear_%1$s! : Nil
            @%1$s = nil
          end
          """, field.name(), field.type().crystalType(), field.defaultName()));
    } else {
      append(String.format("""
          @%1$s : %2$s
          
          def %1$s : %2$s
            @%1$s
          end
          def has_%s? : Bool
            @_presence.test(%3$d)
          end
          def clear_%1$s! : Nil
            @%1$s = %4$s
            @_presence.set(%3$d, false)
          end
          """, field.name(), field.type().crystalType(), field.cIdx(), field.defaultName()));
    }

    append("def %1$s=(value : %2$s) : Nil\n".formatted(field.name(), field.type().crystalType())).indent();
    append("@%1$s = value\n".formatted(field.name()));
    if (field.cIdx() != -1) {
      append(String.format("@_presence.set(%1$d, true)\n", field.cIdx()));
    }
    for (Field sibling : field.oneOfSiblings()) {
      append("clear_%s!\n".formatted(sibling.name()));
    }
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

  private void generateDefault(Field field) {
    append("%1$s = %2$s\n".formatted(field.defaultName(), field.generateDefaultValue()));
  }

  public void run() {
    append("class ").append(this.message.getName()).append("\n").indent();

    for (var field : this.fields) {
      generateDefault(field);
    }
    generatePresence();

    for (var field : this.fields) {
      generateProperty(field);
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
