package space.rymiel.protocr;

import java.util.List;

public record UnionField(String name, List<SimpleField> members) implements Field {
  @Override
  public void generateAssignNilable(IndentedWriter content) {
    for (var m : this.members) {
      content.append(String.format("""
          if !%2$s.nil?
            @%1$s = %2$s
          end
          """, this.name, m.name()));
    }
  }

  @Override
  public void generateAssignEmpty(IndentedWriter content) {
    content.append("@%s = nil\n".formatted(this.name));
  }

  @Override
  public void generateWhenFieldNumber(IndentedWriter content) {
    for (var m : this.members) {
      content.append("when %d\n".formatted(m.number())).indent();
      // TODO: merge messages? apparently valid
      content.append("@%s = r.%s\n".formatted(this.name, m.type().readerMethod()));
      content.dedent();
    }
  }

  @Override
  public void generateWriteSerialized(IndentedWriter content) {
    content.append("case (%1$s = @%1$s)\n".formatted(this.name));
    for (var m : this.members) {
      content.append(String.format("""
        when %1$s
          w.write_tag(%2$d, ::Protocr::WireType::%3$s)
          w.%4$s(%5$s)
        """, m.type().crystalType(), m.number(), m.type().wireType(), m.type().writerMethod(), this.name));
    }
    content.append("end\n");
  }

  @Override
  public void generateCheckEquality(IndentedWriter content) {
    content.append("return false unless @%1$s == other.@%1$s\n".formatted(this.name));
  }

  @Override
  public void generateProperty(IndentedWriter content) {
    content.append(String.format("""
        @%1$s : %2$s
        
        def %1$s : %2$s
          @%1$s
        end
        """, this.name, this.getCrystalType()));

    for (var m : this.members) {
      content.append(String.format("""
          def %1$s : %2$s
            @%4$s.is_a?(%2$s) ? @%4$s.as(%2$s) : %3$s
          end
          def %1$s=(value : %2$s) : Nil
            @%4$s = value
          end
          def has_%s? : Bool
            @%4$s.is_a? %2$s
          end
          def clear_%1$s! : Nil
            @%4$s = nil
          end
          """, m.name(), m.type().crystalType(), m.defaultValue(), this.name));
    }
  }

  @Override
  public void generateParameter(IndentedWriter content) {
    for (var m : this.members) {
      m.generateParameter(content);
    }
  }

  private String getCrystalType() {
    var content = new StringBuilder();
    content.append("::Union(");
    for (var m : this.members) {
      content.append(m.getCrystalType()).append(", ");
    }
    content.append("::Nil)");
    return content.toString();
  }
}
