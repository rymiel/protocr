package space.rymiel.protocr;

public interface Field {
  void generateParameter(IndentedWriter content);

  void generateAssignNilable(IndentedWriter content);

  void generateAssignEmpty(IndentedWriter content);

  void generateWhenFieldNumber(IndentedWriter content);

  void generateWriteSerialized(IndentedWriter content);

  void generateCheckEquality(IndentedWriter content);

  void generateProperty(IndentedWriter content);
}
