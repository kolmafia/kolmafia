package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CupOf13sDatabase {
  private CupOf13sDatabase() {}

  private static final Map<Integer, Integer> cupMap = new HashMap<>();

  static {
    CupOf13sDatabase.reset();
  }

  public static int getTier(int itemId) {
    return cupMap.getOrDefault(itemId, -1);
  }

  private static void reset() {
    boolean error = false;
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("cup_of_13s.txt", KoLConstants.CUP_OF_13S_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length >= 3) {
          int itemId = StringUtilities.parseInt(data[0]);
          int itemTier = StringUtilities.parseInt(data[2]);

          cupMap.put(itemId, itemTier);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
      error = true;
    }

    if (error) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Error loading cup of 13s data");
    }
  }
}
