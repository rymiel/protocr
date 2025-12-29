package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos;

public class FileGenerator extends Generator {
  private final DescriptorProtos.FileDescriptorProto file;

  protected FileGenerator(DescriptorProtos.FileDescriptorProto file) {
    super(new IndentedWriter());
    this.file = file;
  }

  public void run() {
    append("# Generated from %s by protocr\n".formatted(file.getName()));
    append("{% unless flag?(:protocr_included) %}\n");
    append("require \"protocr\"\n");
    append("{% end %}\n");

    for (String dep : file.getDependencyList()) {
      append("require \"./%s\"\n".formatted(StringUtil.crystalFilename(dep)));
    }

    var ns = StringUtil.nsCrystal(file.getPackage());
    append("\nmodule %s\n".formatted(ns)).indent();

    for (var msg : file.getMessageTypeList()) {
      new MessageGenerator(this.content, msg).run();
    }

    dedent().append("end\n");
  }
}
