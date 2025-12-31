package space.rymiel.protocr;

public interface Field {
  String defaultValue();

  String name();

  ProtoType type();

  void generateAssignNilable(IndentedWriter content);

  void generateAssignEmpty(IndentedWriter content);

  void generateWhenFieldNumber(IndentedWriter content);

  void generateWriteSerialized(IndentedWriter content);

  void generateCheckEquality(IndentedWriter content);

  void generateProperty(IndentedWriter content);
}
