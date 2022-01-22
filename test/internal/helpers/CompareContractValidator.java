package internal.helpers;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

public class CompareContractValidator {
    // Helper method to force normalize  comparisons to [-1, 0, 1] before testing
    private static int sgn(int value) {
        return Integer.compare(value, 0);
    }

    // Helper method to append item id
    private static String getIString(Concoction con) {
        return "[" + con.getItemId() + "] " + con;
    }

    public static List<Violaters> checkForContractViolations() {

    // Code intended to verify that Concoction.compareTo() meets its contract.  Since the
    // concoctions data is in a file and this is an expensive check, in terms of time,
    // moved out of unit testing to here.
    Concoction[] ids;
    int maxIndex;
    String msg;
    int[][] result;
    LockableListModel<Concoction> usables = ConcoctionDatabase.getUsables();
    // size is all elements.  getSize is visible elements.
    maxIndex = usables.size();
    ids = new Concoction[maxIndex];
    int i = 0;
    for (Concoction con : usables) {
        ids[i++] = con;
    }
    result = new int[maxIndex][maxIndex];
    for (i = 0; i < maxIndex; ++i) {
        for (int j = 0; j < maxIndex; ++j) {
            result[i][j] = sgn(ids[i].compareTo(ids[j]));
        }
    }
    // sgn(x.compareTo(y)) == -sgn(y.compareTo(x)
    for (i = 0; i < maxIndex; ++i) {
        for (int j = 0; j < maxIndex; ++j) {
            msg =
                    "Failed comparing (quasi symmetry) "
                            + getIString(ids[i])
                            + " and "
                            + getIString(ids[j]);
            if (result[i][j] != -result[j][i]) {
                KoLmafia.updateDisplay(msg);
            }
        }
    }
    // tests the portion of the contract that says (x.compareTo(y)==0) == (x.equals(y))
    for (i = 0; i < maxIndex; ++i) {
        for (int j = 0; j < maxIndex; ++j) {
            if (result[i][j] == 0) {
                msg = "Failed comparing (equality) " + getIString(ids[i]) + " and " + getIString(ids[j]);
                if (!(ids[i].equals(ids[j]))) {
                    KoLmafia.updateDisplay(msg);
                }
                msg =
                        "Failed comparing (other equality) "
                                + getIString(ids[i])
                                + " and "
                                + getIString(ids[j]);
                if (ids[i] != ids[j]) {
                    KoLmafia.updateDisplay(msg);
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
                    msg =
                            "Failed comparing (transitive)"
                                    + getIString(ids[i])
                                    + " and "
                                    + getIString(ids[j])
                                    + " and "
                                    + getIString(ids[k]);
                    if (result[i][k] != result[j][k]) {
                        KoLmafia.updateDisplay(msg);
                    }
                }
            }
        }
    }
}
}
