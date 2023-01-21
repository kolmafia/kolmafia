package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AccordionsCommand extends AbstractCommand {
  static final Accordion[] ACCORDIONS = {
    new Accordion(ItemPool.STOLEN_ACCORDION, "starter item"),
    new Accordion(ItemPool.ROCK_N_ROLL_LEGEND, "Epic Weapon"),
    new Accordion(ItemPool.SQUEEZEBOX_OF_THE_AGES, "Legendary Epic Weapon"),
    new Accordion(ItemPool.TRICKSTER_TRIKITIXA, "Your Nemesis"),
    new Accordion(ItemPool.BEER_BATTERED_ACCORDION, "drunken half-orc hobo"),
    new Accordion(ItemPool.BARITONE_ACCORDION, "bar"),
    new Accordion(ItemPool.MAMAS_SQUEEZEBOX, "werecougar"),
    new Accordion(ItemPool.GUANCERTINA, "perpendicular bat"),
    new Accordion(ItemPool.ACCORDION_FILE, "Knob Goblin Accountant"),
    new Accordion(ItemPool.ACCORD_ION, "Hellion"),
    new Accordion(ItemPool.BONE_BANDONEON, "toothy sklelton"),
    new Accordion(ItemPool.PENTATONIC_ACCORDION, "Ninja Snowman (Chopsticks)"),
    new Accordion(ItemPool.ACCORDION_OF_JORDION, "1335 HaXx0r"),
    new Accordion(ItemPool.NON_EUCLIDEAN_NON_ACCORDION, "cubist bull"),
    new Accordion(ItemPool.AUTOCALLIOPE, "Steampunk Giant"),
    new Accordion(ItemPool.GHOST_ACCORDION, "skeletal sommelier"),
    new Accordion(ItemPool.PYGMY_CONCERTINETTE, "drunk pygmy"),
    new Accordion(ItemPool.ACCORDIONOID_ROCCA, "drab bard"),
    new Accordion(ItemPool.PEACE_ACCORDION, "War Hippy (space) Cadet"),
    new Accordion(ItemPool.ALARM_ACCORDION, "alert mariachi"),
    new Accordion(ItemPool.BAL_MUSETTE_ACCORDION, "depressing French accordionist"),
    new Accordion(ItemPool.CAJUN_ACCORDION, "lively Cajun accordionist"),
    new Accordion(ItemPool.QUIRKY_ACCORDION, "quirky indie-rock accordionist"),
    new Accordion(ItemPool.SHAKESPEARES_SISTERS_ACCORDION, "smithed using The Smith's Tome"),
  };

  public AccordionsCommand() {
    this.usage = " - List status of stolen accordions.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    List<AdventureResult> found = new ArrayList<>();
    String[] itemIds = Preferences.getString("_stolenAccordions").split(",");
    for (int i = 0; i < itemIds.length; ++i) {
      found.add(ItemPool.get(StringUtilities.parseInt(itemIds[i]), 1));
    }

    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=5>");
    output.append("<tr>");
    output.append("<th rowspan=2>Accordion</th>");
    output.append("<th>Have/Today</th>");
    output.append("<th>Source</th>");
    output.append("<th>Hands</th>");
    output.append("<th>Song</th>");
    output.append("</tr>");
    output.append("<tr>");
    output.append("<th colspan=4>Enchantments</th>");
    output.append("</tr>");

    for (int i = 0; i < ACCORDIONS.length; ++i) {
      Accordion accordion = ACCORDIONS[i];
      AdventureResult item = accordion.getItem();

      output.append("<tr>");

      output.append("<td rowspan=2>");
      output.append(accordion.getName());
      output.append("</td>");

      output.append("<td>");
      boolean have = item.getCount(KoLConstants.inventory) > 0 || KoLCharacter.hasEquipped(item);
      boolean today = found.contains(item);
      output.append(have ? "yes" : "no");
      output.append("/");
      output.append(today ? "yes" : "no");
      output.append("</td>");

      output.append("<td>");
      output.append(accordion.getMonster());
      output.append("</td>");

      output.append("<td>");
      output.append(accordion.getHands());
      output.append("</td>");

      output.append("<td>");
      output.append(accordion.getSongDuration());
      output.append("</td>");

      output.append("</tr>");
      output.append("<tr>");

      output.append("<td colspan=4>");
      output.append(
          Modifiers.evaluateModifiers(accordion.getModsLookup(), accordion.getEnchantments())
              .toString());
      output.append("</td>");

      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }

  public static class Accordion {
    private final AdventureResult item;
    private final String name;
    private final String monster;
    private final int hands;
    private final int songDuration;
    private final Modifiers.Lookup modsLookup;
    private final String enchantments;

    public Accordion(final int itemId, final String monster) {
      this.item = ItemPool.get(itemId, 1);
      this.name = this.item.getName();
      this.monster = monster;
      this.hands = EquipmentDatabase.getHands(itemId);

      Modifiers mods = Modifiers.getItemModifiers(itemId);
      if (mods != null) {
        this.songDuration = (int) mods.get(DoubleModifier.SONG_DURATION);
        this.modsLookup = mods.getLookup();

        if (itemId == ItemPool.AUTOCALLIOPE) {
          // Special case to prevent stretching table way wide
          this.enchantments = "Prismatic Damage: [2*(1+skill(Accordion Appreciation))]";
        } else {
          String enchantments = mods.getString(StringModifier.MODIFIERS);
          enchantments = Modifiers.trimModifiers(enchantments, "Class");
          enchantments = Modifiers.trimModifiers(enchantments, "Song Duration");
          this.enchantments = enchantments;
        }
      } else {
        // Handle items missing from modifiers.txt
        this.songDuration = 0;
        this.modsLookup = new Modifiers.Lookup(ModifierType.NONE, "");
        this.enchantments = "";
      }
    }

    public AdventureResult getItem() {
      return this.item;
    }

    public String getName() {
      return this.name;
    }

    public String getMonster() {
      return this.monster;
    }

    public int getHands() {
      return this.hands;
    }

    public int getSongDuration() {
      return this.songDuration;
    }

    public Modifiers.Lookup getModsLookup() {
      return this.modsLookup;
    }

    public String getEnchantments() {
      return this.enchantments;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
