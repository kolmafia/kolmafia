package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;

public class BangPotionsCommand extends AbstractCommand {
  public BangPotionsCommand() {
    this.usage = " - list the potions you've identified.";
  }

  // In alphabetical order, for prettiness, and for convenience in
  // looking at combat item dropdown
  private static final Object[][] BANG_POTIONS =
      new Object[][] {
        {"bubbly potion", "bubbly", IntegerPool.get(ItemPool.BUBBLY_POTION)},
        {"cloudy potion", "cloudy", IntegerPool.get(ItemPool.CLOUDY_POTION)},
        {"dark potion", "dark", IntegerPool.get(ItemPool.DARK_POTION)},
        {"effervescent potion", "effervescent", IntegerPool.get(ItemPool.EFFERVESCENT_POTION)},
        {"fizzy potion", "fizzy", IntegerPool.get(ItemPool.FIZZY_POTION)},
        {"milky potion", "milky", IntegerPool.get(ItemPool.MILKY_POTION)},
        {"murky potion", "murky", IntegerPool.get(ItemPool.MURKY_POTION)},
        {"smoky potion", "smoky", IntegerPool.get(ItemPool.SMOKY_POTION)},
        {"swirly potion", "swirly", IntegerPool.get(ItemPool.SWIRLY_POTION)},
      };

  private static final Object[][] SLIME_VIALS =
      new Object[][] {
        {"vial of red slime", "red", IntegerPool.get(ItemPool.VIAL_OF_RED_SLIME)},
        {"vial of yellow slime", "yellow", IntegerPool.get(ItemPool.VIAL_OF_YELLOW_SLIME)},
        {"vial of blue slime", "blue", IntegerPool.get(ItemPool.VIAL_OF_BLUE_SLIME)},
        {"vial of orange slime", "orange", IntegerPool.get(ItemPool.VIAL_OF_ORANGE_SLIME)},
        {"vial of green slime", "green", IntegerPool.get(ItemPool.VIAL_OF_GREEN_SLIME)},
        {"vial of violet slime", "violet", IntegerPool.get(ItemPool.VIAL_OF_VIOLET_SLIME)},
        {"vial of vermilion slime", "vermilion", IntegerPool.get(ItemPool.VIAL_OF_VERMILION_SLIME)},
        {"vial of amber slime", "amber", IntegerPool.get(ItemPool.VIAL_OF_AMBER_SLIME)},
        {
          "vial of chartreuse slime",
          "chartreuse",
          IntegerPool.get(ItemPool.VIAL_OF_CHARTREUSE_SLIME)
        },
        {"vial of teal slime", "teal", IntegerPool.get(ItemPool.VIAL_OF_TEAL_SLIME)},
        {"vial of indigo slime", "indigo", IntegerPool.get(ItemPool.VIAL_OF_INDIGO_SLIME)},
        {"vial of purple slime", "purple", IntegerPool.get(ItemPool.VIAL_OF_PURPLE_SLIME)},
        {"vial of brown slime", "brown", IntegerPool.get(ItemPool.VIAL_OF_BROWN_SLIME)},
      };

  private static String potionName(final Object[][] table, final int index) {
    return (String) table[index][0];
  }

  private static String potionShortName(final Object[][] table, final int index) {
    return (String) table[index][1];
  }

  private static int potionItemId(final Object[][] table, final int index) {
    return ((Integer) table[index][2]).intValue();
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Object[][] table = BangPotionsCommand.BANG_POTIONS;
    String pref = "lastBangPotion";

    if (cmd.startsWith("v")) {
      table = BangPotionsCommand.SLIME_VIALS;
      pref = "lastSlimeVial";
    }

    for (int index = 0; index < table.length; ++index) {
      int itemId = BangPotionsCommand.potionItemId(table, index);
      String shortName = BangPotionsCommand.potionShortName(table, index);
      StringBuffer buf = new StringBuffer(shortName);
      buf.append(": ");
      buf.append(Preferences.getString(pref + itemId));
      AdventureResult item = ItemPool.get(itemId, 1);
      int have = item.getCount(KoLConstants.inventory);
      int closet = item.getCount(KoLConstants.closet);
      CreateItemRequest creator = CreateItemRequest.getInstance(item);
      int create = creator == null ? 0 : creator.getQuantityPossible();
      if (have + closet + create > 0) {
        buf.append(" (have ");
        buf.append(have);
        if (closet > 0) {
          buf.append(", ");
          buf.append(closet);
          buf.append(" in closet");
        }
        if (create > 0) {
          buf.append(", can make ");
          buf.append(create);
        }
        buf.append(")");
      }
      RequestLogger.printLine(buf.toString());
    }
  }
}
