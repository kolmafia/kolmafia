package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public class TattooDatabase {
  public static final ArrayList<String> tattoos = new ArrayList<>();
  private static final HashMap<String, String> nameToRequirement = new HashMap<>();
  private static final HashMap<String, String> nameToRequirementType = new HashMap<>();

  static {
    reset();
  }

  public static void reset() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("tattoos.txt", KoLConstants.TATTOOS_VERSION);

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 2) {
        continue;
      }

      String image = data[0];
      String name = getName(image);
      String requirementType = data[1];

      if (!requirementType.equals("custom") && !requirementType.equals("other")) {
        if (data.length < 3) {
          RequestLogger.printLine("Tattoo \"" + name + "\" needs a requirement type");
          continue;
        } else {
          nameToRequirement.put(name, data[2]);
        }
      }
      tattoos.add(name);
      nameToRequirementType.put(name, requirementType);
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static boolean hasItemRequirement(final String name) {
    return nameToRequirementType.get(name).equals("item");
  }

  public static boolean hasOutfitRequirement(final String name) {
    return nameToRequirementType.get(name).equals("outfit");
  }

  public static boolean hasClassRequirement(final String name) {
    return nameToRequirementType.get(name).endsWith("class");
  }

  public static String getImage(final String name) {
    return name + ".gif";
  }

  public static String getName(final String image) {
    return image.substring(0, image.length() - 4);
  }

  public static AdventureResult getItemRequirement(final String name) {
    if (!hasItemRequirement(name)) {
      return null;
    }

    return ItemPool.get(nameToRequirement.get(name), 1);
  }

  public static SpecialOutfit getOutfitRequirement(final String name) {
    if (!hasOutfitRequirement(name)) {
      return null;
    }

    return EquipmentDatabase.getOutfit(nameToRequirement.get(name));
  }

  public static String getClassRequirement(final String name) {
    if (!hasClassRequirement(name)) {
      return null;
    }

    return nameToRequirement.get(name);
  }
}
