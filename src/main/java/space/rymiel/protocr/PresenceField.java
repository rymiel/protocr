package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PresenceField extends SimpleField {
  private final int cIdx;

  PresenceField(FieldDescriptorProto protoField, ProtoType type, OneOf oneOf, int cIdx) {
    super(protoField, type, oneOf);
    this.cIdx = cIdx;
  }

  public void generateAssignNilable(IndentedWriter content) {
    content.append(String.format("""
        if %1$s.nil?
          @%1$s = %2$s
        else
          @_presence.set(%3$d, true)
          @%1$s = %1$s
        end
        """, name(), defaultValue(), this.cIdx));
  }

  @Override
  public void generateAssignEmpty(IndentedWriter content) {
    content.append("@%s = %s\n".formatted(name(), defaultValue()));
  }

  @Override
  public void generateWhenFieldNumber(IndentedWriter content) {
    content.append("when %d\n".formatted(number())).indent();
    content.append(String.format("""
        @%2$s = r.%3$s.not_nil!
        @_presence.set(%4$d, true)
        """, number(), name(), type.readerMethod(), this.cIdx));
    for (SimpleField sibling : oneOfSiblings()) {
      content.append("clear_%s!\n".formatted(sibling.name()));
    }
    content.dedent();
  }

  @Override
  public void generateCheckEquality(IndentedWriter content) {
    content.append("return false unless self.%1$s == other.%1$s if self.has_%1$s?\n".formatted(name()));
  }

  @Override
  public void generateProperty(IndentedWriter content) {
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
        """, name(), type.crystalType(), this.cIdx, defaultValue()));

    content.append("def %1$s=(value : %2$s) : Nil\n".formatted(name(), type.crystalType())).indent();
    content.append("@%1$s = value\n".formatted(name()));
    content.append("@_presence.set(%1$d, true)\n".formatted(this.cIdx));
    for (SimpleField sibling : oneOfSiblings()) {
      content.append("clear_%s!\n".formatted(sibling.name()));
    }
    content.dedent().append("end\n");
  }
}
