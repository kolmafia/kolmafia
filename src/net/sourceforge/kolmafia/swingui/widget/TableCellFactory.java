package net.sourceforge.kolmafia.swingui.widget;

import javax.swing.JButton;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import net.sourceforge.kolmafia.persistence.Script;
import net.sourceforge.kolmafia.persistence.ScriptManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.swingui.DatabaseFrame;
import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TableCellFactory {
  private TableCellFactory() {}

  public static Object get(
      int columnIndex,
      LockableListModel<?> model,
      Object result,
      boolean[] flags,
      boolean isSelected) {
    return get(columnIndex, model, result, flags, isSelected, false);
  }

  public static Object get(
      int columnIndex,
      LockableListModel<?> model,
      Object result,
      boolean[] flags,
      boolean isSelected,
      boolean raw) {
    if (result instanceof AdventureResult) {
      AdventureResult advresult = (AdventureResult) result;

      if (flags[0]) { // Equipment panel
        return getEquipmentCell(columnIndex, isSelected, advresult, raw);
      }
      if (flags[1]) { // Restores panel
        return getRestoresCell(columnIndex, isSelected, advresult, raw);
      }
      if (model == KoLConstants.storage) {
        return getStorageCell(columnIndex, isSelected, advresult, raw);
      }
      return getGeneralCell(columnIndex, isSelected, advresult, raw);
    }
    if (result instanceof CreateItemRequest) {
      return getCreationCell(columnIndex, (CreateItemRequest) result, isSelected, raw);
    }
    if (result instanceof LowerCaseEntry) {
      if (model == DatabaseFrame.allItems) {
        return getAllItemsCell(
            columnIndex, isSelected, (LowerCaseEntry<Integer, String>) result, raw);
      }
      return getGeneralDatabaseCell(
          columnIndex, isSelected, (LowerCaseEntry<Integer, ?>) result, raw);
    }
    if (result instanceof String || result instanceof Integer || result instanceof JButton) {
      return result;
    }
    if (result instanceof Script) {
      return getScriptCell(columnIndex, isSelected, (Script) result);
    }
    return null;
  }

  private static Object getScriptCell(int columnIndex, boolean isSelected, Script result) {
    switch (columnIndex) {
      case 0:
        return result.getScriptName();
      case 1:
        return result.getAuthors();
      case 2:
        return result.getShortDesc();
      case 3:
        return result.getCategory();
      case 4:
        return result.getRepo();
    }
    return null;
  }

  private static Object getGeneralDatabaseCell(
      int columnIndex, boolean isSelected, LowerCaseEntry<Integer, ?> result, boolean raw) {
    switch (columnIndex) {
      case 0:
        return result.getValue().toString();
      case 1:
        return IntegerPool.get(result.getKey());
    }
    return null;
  }

  private static Object getAllItemsCell(
      int columnIndex, boolean isSelected, LowerCaseEntry<Integer, String> result, boolean raw) {
    switch (columnIndex) {
      case 0:
        return ItemDatabase.getDisplayName(result.getKey());
      case 1:
        return IntegerPool.get(result.getKey());
      case 2:
        return IntegerPool.get(ItemDatabase.getPriceById(result.getKey()));
      case 3:
        return IntegerPool.get(MallPriceDatabase.getPrice(result.getKey()));
      case 4:
        return ConsumablesDatabase.getFullness(result.getValue())
            + ConsumablesDatabase.getInebriety(result.getValue())
            + ConsumablesDatabase.getSpleenHit(result.getValue());
      case 5:
        return ConsumablesDatabase.getAdvRangeByName(
            ItemDatabase.getCanonicalName(result.getKey()));
      case 6:
        return ConsumablesDatabase.getLevelReqByName(result.getValue());
    }
    return null;
  }

  private static Object getGeneralCell(
      int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw) {
    Integer fill;

    switch (columnIndex) {
      case 0:
        if (raw) {
          return advresult.getName();
        }
        return "<html>"
            + addTag(ColorFactory.getItemColor(advresult), isSelected)
            + advresult.getName();
      case 1:
        return getAutosellString(advresult.getItemId(), raw);
      case 2:
        return IntegerPool.get(advresult.getCount());
      case 3:
        Integer price = IntegerPool.get(MallPriceDatabase.getPrice(advresult.getItemId()));
        return (price > 0) ? price : null;
      case 4:
        int power = EquipmentDatabase.getPower(advresult.getItemId());
        return (power > 0) ? IntegerPool.get(power) : null;
      case 5:
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(advresult.getName())
                    + ConsumablesDatabase.getInebriety(advresult.getName())
                    + ConsumablesDatabase.getSpleenHit(advresult.getName()));
        return fill > 0 ? fill : null;
      case 6:
        double advRange = ConsumablesDatabase.getAdventureRange(advresult.getName());
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(advresult.getName())
                    + ConsumablesDatabase.getInebriety(advresult.getName())
                    + ConsumablesDatabase.getSpleenHit(advresult.getName()));
        if (!Preferences.getBoolean("showGainsPerUnit")) {
          advRange = advRange / fill;
        }
        return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format(advRange) : null;
      default:
        return null;
    }
  }

  private static Object getStorageCell(
      int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw) {
    Integer fill;

    switch (columnIndex) {
      case 0:
        if (raw) {
          return advresult.getName();
        }
        return "<html>"
            + addTag(ColorFactory.getStorageColor(advresult), isSelected)
            + advresult.getName();
      case 1:
        return getAutosellString(advresult.getItemId(), raw);
      case 2:
        return IntegerPool.get(advresult.getCount());
      case 3:
        Integer price = IntegerPool.get(MallPriceDatabase.getPrice(advresult.getItemId()));
        return (price > 0) ? price : null;
      case 4:
        int power = EquipmentDatabase.getPower(advresult.getItemId());
        return (power > 0) ? IntegerPool.get(power) : null;
      case 5:
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(advresult.getName())
                    + ConsumablesDatabase.getInebriety(advresult.getName())
                    + ConsumablesDatabase.getSpleenHit(advresult.getName()));
        return fill > 0 ? fill : null;
      case 6:
        double advRange = ConsumablesDatabase.getAdventureRange(advresult.getName());
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(advresult.getName())
                    + ConsumablesDatabase.getInebriety(advresult.getName())
                    + ConsumablesDatabase.getSpleenHit(advresult.getName()));

        if (!Preferences.getBoolean("showGainsPerUnit")) {
          advRange = advRange / fill;
        }
        return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format(advRange) : null;
      default:
        return null;
    }
  }

  private static Object getCreationCell(
      int columnIndex, CreateItemRequest CIRresult, boolean isSelected, boolean raw) {
    Integer fill;

    switch (columnIndex) {
      case 0:
        if (raw) {
          return CIRresult.getName();
        }
        return "<html>"
            + addTag(ColorFactory.getCreationColor(CIRresult), isSelected)
            + CIRresult.getName();
      case 1:
        return getAutosellString(CIRresult.getItemId(), raw);
      case 2:
        return IntegerPool.get(CIRresult.getQuantityPossible());
      case 3:
        Integer price = IntegerPool.get(MallPriceDatabase.getPrice(CIRresult.getItemId()));
        return (price > 0) ? price : null;
      case 4:
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(CIRresult.getName())
                    + ConsumablesDatabase.getInebriety(CIRresult.getName())
                    + ConsumablesDatabase.getSpleenHit(CIRresult.getName()));
        return fill > 0 ? fill : null;
      case 5:
        double advRange = ConsumablesDatabase.getAdventureRange(CIRresult.getName());
        fill =
            IntegerPool.get(
                ConsumablesDatabase.getFullness(CIRresult.getName())
                    + ConsumablesDatabase.getInebriety(CIRresult.getName())
                    + ConsumablesDatabase.getSpleenHit(CIRresult.getName()));
        if (!Preferences.getBoolean("showGainsPerUnit")) {
          advRange = advRange / fill;
        }
        return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format(advRange) : null;
      case 6:
        Integer lev = ConsumablesDatabase.getLevelReqByName(CIRresult.getName());
        return lev != null ? IntegerPool.get(lev) : null;
      default:
        return null;
    }
  }

  private static Object getEquipmentCell(
      int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw) {
    switch (columnIndex) {
      case 0:
        if (raw) {
          return advresult.getName();
        }
        return "<html>"
            + addTag(ColorFactory.getItemColor(advresult), isSelected)
            + advresult.getName();
      case 1:
        return EquipmentDatabase.getPower(advresult.getItemId());
      case 2:
        return IntegerPool.get(advresult.getCount());
      case 3:
        Integer price = IntegerPool.get(MallPriceDatabase.getPrice(advresult.getItemId()));
        return (price > 0) ? price : null;
      case 4:
        return getAutosellString(advresult.getItemId(), raw);
      default:
        return null;
    }
  }

  private static Object getRestoresCell(
      int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw) {
    switch (columnIndex) {
      case 0:
        if (raw) {
          return advresult.getName();
        }
        return "<html>"
            + addTag(ColorFactory.getItemColor(advresult), isSelected)
            + advresult.getName();
      case 1:
        return getAutosellString(advresult.getItemId(), raw);
      case 2:
        return IntegerPool.get(advresult.getCount());
      case 3:
        Integer price = IntegerPool.get(MallPriceDatabase.getPrice(advresult.getItemId()));
        return (price > 0) ? price : null;
      case 4:
        double hpRestore = RestoresDatabase.getHPAverage(advresult.getName());
        if (hpRestore <= 0) {
          return null;
        }
        long maxHP = KoLCharacter.getMaximumHP();
        if (hpRestore > maxHP) {
          return maxHP;
        }
        return (long) hpRestore;
      case 5:
        double mpRestore = RestoresDatabase.getMPAverage(advresult.getName());
        if (mpRestore <= 0) {
          return null;
        }
        long maxMP = KoLCharacter.getMaximumMP();
        if (mpRestore > maxMP) {
          return maxMP;
        }
        return (long) mpRestore;
      default:
        return null;
    }
  }

  private static String addTag(String itemColor, boolean isSelected) {
    if (itemColor == null || isSelected) {
      return "";
    }
    return "<font color=" + itemColor + ">";
  }

  private static Object getAutosellString(int itemId, boolean raw) {
    int price = 0;
    if (ItemDatabase.isDiscardable(itemId)) {
      price = ItemDatabase.getPriceById(itemId);
    }

    if (raw) {
      // if ( price < 0 ) price = 0;
      return IntegerPool.get(price);
    }

    if (price <= 0) {
      return "no-sell";
    }
    return price + " meat";
  }

  public static String[] getColumnNames(LockableListModel<?> originalModel, boolean[] flags) {
    if (flags[0]) { // Equipment panel
      return new String[] {"item name", "power", "quantity", "mallprice", "autosell"};
    } else if (flags[1]) { // Restores panel
      return new String[] {
        "item name", "autosell", "quantity", "mallprice", "HP restore", "MP restore"
      };
    } else if (originalModel == KoLConstants.inventory
        || originalModel == KoLConstants.tally
        || originalModel == KoLConstants.freepulls
        || originalModel == KoLConstants.storage
        || originalModel == KoLConstants.closet
        || originalModel == KoLConstants.nopulls
        || originalModel == KoLConstants.unlimited) {
      return new String[] {
        "item name", "autosell", "quantity", "mallprice", "power", "fill", "adv/fill"
      };
    } else if (originalModel == ConcoctionDatabase.getCreatables()
        || originalModel == ConcoctionDatabase.getUsables()) {
      return new String[] {
        "item name", "autosell", "quantity", "mallprice", "fill", "adv/fill", "level req"
      };
    } else if (originalModel == DatabaseFrame.allItems) {
      return new String[] {
        "item name", "item ID", "autosell", "mallprice", "fill", "adv range", "level req"
      };
    } else if (originalModel == DatabaseFrame.allFamiliars) {
      return new String[] {
        "familiar name", "familiar ID",
      };
    } else if (originalModel == DatabaseFrame.allEffects) {
      return new String[] {
        "effect name", "effect ID",
      };
    } else if (originalModel == DatabaseFrame.allSkills) {
      return new String[] {
        "skill name", "skill ID",
      };
    } else if (originalModel == DatabaseFrame.allOutfits) {
      return new String[] {
        "outfit name", "outfit ID",
      };
    } else if (originalModel == DatabaseFrame.allMonsters) {
      return new String[] {
        "monster name", "monster ID",
      };
    } else if (originalModel == ScriptManager.getInstalledScripts()
        || originalModel == ScriptManager.getRepoScripts()) {
      return new String[] {"Script Name", "Authors", "Description", "Category", "Repo"};
    }
    return new String[] {"not implemented"};
  }

  public static String getTooltipText(Object value, boolean[] flags) {
    if (value instanceof AdventureResult || value instanceof CreateItemRequest) {
      return getModifiers(value);
    }
    return null;
  }

  private static String getModifiers(Object value) {
    // Code almost entirely lifted from GearChangeFrame.

    int modifiersWidth = 100;
    int itemId = -1;
    if (value instanceof AdventureResult) {
      itemId = ((AdventureResult) value).getItemId();
    } else if (value instanceof CreateItemRequest) {
      itemId = ((CreateItemRequest) value).getItemId();
    }

    if (itemId == -1 || !ItemDatabase.isEquipment(itemId)) {
      return null;
    }

    Modifiers mods = Modifiers.getItemModifiers(itemId);
    if (mods == null) {
      return null;
    }
    String name = mods.getString(Modifiers.INTRINSIC_EFFECT);
    if (name.length() > 0) {
      Modifiers newMods = new Modifiers();
      newMods.add(mods);
      newMods.add(Modifiers.getModifiers("Effect", name));
      mods = newMods;
    }

    StringBuilder buff = new StringBuilder();
    buff.append("<html><table><tr><td width=");
    buff.append(modifiersWidth);
    buff.append(">");

    for (int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i) {
      double val = mods.get(i);
      if (val == 0.0) continue;
      name = Modifiers.getModifierName(i);
      name = StringUtilities.singleStringReplace(name, "Familiar", "Fam");
      name = StringUtilities.singleStringReplace(name, "Experience", "Exp");
      name = StringUtilities.singleStringReplace(name, "Damage", "Dmg");
      name = StringUtilities.singleStringReplace(name, "Resistance", "Res");
      name = StringUtilities.singleStringReplace(name, "Percent", "%");
      buff.append(name);
      buff.append(":<div align=right>");
      buff.append(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(val));
      buff.append("</div>");
    }

    boolean anyBool = false;
    for (int i = 1; i < Modifiers.BITMAP_MODIFIERS; ++i) {
      if (mods.getRawBitmap(i) == 0) continue;
      if (anyBool) {
        buff.append(", ");
      }
      anyBool = true;
      buff.append(Modifiers.getBitmapModifierName(i));
    }

    for (int i = 1; i < Modifiers.BOOLEAN_MODIFIERS; ++i) {
      if (!mods.getBoolean(i)) continue;
      if (anyBool) {
        buff.append(", ");
      }
      anyBool = true;
      buff.append(Modifiers.getBooleanModifierName(i));
    }

    buff.append("</td></tr></table></html>");
    return buff.toString();
  }
}
