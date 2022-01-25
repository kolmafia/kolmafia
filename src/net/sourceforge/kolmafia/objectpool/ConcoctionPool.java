package net.sourceforge.kolmafia.objectpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionPool {
  // ItemID -> Concoction
  private static final Map<Integer, Concoction> items = new TreeMap<Integer, Concoction>();

  // Name -> Concoction
  private static final Map<String, Concoction> nonitems = new TreeMap<String, Concoction>();
  private static final Map<String, Concoction> nonitemsCanonical =
      new TreeMap<String, Concoction>();

  // All concoctions
  private static Collection<Concoction> values = null;
  private static final Map<Integer, Integer> rowCache = new TreeMap<Integer, Integer>();

  static {
    // Pre-set concoctions for all items.
    int maxItemId = ItemDatabase.maxItemId();
    for (int i = 1; i <= maxItemId; ++i) {
      // Skip non-existent items
      if (ItemDatabase.getItemName(i) != null) {
        AdventureResult ar = ItemPool.get(i, 1);
        Concoction c = new Concoction(ar, CraftingType.NOCREATE);
        ConcoctionPool.set(c);
      }
    }
  }

  private ConcoctionPool() {}

  public static Concoction get(int itemId) {
    return ConcoctionPool.items.get(itemId);
  }

  public static Concoction get(final int itemId, final String name) {
    if (itemId > 0) {
      return ConcoctionPool.items.get(itemId);
    }
    Concoction c = ConcoctionPool.nonitems.get(name);
    return c != null ? c : ConcoctionPool.nonitemsCanonical.get(name);
  }

  public static Concoction get(final AdventureResult ar) {
    return ConcoctionPool.get(ar.getItemId(), ar.getName());
  }

  public static void set(final Concoction c) {
    String name = c.getName();
    int itemId = c.getItemId();

    if (itemId > 0) {
      ConcoctionPool.items.put(itemId, c);
    } else {
      ConcoctionPool.nonitems.put(name, c);
      String cname = StringUtilities.getCanonicalName(name);
      if (!name.equals(cname)) {
        ConcoctionPool.nonitemsCanonical.put(cname, c);
      }
    }

    int row = c.getRow();

    if (row > 0) {
      if (ConcoctionPool.rowCache.containsKey(row)) {
        RequestLogger.printLine("Duplicate row for item " + itemId);
      }
      ConcoctionPool.rowCache.put(row, itemId);
    }

    // Rebuild values next time it is needed
    ConcoctionPool.values = null;
  }

  public static int idToRow(int itemId) {
    for (Entry<Integer, Integer> entry : ConcoctionPool.rowCache.entrySet()) {
      if (itemId == entry.getValue()) {
        return entry.getKey();
      }
    }
    return -1;
  }

  public static int rowToId(int row) {
    if (ConcoctionPool.rowCache.containsKey(row)) {
      return ConcoctionPool.rowCache.get(row);
    }
    return -1;
  }

  public static Collection<Concoction> concoctions() {
    if (ConcoctionPool.values == null) {
      ConcoctionPool.values = new ArrayList<Concoction>();
      ConcoctionPool.values.addAll(ConcoctionPool.items.values());
      ConcoctionPool.values.addAll(ConcoctionPool.nonitems.values());
    }
    return ConcoctionPool.values;
  }

  /** Find a concoction made in a particular way that includes the specified ingredient */
  public static final Concoction findConcoction(
      final CraftingType mixingMethod, final int itemId, final int used) {
    for (Concoction item : ConcoctionPool.concoctions()) {
      if (item.getMixingMethod() != mixingMethod) {
        continue;
      }

      AdventureResult[] ingredients =
          ConcoctionDatabase.getStandardIngredients(item.concoction.getItemId());
      if (ingredients == null) {
        continue;
      }

      for (AdventureResult ingredient : ingredients) {
        if (ingredient.getItemId() == itemId && ingredient.getCount() == used) {
          return item;
        }
      }
    }

    return null;
  }

  public static final Concoction findConcoction(final AdventureResult[] ingredients) {
    for (Concoction item : ConcoctionPool.concoctions()) {
      if (item.hasIngredients(ingredients)) {
        return item;
      }
    }

    return null;
  }
}
