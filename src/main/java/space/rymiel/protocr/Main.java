package space.rymiel.protocr;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos;

import java.io.IOException;

import static com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL;

public class Main {
  public static void main(String[] args) throws IOException {
    var results = PluginProtos.CodeGeneratorResponse.newBuilder().setSupportedFeatures(FEATURE_PROTO3_OPTIONAL.getNumber());
    var thing = PluginProtos.CodeGeneratorRequest.parseFrom(System.in);

    for (var filename : thing.getFileToGenerateList()) {
      FileDescriptorProto file = null;
      for (var f : thing.getProtoFileList()) {
        if (f.getName().equals(filename)) file = f;
      }
      if (file == null) continue;

      var gen = new FileGenerator(file);
      gen.run();
      results.addFileBuilder().setName(StringUtil.crystalFilename(file.getName())).setContent(gen.generated()).build();
    }

    results.build().writeTo(System.out);
  }
}
