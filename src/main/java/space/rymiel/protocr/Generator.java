package space.rymiel.protocr;

public abstract class Generator {
  protected final IndentedWriter content;

  protected Generator(IndentedWriter content) {
    this.content = content;
  }

  protected IndentedWriter indent() {
    return content.indent();
  }

  protected IndentedWriter dedent() {
    return content.dedent();
  }

  protected IndentedWriter append(String string) {
    return content.append(string);
  }

  public String generated() {
    return this.content.toString();
  }
}
