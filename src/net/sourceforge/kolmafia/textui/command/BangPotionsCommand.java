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

  record Identifiable(String name, String shortName, int id) {}

  // In alphabetical order, for prettiness, and for convenience in
  // looking at combat item dropdown
  private static final Identifiable[] BANG_POTIONS =
      new Identifiable[] {
        new Identifiable("bubbly potion", "bubbly", ItemPool.BUBBLY_POTION),
        new Identifiable("cloudy potion", "cloudy", ItemPool.CLOUDY_POTION),
        new Identifiable("dark potion", "dark", ItemPool.DARK_POTION),
        new Identifiable("effervescent potion", "effervescent", ItemPool.EFFERVESCENT_POTION),
        new Identifiable("fizzy potion", "fizzy", ItemPool.FIZZY_POTION),
        new Identifiable("milky potion", "milky", ItemPool.MILKY_POTION),
        new Identifiable("murky potion", "murky", ItemPool.MURKY_POTION),
        new Identifiable("smoky potion", "smoky", ItemPool.SMOKY_POTION),
        new Identifiable("swirly potion", "swirly", ItemPool.SWIRLY_POTION),
      };

  private static final Identifiable[] SLIME_VIALS =
      new Identifiable[] {
        new Identifiable("vial of red slime", "red", ItemPool.VIAL_OF_RED_SLIME),
        new Identifiable("vial of yellow slime", "yellow", ItemPool.VIAL_OF_YELLOW_SLIME),
        new Identifiable("vial of blue slime", "blue", ItemPool.VIAL_OF_BLUE_SLIME),
        new Identifiable("vial of orange slime", "orange", ItemPool.VIAL_OF_ORANGE_SLIME),
        new Identifiable("vial of green slime", "green", ItemPool.VIAL_OF_GREEN_SLIME),
        new Identifiable("vial of violet slime", "violet", ItemPool.VIAL_OF_VIOLET_SLIME),
        new Identifiable("vial of vermilion slime", "vermilion", ItemPool.VIAL_OF_VERMILION_SLIME),
        new Identifiable("vial of amber slime", "amber", ItemPool.VIAL_OF_AMBER_SLIME),
        new Identifiable(
            "vial of chartreuse slime", "chartreuse", ItemPool.VIAL_OF_CHARTREUSE_SLIME),
        new Identifiable("vial of teal slime", "teal", ItemPool.VIAL_OF_TEAL_SLIME),
        new Identifiable("vial of indigo slime", "indigo", ItemPool.VIAL_OF_INDIGO_SLIME),
        new Identifiable("vial of purple slime", "purple", ItemPool.VIAL_OF_PURPLE_SLIME),
        new Identifiable("vial of brown slime", "brown", ItemPool.VIAL_OF_BROWN_SLIME),
      };

  @Override
  public void run(final String cmd, final String parameters) {
    var table = BangPotionsCommand.BANG_POTIONS;
    String pref = "lastBangPotion";

    if (cmd.startsWith("v")) {
      table = BangPotionsCommand.SLIME_VIALS;
      pref = "lastSlimeVial";
    }

    for (Identifiable identifiable : table) {
      int itemId = identifiable.id;
      String shortName = identifiable.shortName;
      StringBuilder buf = new StringBuilder(shortName);
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
