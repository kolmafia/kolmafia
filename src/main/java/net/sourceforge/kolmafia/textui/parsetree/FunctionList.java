package net.sourceforge.kolmafia.textui.parsetree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import net.sourceforge.kolmafia.textui.DataTypes;

public class FunctionList implements Iterable<Function> {
  private final TreeMap<String, Function> list = new TreeMap<>();

  // Assumes there will not be more than 65535 functions in any scope.
  // Assumes that \0 will never appear in a function name.
  private char sequence = '\0';

  public boolean add(final Function f) {
    this.list.put(f.getName().toLowerCase() + '\0' + this.sequence, f);
    ++this.sequence;
    return true;
  }

  public boolean remove(final Function f) {
    return this.list.values().remove(f);
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public Iterator<Function> iterator() {
    return list.values().iterator();
  }

  public Function[] findFunctions(String name) {
    name = name.toLowerCase();
    return this.list.subMap(name + '\0', name + '\1').values().toArray(new Function[0]);
  }

  public Function findMatchingFunction(
      String functionName, List<Value> ashArgs, boolean coerceAnyType) {
    Function[] libraryFunctions = findFunctions(functionName);

    if (coerceAnyType && ashArgs.stream().noneMatch(v -> v.getType() == DataTypes.ANY_TYPE)) {
      coerceAnyType = false;
    }
    List<Value> coercedArgs = coerceAnyType ? new ArrayList<>(ashArgs) : ashArgs;

    Function.MatchType[] matchTypes = {
      Function.MatchType.EXACT, Function.MatchType.BASE, Function.MatchType.COERCE
    };
    for (Function.MatchType matchType : matchTypes) {
      for (Function testFunction : libraryFunctions) {
        if (coerceAnyType) {
          var references = testFunction.getVariableReferences();
          // If references is empty, no coercion to be done. Otherwise, have to be careful to match
          // the varargs case.
          for (int i = 0; i < ashArgs.size() && !references.isEmpty(); i++) {
            if (ashArgs.get(i).getType() == DataTypes.ANY_TYPE) {
              int referenceIndex = i >= references.size() ? references.size() - 1 : i;
              Type expectedType = references.get(referenceIndex).getType();
              Value coerced = new Value(expectedType);
              coercedArgs.set(i, coerced);
            }
          }
        }

        if (testFunction.paramsMatch(coercedArgs, matchType)) {
          return testFunction;
        }
      }
    }

    return null;
  }
}
