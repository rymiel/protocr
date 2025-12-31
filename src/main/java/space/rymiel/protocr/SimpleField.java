package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

record SimpleField(FieldDescriptorProto protoField, ProtoType type, int cIdx, OneOf oneOf) implements Field {
  String defaultValue() {
    String source = protoField.hasDefaultValue() ? protoField.getDefaultValue() : null;
    return type.defaultValueFor(source);
  }

  public String name() {
    return this.protoField.getName();
  }

  public String getCrystalType() {
    return type.crystalType();
  }

  int number() {
    return this.protoField.getNumber();
  }

  private Iterable<SimpleField> oneOfSiblings() {
    if (this.oneOf == null) return List.of();
    List<SimpleField> list = new ArrayList<>();
    for (SimpleField x : this.oneOf.members()) {
      if (x != this) list.add(x);
    }
    return list;
  }

  public void generateAssignNilable(IndentedWriter content) {
    if (this.cIdx == -1) {
      content.append("@%1$s = %1$s\n".formatted(name()));
    } else {
      content.append(String.format("""
          if %1$s.nil?
            @%1$s = %2$s
          else
            @_presence.set(%3$d, true)
            @%1$s = %1$s
          end
          """, name(), defaultValue(), this.cIdx));
    }
  }

  @Override
  public void generateAssignEmpty(IndentedWriter content) {
    if (this.cIdx == -1) {
      content.append("@%s = nil\n".formatted(name()));
    } else {
      content.append("@%s = %s\n".formatted(name(), defaultValue()));
    }
  }

  @Override
  public void generateWhenFieldNumber(IndentedWriter content) {
    content.append("when %d\n".formatted(number())).indent();
    if (this.cIdx == -1) {
      // TODO: merge messages? apparently valid
      content.append("@%s = r.%s\n".formatted(name(), type().readerMethod()));
    } else {
      content.append(String.format("""
          @%2$s = r.%3$s.not_nil!
          @_presence.set(%4$d, true)
          """, number(), name(), type().readerMethod(), this.cIdx));
    }
    for (SimpleField sibling : oneOfSiblings()) {
      content.append("clear_%s!\n".formatted(sibling.name()));
    }
    content.dedent();
  }

  @Override
  public void generateWriteSerialized(IndentedWriter content) {
    content.append(String.format("""
        if has_%1$s?
          w.write_tag(%2$d, ::Protocr::WireType::%3$s)
          w.%4$s(@%1$s.not_nil!)
        end
        """, name(), number(), type().wireType(), type().writerMethod()));
  }

  @Override
  public void generateCheckEquality(IndentedWriter content) {
    if (this.cIdx == -1) {
      content.append("return false unless @%1$s == other.@%1$s\n".formatted(name()));
    } else {
      content.append("return false unless self.%1$s == other.%1$s if self.has_%1$s?\n".formatted(name()));
    }
  }

  @Override
  public void generateProperty(IndentedWriter content) {
    if (this.cIdx == -1) {
      // TODO: modifying the value returned by the getter if it was nil will not modify the message, which is likely unintuitive,
      //       but then, should the mere act of trying to access the field cause it to become the active oneof, if it's part of one?
      //       I think the spec wants me to do that, but that seems confusing too. Crystal just doesn't have a notion of mutability in that way.
      //       Of course, I can also consider changing these into structs. I guess that's sort of how the java implementation gets
      //       away with mutability stuff, by having separate classes for mutable and immutable instances, but that fits into Java
      //       and doesn't really fit into Crystal.
      content.append(String.format("""
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
          """, name(), type().crystalType(), defaultValue()));
    } else {
      content.append(String.format("""
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
          """, name(), type().crystalType(), this.cIdx, defaultValue()));
    }

    content.append("def %1$s=(value : %2$s) : Nil\n".formatted(name(), type().crystalType())).indent();
    content.append("@%1$s = value\n".formatted(name()));
    if (this.cIdx != -1) {
      content.append(String.format("@_presence.set(%1$d, true)\n", this.cIdx));
    }
    for (SimpleField sibling : oneOfSiblings()) {
      content.append("clear_%s!\n".formatted(sibling.name()));
    }
    content.dedent().append("end\n");
  }

  @Override
  public void generateParameter(IndentedWriter content) {
    content.append("%s : %s? = nil, ".formatted(name(), getCrystalType()));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SimpleField that)) return false;
    return Objects.equals(protoField, that.protoField);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(protoField);
  }
}
