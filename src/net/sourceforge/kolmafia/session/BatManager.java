package net.sourceforge.kolmafia.session;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BatManager {
  public static final int BASE_BAT_HEALTH = 30;
  public static final int BASE_BAT_PUNCH = 5;
  public static final int BASE_BAT_KICK = 5;
  public static final int BASE_BAT_ARMOR = 0;
  public static final int BASE_BAT_BULLETPROOFING = 0;
  public static final int BASE_BAT_SPOOKY_RESISTANCE = 0;
  public static final int BASE_BAT_HEAT_RESISTANCE = 0;
  public static final int BASE_BAT_STENCH_RESISTANCE = 0;
  public static final int BASE_BAT_INVESTIGATION_PROGRESS = 3;

  public static final String GOTPORK_CITY = "Somewhere in Gotpork City";
  public static final String BAT_CAVERN = "Bat-Cavern";
  public static final String CENTER_PARK = "Center Park (Low Crime)";
  public static final String SLUMS = "Slums (Moderate Crime)";
  public static final String INDUSTRIAL_DISTRICT = "Industrial District (High Crime)";
  public static final String DOWNTOWN = "Downtown";

  private static final TreeSet<BatUpgrade> upgrades = new TreeSet<BatUpgrade>();
  private static final BatStats stats = new BatStats();
  private static int DwayneCoFunds = 0;
  private static int DwayneCoBonusFunds = 0;
  private static int BatMinutes = 0;
  private static String zone = BatManager.GOTPORK_CITY;

  private static final AdventureResult[] ITEMS = {
    // Raw materials for Bat-Fabricator
    ItemPool.get(ItemPool.HIGH_GRADE_METAL, 1),
    ItemPool.get(ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1),
    ItemPool.get(ItemPool.HIGH_GRADE_EXPLOSIVES, 1),

    // Items from Bat-Fabricator
    ItemPool.get(ItemPool.BAT_OOMERANG, 1),
    ItemPool.get(ItemPool.BAT_JUTE, 1),
    ItemPool.get(ItemPool.BAT_O_MITE, 1),

    // Currency & items from Orphanage
    ItemPool.get(ItemPool.KIDNAPPED_ORPHAN, 1),
    ItemPool.get(ItemPool.CONFIDENCE_BUILDING_HUG, 1),
    ItemPool.get(ItemPool.EXPLODING_KICKBALL, 1),

    // Currency & items from ChemiCorp
    ItemPool.get(ItemPool.DANGEROUS_CHEMICALS, 1),
    ItemPool.get(ItemPool.EXPERIMENTAL_GENE_THERAPY, 1),
    ItemPool.get(ItemPool.ULTRACOAGULATOR, 1),

    // Currency & items from GotPork P.D.
    ItemPool.get(ItemPool.INCRIMINATING_EVIDENCE, 1),
    ItemPool.get(ItemPool.SELF_DEFENSE_TRAINING, 1),
    ItemPool.get(ItemPool.FINGERPRINT_DUSTING_KIT, 1),

    // Bat-Suit upgrade
    ItemPool.get(ItemPool.BAT_AID_BANDAGE, 1),

    // Bat-Sedan upgrade
    ItemPool.get(ItemPool.BAT_BEARING, 1),

    // Bat-Cavern upgrade
    ItemPool.get(ItemPool.GLOB_OF_BAT_GLUE, 1),
  };

  // Bat-Suit Upgrades: whichchoice = 1137
  private static final BatUpgrade[] BAT_SUIT_UPGRADES = {
    new BatUpgrade(1, "Hardened Knuckles", "Doubles the damage of Bat-Punches"),
    new BatUpgrade(2, "Steel-Toed Bat-Boots", "Doubles the damage of Bat-Kicks"),
    new BatUpgrade(3, "Extra-Swishy Cloak", "Lets you strike first in combats"),
    new BatUpgrade(4, "Pec-Guards", "Reduces the damage you take from melee attacks"),
    new BatUpgrade(5, "Kevlar Undergarments", "Reduces the damage you take from gunshots"),
    new BatUpgrade(6, "Improved Cowl Optics", "Lets you find more items and hidden things"),
    new BatUpgrade(7, "Asbestos Lining", "Provides resistance to Hot damage"),
    new BatUpgrade(8, "Utility Belt First Aid Kit", "Contains bandages (in theory)"),
  };

  public static final BatUpgrade HARDENED_KNUCKLES =
      BatManager.findOption(BAT_SUIT_UPGRADES, "Hardened Knuckles");
  public static final BatUpgrade STEEL_TOED_BAT_BOOTS =
      BatManager.findOption(BAT_SUIT_UPGRADES, "Steel-Toed Bat-Boots");
  public static final BatUpgrade PEC_GUARDS =
      BatManager.findOption(BAT_SUIT_UPGRADES, "Pec-Guards");
  public static final BatUpgrade KEVLAR_UNDERGARMENTS =
      BatManager.findOption(BAT_SUIT_UPGRADES, "Kevlar Undergarments");
  public static final BatUpgrade ASBESTOS_LINING =
      BatManager.findOption(BAT_SUIT_UPGRADES, "Asbestos Lining");

  // Bat-Sedan Upgrades: whichchoice = 1138
  private static final BatUpgrade[] BAT_SEDAN_UPGRADES = {
    new BatUpgrade(1, "Rocket Booster", "Reduce travel time by 5 minutes"),
    new BatUpgrade(2, "Glove Compartment First-Aid Kit", "Restore your health on the go!"),
    new BatUpgrade(3, "Street Sweeper", "Gather evidence as you drive around"),
    new BatUpgrade(4, "Advanced Air Filter", "Gather dangerous chemicals as you drive around"),
    new BatUpgrade(5, "Orphan Scoop", "Rescue loose orphans as you drive around"),
    new BatUpgrade(6, "Spotlight", "Helps you find your way through villains' lairs"),
    new BatUpgrade(7, "Bat-Freshener", "Provides resistance to Stench damage"),
    new BatUpgrade(8, "Loose Bearings", "Bearings will periodically fall out of the car."),
  };

  public static final BatUpgrade SPOTLIGHT = BatManager.findOption(BAT_SEDAN_UPGRADES, "Spotlight");
  public static final BatUpgrade BAT_FRESHENER =
      BatManager.findOption(BAT_SEDAN_UPGRADES, "Bat-Freshener");

  // Bat-Cavern Upgrades: whichchoice = 1139
  private static final BatUpgrade[] BAT_CAVERN_UPGRADES = {
    new BatUpgrade(1, "Really Long Winch", "Traveling to the Bat-Cavern is instantaneous"),
    new BatUpgrade(2, "Improved 3-D Bat-Printer", "Reduce materials cost in the Bat-Fabricator"),
    new BatUpgrade(3, "Transfusion Satellite", "Remotely restore some of your HP after fights"),
    new BatUpgrade(4, "Surveillance Network", "Fights take 1 minute less"),
    new BatUpgrade(5, "Blueprints Database", "Make faster progress through villain lairs"),
    new BatUpgrade(7, "Snugglybear Nightlight", "Provides resistance to Spooky damage"),
    new BatUpgrade(8, "Glue Factory", "An automated mail-order glue factory"),
  };

  public static final BatUpgrade IMPROVED_3D_BAT_PRINTER =
      BatManager.findOption(BAT_CAVERN_UPGRADES, "Improved 3-D Bat-Printer");
  public static final BatUpgrade TRANSFUSION_SATELLITE =
      BatManager.findOption(BAT_CAVERN_UPGRADES, "Transfusion Satellite");
  public static final BatUpgrade BLUEPRINTS_DATABASE =
      BatManager.findOption(BAT_CAVERN_UPGRADES, "Blueprints Database");
  public static final BatUpgrade SNUGGLYBEAR_NIGHTLIGHT =
      BatManager.findOption(BAT_CAVERN_UPGRADES, "Snugglybear Nightlight");

  private static BatUpgrade findOption(final BatUpgrade[] upgrades, final int option) {
    for (BatUpgrade upgrade : upgrades) {
      if (upgrade.option == option) {
        return upgrade;
      }
    }
    return null;
  }

  private static BatUpgrade findOption(final BatUpgrade[] upgrades, final String name) {
    for (BatUpgrade upgrade : upgrades) {
      if (upgrade.name.equals(name)) {
        return upgrade;
      }
    }
    return null;
  }

  private static void addUpgrade(final BatUpgrade newUpgrade) {
    if (BatManager.hasUpgrade(newUpgrade)) {
      return;
    }

    BatManager.upgrades.add(newUpgrade);

    StringBuilder buffer = new StringBuilder();
    String separator = "";
    for (BatUpgrade upgrade : BatManager.upgrades) {
      buffer.append(separator);
      buffer.append(upgrade.name);
      separator = ";";
    }
    Preferences.setString("batmanUpgrades", buffer.toString());

    if (BatManager.DwayneCoFunds > 0) {
      Preferences.setInteger("batmanFundsAvailable", --BatManager.DwayneCoFunds);
    }
  }

  public static void batSuitUpgrade(final int option, final String text) {
    BatUpgrade upgrade = BatManager.findOption(BAT_SUIT_UPGRADES, option);
    if (upgrade != null && !BatManager.hasUpgrade(upgrade)) {
      BatManager.addUpgrade(upgrade);
      if (upgrade == BatManager.HARDENED_KNUCKLES) {
        BatManager.stats.set("Bat-Punch Multiplier", 2);
      } else if (upgrade == BatManager.STEEL_TOED_BAT_BOOTS) {
        BatManager.stats.set("Bat-Kick Multiplier", 2);
      } else if (upgrade == BatManager.PEC_GUARDS) {
        BatManager.stats.increment("Bat-Armor", 3);
      } else if (upgrade == BatManager.KEVLAR_UNDERGARMENTS) {
        BatManager.stats.increment("Bat-Bulletproofing", 3);
      } else if (upgrade == BatManager.ASBESTOS_LINING) {
        BatManager.stats.increment("Bat-Heat Resistance", 50);
      }
    }
  }

  public static void batSedanUpgrade(final int option, final String text) {
    BatUpgrade upgrade = BatManager.findOption(BAT_SEDAN_UPGRADES, option);
    if (upgrade != null && !BatManager.hasUpgrade(upgrade)) {
      BatManager.addUpgrade(upgrade);
      if (upgrade == BatManager.SPOTLIGHT) {
        BatManager.stats.increment("Bat-Investigation Progress", 1);
      } else if (upgrade == BatManager.BAT_FRESHENER) {
        BatManager.stats.increment("Bat-Stench Resistance", 50);
      }
    }
  }

  public static void batCavernUpgrade(final int option, final String text) {
    BatUpgrade upgrade = BatManager.findOption(BAT_CAVERN_UPGRADES, option);
    if (upgrade != null && !BatManager.hasUpgrade(upgrade)) {
      BatManager.addUpgrade(upgrade);
      if (upgrade == BatManager.SNUGGLYBEAR_NIGHTLIGHT) {
        BatManager.stats.increment("Bat-Spooky Resistance", 50);
      } else if (upgrade == BatManager.BLUEPRINTS_DATABASE) {
        BatManager.stats.increment("Bat-Investigation Progress", 1);
      } else if (upgrade == BatManager.TRANSFUSION_SATELLITE) {
        BatManager.stats.increment("Bat-Health Regeneration", 5);
      }
    }
  }

  public static boolean hasUpgrade(final BatUpgrade upgrade) {
    return BatManager.upgrades.contains(upgrade);
  }

  private static void reset(final boolean active) {
    // Zero out Time until Gotpork City explodes
    Preferences.setInteger("batmanTimeLeft", 0);
    BatManager.BatMinutes = 0;

    // Zero out DwayneCo funds
    Preferences.setInteger("batmanFundsAvailable", 0);
    BatManager.DwayneCoFunds = 0;
    // (haven't seen the charpane yet, so assume it is what we saw last time we started.)
    BatManager.DwayneCoBonusFunds = Preferences.getInteger("batmanBonusInitialFunds");

    // Reset Bat-Stats
    Preferences.setString("batmanStats", "");
    BatManager.stats.reset(active);

    // Clear Bat-Upgrades
    Preferences.setString("batmanUpgrades", "");
    BatManager.upgrades.clear();

    // Clean up inventory
    BatManager.resetItems();

    // You are somewhere in Gotpork City
    BatManager.setBatZone(BatManager.GOTPORK_CITY);

    AdventureSpentDatabase.setTurns("Center Park After Dark", 0);
    AdventureSpentDatabase.setTurns("The Mean Streets", 0);
    AdventureSpentDatabase.setTurns("Warehouse Row", 0);
    AdventureSpentDatabase.setTurns("Gotpork Conservatory of Flowers", 0);
    AdventureSpentDatabase.setTurns("Gotpork Municipal Reservoir", 0);
    AdventureSpentDatabase.setTurns("Gotpork Gardens Cemetery", 0);
    AdventureSpentDatabase.setTurns("Gotpork City Sewers", 0);
    AdventureSpentDatabase.setTurns("Porkham Asylum", 0);
    AdventureSpentDatabase.setTurns("The Old Gotpork Library", 0);
    AdventureSpentDatabase.setTurns("Gotpork Clock, Inc.", 0);
    AdventureSpentDatabase.setTurns("Gotpork Foundry", 0);
    AdventureSpentDatabase.setTurns("Trivial Pursuits, LLC", 0);
  }

  public static void begin() {
    BatManager.reset(true);

    // Add items that you begin with
    ResultProcessor.processItem(ItemPool.BAT_OOMERANG, 1);
    ResultProcessor.processItem(ItemPool.BAT_JUTE, 1);
    ResultProcessor.processItem(ItemPool.BAT_O_MITE, 1);

    // You start with 10 h. 0 m.
    Preferences.setInteger("batmanTimeLeft", 600);
    BatManager.BatMinutes = 600;

    // You start with 3 billions + 1 billion per run
    BatManager.DwayneCoFunds = 3 + BatManager.DwayneCoBonusFunds;

    // You start in the Bat-Cavern
    BatManager.setBatZone(BatManager.BAT_CAVERN);

    // You can use Batfellow combat skills.
    BatManager.setCombatSkills();
  }

  public static void setCombatSkills() {
    KoLCharacter.addAvailableCombatSkill("Bat-Punch");
    KoLCharacter.addAvailableCombatSkill("Bat-Kick");
    KoLCharacter.addAvailableCombatSkill("Bat-oomerang");
    KoLCharacter.addAvailableCombatSkill("Bat-Jute");
    KoLCharacter.addAvailableCombatSkill("Bat-o-mite");
    KoLCharacter.addAvailableCombatSkill("Ultracoagulator");
    KoLCharacter.addAvailableCombatSkill("Kickball");
    KoLCharacter.addAvailableCombatSkill("Bat-Glue");
    KoLCharacter.addAvailableCombatSkill("Bat-Bearing");
    KoLCharacter.addAvailableCombatSkill("Use Bat-Aid");
  }

  public static void end() {
    BatManager.reset(false);

    // You can no longer use Batfellow combat skills.
    KoLCharacter.removeAvailableCombatSkill("Bat-Punch");
    KoLCharacter.removeAvailableCombatSkill("Bat-Kick");
    KoLCharacter.removeAvailableCombatSkill("Bat-oomerang");
    KoLCharacter.removeAvailableCombatSkill("Bat-Jute");
    KoLCharacter.removeAvailableCombatSkill("Bat-o-mite");
    KoLCharacter.removeAvailableCombatSkill("Ultracoagulator");
    KoLCharacter.removeAvailableCombatSkill("Kickball");
    KoLCharacter.removeAvailableCombatSkill("Bat-Glue");
    KoLCharacter.removeAvailableCombatSkill("Bat-Bearing");
    KoLCharacter.removeAvailableCombatSkill("Use Bat-Aid");
  }

  private static void resetItems() {
    for (AdventureResult item : BatManager.ITEMS) {
      int count = item.getCount(KoLConstants.inventory);
      if (count > 0) {
        AdventureResult result = item.getInstance(-count);
        AdventureResult.addResultToList(KoLConstants.inventory, result);
        AdventureResult.addResultToList(KoLConstants.tally, result);
      }
    }
  }

  // <a target=mainpane href=place.php?whichplace=batman_cave>
  public static final Pattern ZONE_PATTERN = Pattern.compile("whichplace=(.*?.php)");

  public static void parseTopMenu(final String responseText) {
    Matcher matcher = BatManager.ZONE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      BatManager.newBatZone(BatManager.placeToBatZone(matcher.group(1)));
    }
  }

  public static void parsePlaceResponse(final String urlString, final String responseText) {
    String zone =
        urlString.contains("batman_cave")
            ? BatManager.BAT_CAVERN
            : urlString.contains("batman_downtown")
                ? BatManager.DOWNTOWN
                : urlString.contains("batman_park")
                    ? BatManager.CENTER_PARK
                    : urlString.contains("batman_slums")
                        ? BatManager.SLUMS
                        : urlString.contains("batman_industrial")
                            ? BatManager.INDUSTRIAL_DISTRICT
                            : GOTPORK_CITY;
    BatManager.newBatZone(zone);
  }

  // <td><img src=http://images.kingdomofloathing.com/itemimages/watch.gif alt='Time until Gotpork
  // City explodes' title='Time until Gotpork City explodes'></td><td valign=center><font
  // face=arial>10 h. 0 m.</td>
  // <td><img src=http://images.kingdomofloathing.com/itemimages/watch.gif alt='Time until Gotpork
  // City explodes' title='Time until Gotpork City explodes'></td><td valign=center><font
  // face=arial>8 m.</td>
  public static final Pattern TIME_PATTERN =
      Pattern.compile(
          "Time until Gotpork City explodes.*?<font face=arial>(?:<font color=red>)?(?:(\\d+) h. )?(\\d+) m.<");

  // <td><img src=http://images.kingdomofloathing.com/itemimages/dollarsign.gif alt='DwayneCo funds'
  // title='DwayneCo funds'></td><td valign=center><font face=arial>4 bn.</font></td>
  public static final Pattern FUNDS_PATTERN =
      Pattern.compile("DwayneCo funds.*?<font face=arial>(\\d+) bn.<");

  // <td><img src=http://images.kingdomofloathing.com/itemimages/hp.gif alt='Bat-Health'
  // title='Bat-Health'></td><td valign=center><font face=arial>30 / 30</font></td>
  private static final Pattern HP_PATTERN =
      Pattern.compile("Bat-Health.*?<font face=arial>(\\d+) / (\\d+)<");

  public static void parseCharpane(final String responseText) {
    if (!responseText.contains("You're Batfellow")) {
      return;
    }

    CharPaneRequest.parseAvatar(responseText);

    Matcher matcher = BatManager.TIME_PATTERN.matcher(responseText);
    if (matcher.find()) {
      String hourString = matcher.group(1);
      String minuteString = matcher.group(2);
      int hours = hourString == null ? 0 : StringUtilities.parseInt(hourString);
      int minutes = StringUtilities.parseInt(minuteString);
      BatManager.BatMinutes = (hours * 60) + minutes;
      Preferences.setInteger("batmanTimeLeft", BatManager.BatMinutes);
    }

    matcher = BatManager.HP_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int currentHP = StringUtilities.parseInt(matcher.group(1));
      int maximumHP = StringUtilities.parseInt(matcher.group(2));
      KoLCharacter.setHP(currentHP, maximumHP, maximumHP);
      BatManager.stats.set("Bat-Health", currentHP);
      BatManager.stats.set("Maximum Bat-Health", maximumHP);
    }

    matcher = BatManager.FUNDS_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int funds = StringUtilities.parseInt(matcher.group(1));
      BatManager.DwayneCoFunds = funds;
      Preferences.setInteger("batmanFundsAvailable", funds);

      // possibly learn bonus fund amount
      int bonus = funds - 3;
      if (bonus > BatManager.DwayneCoBonusFunds) {
        BatManager.DwayneCoBonusFunds = bonus;
        Preferences.setInteger("batmanBonusInitialFunds", bonus);
      }
    }

    // Current Bat-Tasks:
    //
    // Learn the Jokester's access code:<br>&nbsp;&nbsp;&nbsp;<font size=+2><b>*********</b></font>
    // Track down Kudzu<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down Mansquito<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down Miss Graves<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down The Plumber<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down The Author<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down The Mad-Libber<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down Doc Clock<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down Mr. Burns<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
    // Track down The Inquisitor<font size=1><br>&nbsp;&nbsp;(0% progress)
    //
    // Defeat Kudzu
  }

  public static String currentBatZone() {
    return BatManager.zone;
  }

  public static String placeToBatZone(final String place) {
    return place.equals("batman_cave")
        ? BatManager.BAT_CAVERN
        : place.equals("batman_downtown")
            ? BatManager.DOWNTOWN
            : place.equals("batman_park")
                ? BatManager.CENTER_PARK
                : place.equals("batman_slums")
                    ? BatManager.SLUMS
                    : place.equals("batman_industrial") ? BatManager.INDUSTRIAL_DISTRICT : null;
  }

  // choice.php?whichchoice=1135&option=1
  public static final Pattern MAP_PATTERN =
      Pattern.compile("choice.php\\?whichchoice=1135&option=(\\d+)");

  public static String parseBatSedan(final String responseText) {
    // We can tell where we are by looking at the map
    boolean cavern = false;
    boolean park = false;
    boolean slums = false;
    boolean industrial = false;
    boolean downtown = false;

    Matcher matcher = BatManager.MAP_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int zone = StringUtilities.parseInt(matcher.group(1));
      switch (zone) {
        case 1:
          cavern = true;
          break;
        case 2:
          downtown = true;
          break;
        case 3:
          slums = true;
          break;
        case 4:
          industrial = true;
          break;
        case 5:
          park = true;
          break;
        case 9:
          // Eject
          break;
      }
    }

    String zone =
        !cavern
            ? BatManager.BAT_CAVERN
            : !downtown
                ? BatManager.DOWNTOWN
                : !park
                    ? BatManager.CENTER_PARK
                    : !slums
                        ? BatManager.SLUMS
                        : !industrial ? BatManager.INDUSTRIAL_DISTRICT : GOTPORK_CITY;
    BatManager.setBatZone(zone);

    return null;
  }

  private static void setBatZone(final String zone) {
    BatManager.zone = zone;
    Preferences.setString("batmanZone", zone);
    NamedListenerRegistry.fireChange("(batfellow)");
  }

  public static void newBatZone(final String zone) {
    if (zone != BatManager.currentBatZone()) {
      String message = "Drive to " + zone;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      BatManager.setBatZone(zone);
    }
  }

  public static void gainItem(final AdventureResult item) {
    switch (item.getItemId()) {
      case ItemPool.EXPERIMENTAL_GENE_THERAPY:
        BatManager.stats.increment("Maximum Bat-Health", 10);
        break;

      case ItemPool.SELF_DEFENSE_TRAINING:
        BatManager.stats.increment("Bat-Armor", 1);
        break;

      case ItemPool.CONFIDENCE_BUILDING_HUG:
        BatManager.stats.increment("Bat-Punch Modifier", 1);
        BatManager.stats.increment("Bat-Kick Modifier", 1);
        break;
    }
  }

  public static void changeBatHealth(AdventureResult result) {
    BatManager.stats.increment("Bat-Health", result.getCount());
  }

  public static void wonFight(final String monsterName, final String responseText) {
    // Low Crime zones
    if (monsterName.equals("vicious plant creature")) {
      if (responseText.contains("(+1 Bat-Health regeneration per fight)")) {
        BatManager.stats.increment("Bat-Health Regeneration", 1);
      }
    } else if (monsterName.equals("giant mosquito")) {
      if (responseText.contains("(+3 Maximum Bat-Health)")) {
        BatManager.stats.increment("Maximum Bat-Health", 3);
      }
    } else if (monsterName.equals("walking skeleton")) {
      if (responseText.contains("(+1 Bat-Armor)")) {
        BatManager.stats.increment("Bat-Armor", 1);
      }
    }
    // Medium Crime zones
    else if (monsterName.equals("former guard")) {
      if (responseText.contains("(+1 Bat-Bulletproofing)")) {
        BatManager.stats.increment("Bat-Bulletproofing", 1);
      }
    } else if (monsterName.equals("plumber's helper")) {
      if (responseText.contains("(+10% Bat-Stench Resistance)")) {
        BatManager.stats.increment("Bat-Stench Resistance", 10);
      }
    } else if (monsterName.equals("very [adjective] henchman")) {
      if (responseText.contains("(+10% Bat-Spooky Resistance)")) {
        BatManager.stats.increment("Bat-Spooky Resistance", 10);
      }
    }
    // High Crime zones
    if (monsterName.equals("time bandit")) {
      if (responseText.contains("(+10 Bat-Minutes)")) {
        BatManager.BatMinutes += 10;
      }
    }
    if (monsterName.equals("burner")) {
      if (responseText.contains("(+10% Bat-Heat Resistance)")) {
        BatManager.stats.increment("Bat-Heat Resistance", 10);
      }
    }
    if (monsterName.equals("inquisitee")) {
      if (responseText.contains("(+1% Investigation Progress per fight)")) {
        BatManager.stats.increment("Bat-Investigation Progress", 1);
      }
    }

    // (+3% Bat-Progress) or (+4% Bat-Progress)
  }

  public static int getTimeLeft() {
    // Return minutes left: 0 - 600
    return BatManager.BatMinutes;
  }

  public static String getTimeLeftString() {
    int minutes = BatManager.getTimeLeft();
    StringBuilder buffer = new StringBuilder();
    int hours = minutes / 60;
    if (hours > 0) {
      buffer.append(hours);
      buffer.append(" h. ");
      minutes = minutes % 60;
    }
    buffer.append(minutes);
    buffer.append(" m.");
    return buffer.toString();
  }

  private static class BatStats {
    // Bat-Health
    public int BatHealth = BASE_BAT_HEALTH;
    public int MaximumBatHealth = BASE_BAT_HEALTH;
    public int BatHealthRegeneration = 0;

    // Bat-Punch
    public int BatPunch = BASE_BAT_PUNCH;
    public int BatPunchModifier = 0;
    public int BatPunchMultiplier = 1;

    // Bat-Kick
    public int BatKick = BASE_BAT_KICK;
    public int BatKickModifier = 0;
    public int BatKickMultiplier = 1;

    // Bat-Armor
    public int BatArmor = BASE_BAT_ARMOR;

    // Bat-Bulletproofing
    public int BatBulletproofing = BASE_BAT_BULLETPROOFING;

    // Bat-Spooky Resistance
    public int BatSpookyResistance = BASE_BAT_SPOOKY_RESISTANCE;

    // Bat-Heat Resistance
    public int BatHeatResistance = BASE_BAT_HEAT_RESISTANCE;

    // Bat-Stench Resistance
    public int BatStenchResistance = BASE_BAT_STENCH_RESISTANCE;

    // Bat-Investigation Progress
    public int BatInvestigationProgress = BASE_BAT_INVESTIGATION_PROGRESS;

    public String stringform = "";

    public BatStats() {
      this.reset(false);
    }

    public void reset(final boolean active) {
      this.BatHealth = BASE_BAT_HEALTH;
      this.MaximumBatHealth = BASE_BAT_HEALTH;
      this.BatHealthRegeneration = 0;
      this.BatPunch = BASE_BAT_PUNCH;
      this.BatPunchModifier = 0;
      this.BatPunchMultiplier = 1;
      this.BatKick = BASE_BAT_KICK;
      this.BatKickModifier = 0;
      this.BatKickMultiplier = 1;
      this.BatArmor = BASE_BAT_ARMOR;
      this.BatBulletproofing = BASE_BAT_BULLETPROOFING;
      this.BatSpookyResistance = BASE_BAT_SPOOKY_RESISTANCE;
      this.BatHeatResistance = BASE_BAT_HEAT_RESISTANCE;
      this.BatStenchResistance = BASE_BAT_STENCH_RESISTANCE;
      this.BatInvestigationProgress = BASE_BAT_INVESTIGATION_PROGRESS;
      this.calculateStringform(active);
    }

    public int get(final String name) {
      if (name.equals("Bat-Health")) {
        return this.BatHealth;
      }
      if (name.equals("Maximum Bat-Health")) {
        return this.MaximumBatHealth;
      }
      if (name.equals("Bat-Health Regeneration")) {
        return this.BatHealthRegeneration;
      }
      if (name.equals("Bat-Punch")) {
        return this.BatPunch;
      }
      if (name.equals("Bat-Punch Modifier")) {
        return this.BatPunchModifier;
      }
      if (name.equals("Bat-Punch Multiplier")) {
        return this.BatPunchMultiplier;
      }
      if (name.equals("Bat-Kick")) {
        return this.BatKick;
      }
      if (name.equals("Bat-Kick Modifier")) {
        return this.BatKickModifier;
      }
      if (name.equals("Bat-Kick Multiplier")) {
        return this.BatKickMultiplier;
      }
      if (name.equals("Bat-Armor")) {
        return this.BatArmor;
      }
      if (name.equals("Bat-Bulletproofing")) {
        return this.BatBulletproofing;
      }
      if (name.equals("Bat-Spooky Resistance")) {
        return this.BatSpookyResistance;
      }
      if (name.equals("Bat-Heat Resistance")) {
        return this.BatHeatResistance;
      }
      if (name.equals("Bat-Stench Resistance")) {
        return this.BatStenchResistance;
      }
      if (name.equals("Bat-Investigation Progress")) {
        return this.BatInvestigationProgress;
      }
      return 0;
    }

    public int set(final String name, final int value) {
      int current = 0;
      if (name.equals("Bat-Health")) {
        current = this.BatHealth;
        this.BatHealth = value;
      } else if (name.equals("Maximum Bat-Health")) {
        current = this.MaximumBatHealth;
        this.MaximumBatHealth = value;
      } else if (name.equals("Bat-Health Regeneration")) {
        current = this.BatHealthRegeneration;
        this.BatHealthRegeneration = value;
      } else if (name.equals("Bat-Punch")) {
        current = this.BatPunch;
        this.BatPunch = value;
      } else if (name.equals("Bat-Punch Modifier")) {
        current = this.BatPunchModifier;
        this.BatPunchModifier = value;
      } else if (name.equals("Bat-Punch Multiplier")) {
        current = this.BatPunchMultiplier;
        this.BatPunchMultiplier = value;
      } else if (name.equals("Bat-Kick")) {
        current = this.BatKick;
        this.BatKick = value;
      } else if (name.equals("Bat-Kick Modifier")) {
        current = this.BatKickModifier;
        this.BatKickModifier = value;
      } else if (name.equals("Bat-Kick Multiplier")) {
        current = this.BatKickMultiplier;
        this.BatKickMultiplier = value;
      } else if (name.equals("Bat-Armor")) {
        current = this.BatArmor;
        this.BatArmor = value;
      } else if (name.equals("Bat-Bulletproofing")) {
        current = this.BatBulletproofing;
        this.BatBulletproofing = value;
      } else if (name.equals("Bat-Spooky Resistance")) {
        current = this.BatSpookyResistance;
        this.BatSpookyResistance = value;
      } else if (name.equals("Bat-Heat Resistance")) {
        current = this.BatHeatResistance;
        this.BatHeatResistance = value;
      } else if (name.equals("Bat-Stench Resistance")) {
        current = this.BatStenchResistance;
        this.BatStenchResistance = value;
      } else if (name.equals("Bat-Investigation Progress")) {
        current = this.BatInvestigationProgress;
        this.BatInvestigationProgress = value;
      } else {
        return 0;
      }

      if (current != value) {
        this.calculateStringform(true);
      }

      return value;
    }

    public int increment(final String name, final int delta) {
      int current = this.get(name);
      return delta == 0 ? current : this.set(name, current + delta);
    }

    private void appendStat(StringBuilder buffer, String tag, int stat) {
      if (buffer.length() > 0) {
        buffer.append(";");
      }
      buffer.append(tag);
      buffer.append("=");
      buffer.append(stat);
    }

    private void calculateStringform(final boolean active) {
      StringBuilder buffer = new StringBuilder();

      this.appendStat(buffer, "Bat-Health", this.BatHealth);
      this.appendStat(buffer, "Maximum Bat-Health", this.MaximumBatHealth);
      this.appendStat(buffer, "Bat-Health Regeneration", this.BatHealthRegeneration);
      this.appendStat(buffer, "Bat-Punch", this.BatPunch);
      this.appendStat(buffer, "Bat-Punch Modifier", this.BatPunchModifier);
      this.appendStat(buffer, "Bat-Punch Multiplier", this.BatPunchMultiplier);
      this.appendStat(buffer, "Bat-Kick", this.BatKick);
      this.appendStat(buffer, "Bat-Kick Modifier", this.BatKickModifier);
      this.appendStat(buffer, "Bat-Kick Multiplier", this.BatKickMultiplier);
      this.appendStat(buffer, "Bat-Armor", this.BatArmor);
      this.appendStat(buffer, "Bat-Bulletproofing", this.BatBulletproofing);
      this.appendStat(buffer, "Bat-Spooky Resistance", this.BatSpookyResistance);
      this.appendStat(buffer, "Bat-Heat Resistance", this.BatHeatResistance);
      this.appendStat(buffer, "Bat-Stench Resistance", this.BatStenchResistance);
      this.appendStat(buffer, "Bat-Investigation Progress", this.BatInvestigationProgress);

      this.stringform = buffer.toString();

      if (active) {
        Preferences.setString("batmanStats", this.stringform);
      }
    }

    @Override
    public String toString() {
      return this.stringform;
    }
  }

  private static class BatUpgrade implements Comparable<BatUpgrade> {
    public final int option;
    public final String name;
    public final String description;

    public BatUpgrade(final int option, final String name, final String description) {
      this.option = option;
      this.name = name;
      this.description = description;
    }

    @Override
    public int compareTo(final BatUpgrade that) {
      return this.name.compareTo(that.name);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
