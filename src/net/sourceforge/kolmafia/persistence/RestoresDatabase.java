package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RestoreExpression;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RestoresDatabase {
  private static final ArrayList<String> restoreNames = new ArrayList<>();
  private static final Map<String, String> typeByName = new HashMap<>();
  private static final Map<String, String> hpMinByName = new HashMap<>();
  private static final Map<String, String> hpMaxByName = new HashMap<>();
  private static final Map<String, String> mpMinByName = new HashMap<>();
  private static final Map<String, String> mpMaxByName = new HashMap<>();
  private static final Map<String, Integer> advCostByName = new HashMap<>();
  private static final Map<String, String> usesLeftByName = new HashMap<>();
  private static final Map<String, String> notesByName = new HashMap<>();

  static {
    RestoresDatabase.reset();
  }

  private RestoresDatabase() {}

  public static void reset() {
    RestoresDatabase.restoreNames.clear();
    RestoresDatabase.typeByName.clear();
    RestoresDatabase.hpMinByName.clear();
    RestoresDatabase.hpMaxByName.clear();
    RestoresDatabase.mpMinByName.clear();
    RestoresDatabase.mpMaxByName.clear();
    RestoresDatabase.advCostByName.clear();
    RestoresDatabase.usesLeftByName.clear();
    RestoresDatabase.notesByName.clear();

    RestoresDatabase.readData();
  }

  private static void readData() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("restores.txt", KoLConstants.RESTORES_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 7) {
          continue;
        }

        String name = data[0];
        RestoresDatabase.restoreNames.add(name);
        RestoresDatabase.typeByName.put(name, data[1]);
        RestoresDatabase.hpMinByName.put(name, data[2]);
        RestoresDatabase.hpMaxByName.put(name, data[3]);
        RestoresDatabase.mpMinByName.put(name, data[4]);
        RestoresDatabase.mpMaxByName.put(name, data[5]);
        int advCost = StringUtilities.parseInt(data[6]);
        RestoresDatabase.advCostByName.put(name, advCost);

        if (data.length > 7) {
          RestoresDatabase.usesLeftByName.put(name, data[7]);
        } else {
          RestoresDatabase.usesLeftByName.put(name, "unlimited");
        }

        if (data.length > 8) {
          RestoresDatabase.notesByName.put(name, data[8]);
        } else {
          RestoresDatabase.notesByName.put(name, "");
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final void setValue(
      final String name,
      final String type,
      final String hpMin,
      final String hpMax,
      final String mpMin,
      final String mpMax,
      final int advCost,
      final int usesLeft,
      final String notes) {
    RestoresDatabase.typeByName.put(name, type);
    RestoresDatabase.hpMinByName.put(name, hpMin);
    RestoresDatabase.hpMaxByName.put(name, hpMax);
    RestoresDatabase.mpMinByName.put(name, mpMin);
    RestoresDatabase.mpMaxByName.put(name, mpMax);
    RestoresDatabase.advCostByName.put(name, advCost);
    if (usesLeft != -1) {
      RestoresDatabase.usesLeftByName.put(name, Integer.toString(usesLeft));
    }
    if (notes != null) {
      RestoresDatabase.notesByName.put(name, notes);
    }
  }

  private static long getValue(final String stringValue, final String name) {
    if (stringValue == null) {
      return -1;
    }
    int lb = stringValue.indexOf("[");
    if (lb == -1) {
      return Long.parseLong(stringValue);
    }
    int rb = stringValue.indexOf("]", lb);
    RestoreExpression expr = new RestoreExpression(stringValue.substring(lb + 1, rb), name);
    if (expr.hasErrors()) {
      KoLmafia.updateDisplay(
          "Error in restores.txt for item " + name + ", invalid expression " + stringValue);
      return -1;
    }
    return (long) expr.eval();
  }

  public static final String getType(final String name) {
    if (name == null) {
      return null;
    }

    return RestoresDatabase.typeByName.get(name);
  }

  public static final long getHPMin(final String name) {
    if (name == null || !RestoresDatabase.pathSafeHP(name)) {
      return 0;
    }

    if (name.equals("Campground") || name.equals("Free rests")) {
      return KoLCharacter.getRestingHP();
    }

    String hpMin = RestoresDatabase.hpMinByName.get(name);
    if (hpMin == null) {
      return 0;
    }
    return (long) Math.floor(RestoresDatabase.getValue(hpMin, name));
  }

  public static final long getHPMax(final String name) {
    if (name == null || !RestoresDatabase.pathSafeHP(name)) {
      return 0;
    }

    if (name.equals("Campground") || name.equals("Free rests")) {
      return KoLCharacter.getRestingHP();
    }

    String hpMax = RestoresDatabase.hpMaxByName.get(name);
    if (hpMax == null) {
      return 0;
    }
    return (long) Math.ceil(RestoresDatabase.getValue(hpMax, name));
  }

  public static final long getMPMin(final String name) {
    if (name == null || !RestoresDatabase.pathSafeMP(name)) {
      return 0;
    }

    if (name.equals("Campground") || name.equals("Free rests")) {
      return KoLCharacter.getRestingMP();
    }

    String mpMin = RestoresDatabase.mpMinByName.get(name);
    if (mpMin == null) {
      return 0;
    }
    return (long) Math.floor(RestoresDatabase.getValue(mpMin, name));
  }

  public static final long getMPMax(final String name) {
    if (name == null || !RestoresDatabase.pathSafeMP(name)) {
      return 0;
    }

    if (name.equals("Campground") || name.equals("Free rests")) {
      return KoLCharacter.getRestingMP();
    }

    String mpMax = RestoresDatabase.mpMaxByName.get(name);
    if (mpMax == null) {
      return 0;
    }
    return (long) Math.ceil(RestoresDatabase.getValue(mpMax, name));
  }

  public static final double getHPAverage(final String name) {
    if (name == null || !RestoresDatabase.pathSafeHP(name)) {
      return 0;
    }

    return (RestoresDatabase.getHPMax(name) + RestoresDatabase.getHPMin(name)) / 2.0;
  }

  public static final double getMPAverage(final String name) {
    if (name == null || !RestoresDatabase.pathSafeMP(name)) {
      return 0;
    }

    return (RestoresDatabase.getMPMax(name) + RestoresDatabase.getMPMin(name)) / 2.0;
  }

  public static final String getHPRange(final String name) {
    if (name == null || !RestoresDatabase.pathSafeHP(name)) {
      return "";
    }

    long hpMin = RestoresDatabase.getHPMin(name);
    long hpMax = RestoresDatabase.getHPMax(name);
    if (hpMin == 0 && hpMax == 0) {
      return "";
    }
    if (hpMin == hpMax) {
      return Long.toString(hpMin);
    }
    return (hpMin + "-" + hpMax);
  }

  public static final String getMPRange(final String name) {
    if (name == null || !RestoresDatabase.pathSafeMP(name)) {
      return "";
    }

    long mpMin = RestoresDatabase.getMPMin(name);
    long mpMax = RestoresDatabase.getMPMax(name);
    if (mpMin == 0 && mpMax == 0) {
      return "";
    }
    if (mpMin == mpMax) {
      return Long.toString(mpMin);
    }
    return (mpMin + "-" + mpMax);
  }

  public static final int getAdvCost(final String name) {
    if (name == null) {
      return 0;
    }

    Integer advCost = RestoresDatabase.advCostByName.get(name);
    if (advCost == null) {
      return 0;
    }
    return advCost.intValue();
  }

  public static final int getUsesLeft(final String name) {
    if (name == null) {
      return 0;
    }

    if (name.equals("Free rests")) {
      return Math.max(KoLCharacter.freeRestsAvailable() - Preferences.getInteger("timesRested"), 0);
    }

    if (getType(name).equals("item")) {
      int max = UseItemRequest.maximumUses(name);
      return (max == Integer.MAX_VALUE) ? -1 : max;
    }

    String usesLeft = RestoresDatabase.usesLeftByName.get(name);
    if (usesLeft == null) {
      return 0;
    }
    if (usesLeft.equals("unlimited")) {
      return -1;
    }
    return (int) Math.floor(RestoresDatabase.getValue(usesLeft, name));
  }

  public static final String getNotes(final String name) {
    if (name == null) {
      return null;
    }

    String notes = RestoresDatabase.notesByName.get(name);
    if (notes == null) {
      return "";
    }
    return notes;
  }

  public static final Boolean isRestore(final int itemId) {
    String name = ItemDatabase.getItemName(itemId);
    if (name == null) {
      return false;
    }
    String type = RestoresDatabase.getType(name);
    if (type == null) {
      return false;
    }
    return type.equalsIgnoreCase("item");
  }

  public static final Boolean restoreAvailable(final String name, final Boolean purchaseable) {
    var limitMode = KoLCharacter.getLimitMode();

    String type = RestoresDatabase.getType(name);

    return switch (type) {
      case "item" -> {
        int itemId = ItemDatabase.getItemId(name);
        yield purchaseable
            ? (ItemDatabase.isTradeable(itemId) || InventoryManager.getAccessibleCount(itemId) > 0)
            : (InventoryManager.getAccessibleCount(itemId) > 0);
      }
      case "skill" -> KoLCharacter.hasSkill(name);
      case "loc" -> switch (name) {
        case "A Relaxing Hot Tub" -> InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0
            && (!KoLCharacter.inBadMoon() || KoLCharacter.kingLiberated())
            && !limitMode.limitClan();
        case "April Shower" -> InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0
            && (!KoLCharacter.inBadMoon() || KoLCharacter.kingLiberated())
            && StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "April Shower")
            && !limitMode.limitClan();
        case "Campground" -> !limitMode.limitCampground()
            && !KoLCharacter.isEd()
            && !KoLCharacter.inNuclearAutumn();
        case "Comfy Sofa" -> !limitMode.limitClan();
        case "Doc Galaktik's" -> true;
        case "Free rests" -> KoLCharacter.freeRestsAvailable() > 0;
        case "Nunnery (Frat Warrior)" -> Preferences.getString("sidequestNunsCompleted")
            .equals("fratboy");
        case "Nunnery (War Hippy)" -> Preferences.getString("sidequestNunsCompleted")
            .equals("hippy");
        default -> false;
      };
      default -> false;
    };
  }

  public static final String[][] getRestoreData(final String level) {
    Iterator<String> it = RestoresDatabase.restoreNames.iterator();

    int restores = 0;
    int count = RestoresDatabase.restoreNames.size();

    if (count > 0) {
      String[][] restoreData = new String[count][7];
      while (it.hasNext()) {
        String current = it.next();
        if ((level.equals("available") && RestoresDatabase.restoreAvailable(current, false))
            || (level.equals("obtainable") && RestoresDatabase.restoreAvailable(current, true))
            || level.equals("all")) {
          restoreData[restores][0] = current;
          restoreData[restores][1] = RestoresDatabase.getType(current);
          restoreData[restores][2] = RestoresDatabase.getHPRange(current);
          restoreData[restores][3] = RestoresDatabase.getMPRange(current);
          restoreData[restores][4] = String.valueOf(RestoresDatabase.getAdvCost(current));
          int usesLeft = RestoresDatabase.getUsesLeft(current);
          restoreData[restores][5] = usesLeft == -1 ? "Unlimited" : Integer.toString(usesLeft);
          restoreData[restores][6] = RestoresDatabase.getNotes(current);
          restores++;
        }
      }
      return restoreData;
    }

    return null;
  }

  public static final boolean restoresMaxHP(final String restore) {
    return RestoresDatabase.hpMinByName.get(restore).equals("[HP]");
  }

  public static final boolean restoresMaxMP(final String restore) {
    return RestoresDatabase.mpMinByName.get(restore).equals("[MP]");
  }

  public static final boolean pathSafeHP(final String hpRestore) {
    if (KoLCharacter.isVampyre()) {
      return false;
    }

    if (KoLCharacter.isEd()) {
      return hpRestore.equals("cotton bandages")
          || hpRestore.equals("linen bandages")
          || hpRestore.equals("silk bandages");
    }

    if (KoLCharacter.isPlumber()) {
      return hpRestore.equals("mushroom")
          || hpRestore.equals("deluxe mushroom")
          || hpRestore.equals("super deluxe mushroom");
    }
    return true;
  }

  public static final boolean pathSafeMP(final String mpRestore) {
    if (KoLCharacter.isVampyre()) {
      return false;
    }
    return true;
  }
}
