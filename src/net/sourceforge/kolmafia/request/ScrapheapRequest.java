package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ScrapheapRequest extends PlaceRequest {
  public ScrapheapRequest() {
    super("scrapheap");
  }

  public ScrapheapRequest(final String action) {
    super("scrapheap", action);
  }

  public static void refresh() {
    if (KoLCharacter.inRobocore()) {
      RequestThread.postRequest(new ScrapheapRequest());
    }
  }

  @Override
  public void processResults() {
    ScrapheapRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    if (action == null || action.startsWith("sh_chrono")) {
      parseChronolith(responseText);
      return;
    }

    if (action.startsWith("sh_getpower")) {
      parseCollectEnergy(responseText);
      return;
    }

    if (action.startsWith("sh_scrounge")) {
      Preferences.setBoolean("youRobotScavenged", true);
      return;
    }
  }

  private static final Pattern CHRONOLITH_COST = Pattern.compile("title=\"\\((\\d+) Energy\\)\"");

  private static void parseChronolith(final String responseText) {
    Matcher m = CHRONOLITH_COST.matcher(responseText);

    if (m.find()) {
      int cost = StringUtilities.parseInt(m.group(1));

      if (cost > 148) {
        cost /= 10;
      } else if (cost > 47) {
        cost /= 2;
      }

      cost -= 10;

      Preferences.setInteger("_chronolithActivations", cost);
    }
  }

  private static final Pattern ENERGY_GAIN = Pattern.compile("You gain (\\d+) Energy.");

  private static void parseCollectEnergy(final String responseText) {
    Matcher m = ENERGY_GAIN.matcher(responseText);

    if (m.find()) {
      // Progression went 25, 21, 18, 13, 11, 10, 8, 6 ,6 ,5?
      // int gain = StringUtilities.parseInt( m.group( 1 ) );
      Preferences.increment("_energyCollected");
    }
  }

  private static final Pattern STATBOT_COST =
      Pattern.compile("Current upgrade cost: <b>(\\d+) energy</b>");

  public static void parseStatbotCost(final String responseText) {
    Matcher m = STATBOT_COST.matcher(responseText);

    if (m.find()) {
      int cost = StringUtilities.parseInt(m.group(1));
      Preferences.setInteger("statbotUses", cost - 10);
    }
  }

  //        Top             Left               Right               Bottom
  // 1 = Pea Shooter    Pound-O-Tron       Slab-O-Matic         Bald Tires
  // 2 = Bird Cage      Reflective Shard   Junk Shield          Rocket Crotch
  // 3 = Solar Panel    Metal Detector     Horseshoe Magnet     Motorcycle Wheel
  // 4 = Mannequin Head Vice Grips         Omni-Claw            Robo-Legs
  // 5 = Meat Radar     Sniper Rifle       Mammal Prod          Magno-Lev
  // 6 = Junk Cannon    Junk Mace          Solenoid Piston      Tank Treads
  // 7 = Tesla Blaster  Camouflage Curtain Blaring Speaker      Snowplow
  // 8 = Snow Blower    Grease Gun         Surplus Flamethrower

  // Pound-O-Tron -> Swing Pound-O-Tron
  // Pea Shooter -> Shoot Pea
  // Rocket Crotch -> Crotch Burn
  // Junk Cannon -> Junk Blast
  // Tesla Blaster -> Tesla Blast
  // Sniper Rifle -> Snipe
  // Junk Mace -> Junk Mace Smash
  // Mammal Prod -> Prod
  // Solenoid Piston -> Solenoid Slam
  // Snowblower -> Blow Snow
  // Surplus Flamethrower -> Throw Flame
  // Grease Gun -> Shoot Grease

  // Bird Cage - can use familiars
  // Mannequin Head - can equip hats
  // Vice Grips -> can equip (1 or 2 handed) weapons
  // Omni-Claw -> can equip offhand items (unless 2-handed weapon in Vice Grips)
  // Robo Legs -> can equip pants

  // Solar Panel -> Energy: +1
  // Meat Radar -> Meat Drop: +50
  // Reflective Shard -> Resist All: +3
  // Metal Detector -> Item Drop: +30
  // Camouflage Curtain -> Combat Rate: -15
  // Slab-O-Matic -> Maximum HP: +30
  // Junk Shield -> Damage Reduction: +10, Damage Absorption: +50
  // Horseshoe Magnet -> Scrap: +1
  // Blaring Speakers -> Monster Level: +30
  // Bald Tires -> Maximum HP: +10
  // Motorcycle Wheels -> Initiative: +30
  // Magno-Lex -> Item Drop: +30
  // Tank Treads -> Maximum HP: +50, Damage Reduction: +10
  // Snowplow -> Scrap: +1

  private static final Pattern CONFIGURATION =
      Pattern.compile("robot/(left|right|top|bottom|body)(\\d+).png\"");

  public static void parseConfiguration(final String text) {
    Matcher m = CONFIGURATION.matcher(text);

    while (m.find()) {
      String section = m.group(1);
      int config = StringUtilities.parseInt(m.group(2));
      Preferences.setInteger("youRobot" + StringUtilities.toTitleCase(section), config);
    }
  }

  // robot_muscle -> Leverage Coprocessing -> Muscle: +15
  // robot_mysticality -> Dynamic Arcane Flux Modeling -> Mysticality: +15
  // robot_moxie -> Upgraded Fashion Sensor -> Moxie: +15
  // robot_meat -> Finance Neural Net -> Meat Drop: +20
  // robot_hp1 -> Spatial Compression Functions -> Maximum HP: +30
  // robot_regen -> Self-Repair Routines -> HP Regen Min: +10, HP Regen Max: +10
  // robot_resist -> Weather Control Algorithms -> Resist All: +2
  // robot_items -> Improved Optical Processing -> Item Drop: +20
  // robot_shirt -> Topology Grid -> can equip shirts
  // robot_energy -> Overclocking -> Energy: +1
  // robot_potions -> Biomass Processing Function -> can use potions
  // robot_hp2 -> Holographic Deflector Projection -> Maximum HP: +30

  private static final Pattern CPU_UPGRADE_INSTALLED =
      Pattern.compile("<button.*?value=\"([a-z0-9_]+)\"[^\\(]+\\(already installed\\)");

  public static void parseCPUUpgrades(final String text) {
    List<String> cpuUpgrades = new ArrayList<>();
    Matcher m = CPU_UPGRADE_INSTALLED.matcher(text);

    while (m.find()) {
      cpuUpgrades.add(m.group(1));
    }

    Preferences.setString("youRobotCPUUpgrades", String.join(",", cpuUpgrades));
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php") || !urlString.contains("whichplace=scrapheap")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      // Nothing to log for simple visits
      return true;
    }

    String message = null;

    if (action.startsWith("sh_chrono")) {
      message = "Activating the Chronolith";
    }
    if (action.startsWith("sh_upgrade")) {
      return true;
    }
    if (action.startsWith("sh_getpower")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Collecting energy";
    }
    if (action.startsWith("sh_scrounge")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Scavenging scrap";
    } else if (action.startsWith("sh_configure")) {
      return true;
    }

    if (message == null) {
      // Log URL for anything else
      return false;
    }

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
