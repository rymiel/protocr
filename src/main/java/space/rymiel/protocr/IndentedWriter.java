package space.rymiel.protocr;

public class IndentedWriter {
  private int indent = 0;
  private boolean mustIndent = false;
  private final StringBuilder builder = new StringBuilder();

  public IndentedWriter indent() {
    indent += 2;
    return this;
  }

  public IndentedWriter dedent() {
    indent -= 2;
    return this;
  }

  public IndentedWriter append(String string) {
    boolean flag = false;
    for (var i : string.split("\n", -1)) {
      if (flag) {
        builder.append('\n');
        mustIndent = true;
      }
      if (!i.isEmpty()) {
        if (mustIndent) {
          writeIndent();
          mustIndent = false;
        }
        builder.append(i);
      }
      flag = true;
    }
    return this;
  }

  private void writeIndent() {
    builder.append(" ".repeat(Math.max(0, indent)));
  }

  public String toString() {
    return builder.toString();
  }
}
