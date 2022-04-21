package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
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
        {"bubbly potion", "bubbly", ItemPool.BUBBLY_POTION},
        {"cloudy potion", "cloudy", ItemPool.CLOUDY_POTION},
        {"dark potion", "dark", ItemPool.DARK_POTION},
        {"effervescent potion", "effervescent", ItemPool.EFFERVESCENT_POTION},
        {"fizzy potion", "fizzy", ItemPool.FIZZY_POTION},
        {"milky potion", "milky", ItemPool.MILKY_POTION},
        {"murky potion", "murky", ItemPool.MURKY_POTION},
        {"smoky potion", "smoky", ItemPool.SMOKY_POTION},
        {"swirly potion", "swirly", ItemPool.SWIRLY_POTION},
      };

  private static final Object[][] SLIME_VIALS =
      new Object[][] {
        {"vial of red slime", "red", ItemPool.VIAL_OF_RED_SLIME},
        {"vial of yellow slime", "yellow", ItemPool.VIAL_OF_YELLOW_SLIME},
        {"vial of blue slime", "blue", ItemPool.VIAL_OF_BLUE_SLIME},
        {"vial of orange slime", "orange", ItemPool.VIAL_OF_ORANGE_SLIME},
        {"vial of green slime", "green", ItemPool.VIAL_OF_GREEN_SLIME},
        {"vial of violet slime", "violet", ItemPool.VIAL_OF_VIOLET_SLIME},
        {"vial of vermilion slime", "vermilion", ItemPool.VIAL_OF_VERMILION_SLIME},
        {"vial of amber slime", "amber", ItemPool.VIAL_OF_AMBER_SLIME},
        {"vial of chartreuse slime", "chartreuse", ItemPool.VIAL_OF_CHARTREUSE_SLIME},
        {"vial of teal slime", "teal", ItemPool.VIAL_OF_TEAL_SLIME},
        {"vial of indigo slime", "indigo", ItemPool.VIAL_OF_INDIGO_SLIME},
        {"vial of purple slime", "purple", ItemPool.VIAL_OF_PURPLE_SLIME},
        {"vial of brown slime", "brown", ItemPool.VIAL_OF_BROWN_SLIME},
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
