package space.rymiel.protocr;

import java.util.List;

record Field(String name, int number, ProtoType type, int cIdx, List<Field> oneOfSiblings) {
}
