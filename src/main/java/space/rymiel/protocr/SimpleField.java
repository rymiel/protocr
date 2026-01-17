package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class SimpleField implements Field {
  private final FieldDescriptorProto protoField;
  protected final ProtoType type;
  private final OneOf oneOf;

  SimpleField(FieldDescriptorProto protoField, ProtoType type, OneOf oneOf) {
    this.protoField = protoField;
    this.type = type;
    this.oneOf = oneOf;
  }

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

  ProtoType type() {
    return type;
  }

  protected Iterable<SimpleField> oneOfSiblings() {
    if (this.oneOf == null) return List.of();
    List<SimpleField> list = new ArrayList<>();
    for (SimpleField x : this.oneOf.members()) {
      if (x != this) list.add(x);
    }
    return list;
  }

  public void generateAssignNilable(IndentedWriter content) {
    content.append("@%1$s = %1$s\n".formatted(name()));
  }

  @Override
  public void generateAssignEmpty(IndentedWriter content) {
    content.append("@%s = nil\n".formatted(name()));
  }

  @Override
  public void generateWhenFieldNumber(IndentedWriter content) {
    content.append("when %d\n".formatted(number())).indent();
    // TODO: merge messages? apparently valid
    content.append("@%s = r.%s\n".formatted(name(), type.readerMethod()));
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
        """, name(), number(), type.wireType(), type.writerMethod()));
  }

  @Override
  public void generateCheckEquality(IndentedWriter content) {
    content.append("return false unless @%1$s == other.@%1$s\n".formatted(name()));
  }

  @Override
  public void generateProperty(IndentedWriter content) {
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
        """, name(), type.crystalType(), defaultValue()));

    content.append("def %1$s=(value : %2$s) : Nil\n".formatted(name(), type.crystalType())).indent();
    content.append("@%1$s = value\n".formatted(name()));
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
  public void generateInspect(IndentedWriter content) {
    content.append(String.format("""
        if has_%1$s?
          io << "%1$s="
          %1$s.inspect(io)
        else
          io << "!%1$s"
        end
        """, name()));
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
