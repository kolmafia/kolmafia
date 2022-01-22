package internal.helpers;

import java.util.ArrayList;
import java.util.List;

public class CompareContractValidator {
  // Helper method to normalize comparisons to [-1, 0, 1] before testing
  private static int sgn(int value) {
    return Integer.compare(value, 0);
  }

  public static class Violator {
    final String first;
    final String second;
    final String reason;

    public Violator(String a, String b, String c) {
      first = a;
      second = b;
      reason = c;
    }
  }

  public enum ViolationType {
    SYMMETRY,
    FUNCTION_EQUALITY,
    OPERATOR_EQUALITY,
    TRANSITIVITY
  }

  // Code intended to verify that compareTo() meets its contract.
  // public static <T extends Comparable<T>> List<T> checkForContractViolations(List<T> inputList) {
  public static <T> List<Violator> checkForContractViolations(List<T> inputList) {

    List<Violator> violators = new ArrayList<>();
    ArrayList<T> ids = new ArrayList<>(inputList.size());
    int i;
    int maxIndex = inputList.size();
    int[][] result;
    ids.addAll(inputList);
    ViolationType vt;
    result = new int[maxIndex][maxIndex];

    Object checker = inputList.get(0);
    Class[] interfaces = checker.getClass().getInterfaces();
    Object comp = Comparable.class;
    boolean canCompare = false;

    // Not sure how to enforce that the type must be Comparable
    for (i = 0; i < interfaces.length; i++) {
      if (comp == interfaces[i]) {
        canCompare = true;
        break;
      }
    }
    if (!canCompare) {
      Violator faux = new Violator("bad", "input", "fail");
      violators.add(faux);
      return violators;
    }

    for (i = 0; i < maxIndex; ++i) {
      @SuppressWarnings("unchecked")
      Comparable<T> objectThatCanCompare = (Comparable<T>) ids.get(i);
      for (int j = 0; j < maxIndex; ++j) {
        result[i][j] = sgn(objectThatCanCompare.compareTo(ids.get(j)));
      }
    }
    // sgn(x.compareTo(y)) == -sgn(y.compareTo(x)
    for (i = 0; i < maxIndex; ++i) {
      for (int j = 0; j < maxIndex; ++j) {
        vt = ViolationType.SYMMETRY;
        if (result[i][j] != -result[j][i]) {
          violators.add(new Violator(ids.get(i).toString(), ids.get(j).toString(), vt.name()));
        }
      }
    }
    // tests the portion of the contract that says (x.compareTo(y)==0) == (x.equals(y))
    for (i = 0; i < maxIndex; ++i) {
      for (int j = 0; j < maxIndex; ++j) {
        if (result[i][j] == 0) {
          vt = ViolationType.FUNCTION_EQUALITY;
          if (!(ids.get(i).equals(ids.get(j)))) {
            violators.add(new Violator(ids.get(i).toString(), ids.get(j).toString(), vt.name()));
          }

          vt = ViolationType.OPERATOR_EQUALITY;
          if (ids.get(i) != ids.get(j)) {
            violators.add(new Violator(ids.get(i).toString(), ids.get(j).toString(), vt.name()));
          }
        }
      }
    }
    // x.compareTo(y)==0 implies that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
    for (i = 0; i < maxIndex; ++i) {
      // Don't have to check whole matrix
      for (int j = i; j < maxIndex; ++j) {
        if (result[i][j] == 0) {
          for (int k = 1; k < maxIndex; ++k) {
            vt = ViolationType.TRANSITIVITY;
            if (result[i][k] != result[j][k]) {
              violators.add(new Violator(ids.get(i).toString(), ids.get(j).toString(), vt.name()));
            }
          }
        }
      }
    }
    return violators;
  }
}
