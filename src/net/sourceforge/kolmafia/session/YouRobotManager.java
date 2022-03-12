package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class YouRobotManager {

  public enum Part {
    TOP("top", "Top Attachment"),
    LEFT("left", "Left Arm"),
    RIGHT("right", "Right Arm"),
    BOTTOM("bottom", "Propulsion System"),
    CPU("cpus", "CPU Upgrade");

    String keyword;
    String name;

    Part(String keyword, String name) {
      this.keyword = keyword;
      this.name = name;
    }

    String getKeyword() {
      return this.keyword;
    }

    String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static Map<String, Part> keywordToPart = new HashMap<>();

  static {
    for (Part part : Part.values()) {
      keywordToPart.put(part.getKeyword(), part);
    }
  }

  public enum Effect {
    PASSIVE,
    COMBAT,
    EQUIP;
  }

  public enum Usable {
    NONE("no special effect"),
    HAT("can equip hats", EquipmentManager.HAT),
    WEAPON("can equip weapons", EquipmentManager.WEAPON),
    OFFHAND("can equip offhands", EquipmentManager.OFFHAND),
    SHIRT("can equip shirts", EquipmentManager.SHIRT),
    PANTS("can equip pants", EquipmentManager.PANTS),
    FAMILIAR("can use familiars"),
    POTIONS("can use potions");

    String description;
    int slot;

    Usable(String description) {
      this(description, EquipmentManager.NONE);
    }

    Usable(String description, int slot) {
      this.description = description;
      this.slot = slot;
    }

    public String getDescription() {
      return this.description;
    }

    public int getSlot() {
      return this.slot;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  public enum RobotUpgrade {
    PEA_SHOOTER("Pea Shooter", Part.TOP, 1, 5, Effect.COMBAT, "Shoot Pea"),
    BIRD_CAGE("Bird Cage", Part.TOP, 2, 5, Usable.FAMILIAR),
    SOLAR_PANEL("Solar Panel", Part.TOP, 3, 5),
    MANNEQUIN_HEAD("Mannequin Head", Part.TOP, 4, 15, Usable.HAT),
    MEAT_RADAR("Meat Radar", Part.TOP, 5, 30),
    JUNK_CANNON("Junk Cannon", Part.TOP, 6, 30, Effect.COMBAT, "Junk Blast"),
    TESLA_BLASTER("Tesla Blaster", Part.TOP, 7, 30, Effect.COMBAT, "Tesla Blast"),
    SNOW_BLOWER("Snow Blower", Part.TOP, 8, 40, Effect.COMBAT, "Blow Snow"),

    POUND_O_TRON("Pound-O-Tron", Part.LEFT, 1, 5, Effect.COMBAT, "Swing Pound-O-Tron"),
    REFLECTIVE_SHARD("Reflective Shard", Part.LEFT, 2, 5, "Resist All: +3"),
    METAL_DETECTOR("Metal Detector", Part.LEFT, 3, 5),
    VICE_GRIPS("Vice Grips", Part.LEFT, 4, 15, Usable.WEAPON),
    SNIPER_RIFLE("Sniper Rifle", Part.LEFT, 5, 30, Effect.COMBAT, "Snipe"),
    JUNK_MACE("Junk Mace", Part.LEFT, 6, 30, Effect.COMBAT, "Junk Mace Smash"),
    CAMOUFLAGE_CURTAIN("Camouflage Curtain", Part.LEFT, 7, 30),
    GREASE_GUN("Grease Gun", Part.LEFT, 8, 40, Effect.COMBAT, "Shoot Grease"),

    SLAB_O_MATIC("Slab-O-Matic", Part.RIGHT, 1, 5),
    JUNK_SHIELD("Junk Shield", Part.RIGHT, 2, 5),
    HORSESHOE_MAGNET("Horseshoe Magnet", Part.RIGHT, 3, 5),
    OMNI_CLAW("Omni-Claw", Part.RIGHT, 4, 15, Usable.OFFHAND),
    MAMMAL_PROD("Mammal Prod", Part.RIGHT, 5, 30, Effect.COMBAT, "Prod"),
    SOLENOID_PISTON("Solenoid Piston", Part.RIGHT, 6, 30, Effect.COMBAT, "Solenoid Slam"),
    BLARING_SPEAKER("Blaring Speaker", Part.RIGHT, 7, 30),
    SURPLUS_FLAMETHROWER("Surplus Flamethrower", Part.RIGHT, 8, 40, Effect.COMBAT, "Throw Flame"),

    BALD_TIRES("Bald Tires", Part.BOTTOM, 1, 5),
    ROCKET_CROTCH("Rocket Crotch", Part.BOTTOM, 2, 5, Effect.COMBAT, "Crotch Burn"),
    MOTORCYCLE_WHEEL("Motorcycle Wheel", Part.BOTTOM, 3, 5),
    ROBO_LEGS("Robo-Legs", Part.BOTTOM, 4, 15, Usable.PANTS),
    MAGNO_LEV("Magno-Lev", Part.BOTTOM, 5, 30),
    TANK_TREADS("Tank Treads", Part.BOTTOM, 6, 30),
    SNOWPLOW("Snowplow", Part.BOTTOM, 7, 30),

    LEVERAGE_COPROCESSNG("Leverage Coprocessing", "robot_muscle", 30),
    DYNAMIC_ARCANE_FLUX_MODELING("Dynamic Arcane Flux Modeling", "robot_mysticality", 30),
    UPGRADED_FASHION_SENSE("Upgraded Fashion Sensor", "robot_moxie", 30),
    FINANCE_NEURAL_NET("Finance Neural Net", "robot_meat", 30),
    SPATIAL_COMPRESSION_FNCTION("Spatial Compression Functions", "robot_hp1", 40),
    SELF_REPAIR_ROUTINES("Self-Repair Routines", "robot_regen", 40),
    WEATHER_CONTROL_ALGORITHMS("Weather Control Algorithms", "robot_resist", 40, "Resist All: +2"),
    IMPROVED_OPTICAL_PROCESSING("Improved Optical Processing", "robot_items", 40),
    TOPOLOGY_GRID("Topology Grid", "robot_shirt", 50, Usable.SHIRT),
    OVERCLOCKING("Overclocking", "robot_energy", 50),
    BIOMASS_PROCESSING_FUNCTION("Biomass Processing Function", "robot_potions", 50, Usable.POTIONS),
    HOLOGRAPHIC_DEFLECTOR_PROJECTION("Holographic Deflector Projection", "robot_hp2", 50);

    String name;

    // Energy or scraps
    int cost;

    // For body parts
    Part part;
    int index;

    // For CPU upgrade
    String keyword;

    // PASSIVE, COMBAT, EQUIP
    Effect effect;

    // PASSIVE
    Modifiers mods;

    // modifiers, skill name
    String string;

    // usable
    Usable usable;

    RobotUpgrade(String name, Part part, Effect effect, int cost) {
      this.name = name;
      this.part = part;
      this.effect = effect;
      this.cost = cost;
      this.mods = Modifiers.getModifiers("Robot", name);
    }

    // COMBAT
    RobotUpgrade(String name, Part part, int index, int cost, Effect effect, String skill) {
      this(name, part, effect, cost);
      this.index = index;
      this.keyword = "";
      this.string = skill;
      this.usable = Usable.NONE;
    }

    // PASSIVE
    RobotUpgrade(String name, Part part, int index, int cost, String description) {
      this(name, part, Effect.PASSIVE, cost);
      this.index = index;
      this.keyword = "";
      this.string = description;
      this.usable = Usable.NONE;
    }

    // PASSIVE
    RobotUpgrade(String name, Part part, int index, int cost) {
      this(name, part, Effect.PASSIVE, cost);
      this.index = index;
      this.keyword = "";
      this.string = this.mods.getString("Modifiers");
      this.usable = Usable.NONE;
    }

    // EQUIP
    RobotUpgrade(String name, Part part, int index, int cost, Usable thing) {
      this(name, part, Effect.EQUIP, cost);
      this.index = index;
      this.keyword = "";
      this.string = thing.toString();
      this.usable = thing;
    }

    // CPU PASSIVE
    RobotUpgrade(String name, String keyword, int cost, String string) {
      this(name, Part.CPU, Effect.PASSIVE, cost);
      this.index = 0;
      this.keyword = keyword;
      this.string = string;
      this.usable = Usable.NONE;
    }

    // CPU PASSIVE
    RobotUpgrade(String name, String keyword, int cost) {
      this(name, Part.CPU, Effect.PASSIVE, cost);
      this.index = 0;
      this.keyword = keyword;
      this.string = this.mods.getString("Modifiers");
      this.usable = Usable.NONE;
    }

    // CPU EQUIP
    RobotUpgrade(String name, String keyword, int cost, Usable thing) {
      this(name, Part.CPU, Effect.EQUIP, cost);
      this.index = 0;
      this.keyword = keyword;
      this.string = thing.toString();
      this.usable = thing;
    }

    public String getName() {
      return this.name;
    }

    public Part getPart() {
      return this.part;
    }

    public Effect getEffect() {
      return this.effect;
    }

    public int getIndex() {
      return this.index;
    }

    public String getKeyword() {
      return this.keyword;
    }

    public int getCost() {
      return this.cost;
    }

    public String getString() {
      return this.string;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static Set<RobotUpgrade> allUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allLeftUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allRightUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allTopUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allBottomUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allCPUUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allPassiveUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allCombatUpgrades = new HashSet<>();
  private static Set<RobotUpgrade> allEquipUpgrades = new HashSet<>();

  private static Map<Integer, RobotUpgrade> indexToLeft = new HashMap<>();
  private static Map<Integer, RobotUpgrade> indexToRight = new HashMap<>();
  private static Map<Integer, RobotUpgrade> indexToTop = new HashMap<>();
  private static Map<Integer, RobotUpgrade> indexToBottom = new HashMap<>();
  private static Map<String, RobotUpgrade> keywordToCPU = new HashMap<>();
  private static Map<String, RobotUpgrade> nameToUpgrade = new HashMap<>();

  static final Map<Part, Map<Integer, RobotUpgrade>> partToIndexMap = new HashMap<>();

  static {
    partToIndexMap.put(Part.TOP, indexToTop);
    partToIndexMap.put(Part.LEFT, indexToLeft);
    partToIndexMap.put(Part.RIGHT, indexToRight);
    partToIndexMap.put(Part.BOTTOM, indexToBottom);
  }

  private static RobotUpgrade urlFieldsToUpgrade(Part part, String chosenPart) {
    if (part == Part.CPU) {
      return keywordToCPU.get(chosenPart);
    }
    return partToIndexMap.get(part).get(StringUtilities.parseInt(chosenPart));
  }

  private static void addToUpgradeSets(RobotUpgrade upgrade) {
    allUpgrades.add(upgrade);
    nameToUpgrade.put(upgrade.getName(), upgrade);
    switch (upgrade.getPart()) {
      case TOP:
        allTopUpgrades.add(upgrade);
        indexToTop.put(upgrade.getIndex(), upgrade);
        break;
      case LEFT:
        allLeftUpgrades.add(upgrade);
        indexToLeft.put(upgrade.getIndex(), upgrade);
        break;
      case RIGHT:
        allRightUpgrades.add(upgrade);
        indexToRight.put(upgrade.getIndex(), upgrade);
        break;
      case BOTTOM:
        allBottomUpgrades.add(upgrade);
        indexToBottom.put(upgrade.getIndex(), upgrade);
        break;
      case CPU:
        allCPUUpgrades.add(upgrade);
        keywordToCPU.put(upgrade.getKeyword(), upgrade);
        break;
    }
    switch (upgrade.getEffect()) {
      case PASSIVE:
        allPassiveUpgrades.add(upgrade);
        break;
      case COMBAT:
        allCombatUpgrades.add(upgrade);
        break;
      case EQUIP:
        allEquipUpgrades.add(upgrade);
        break;
    }
  }

  static {
    for (RobotUpgrade upgrade : RobotUpgrade.values()) {
      addToUpgradeSets(upgrade);
    }
  }

  // *** Current state of Configuration

  private static RobotUpgrade currentLeft = null;
  private static RobotUpgrade currentTop = null;
  private static RobotUpgrade currentRight = null;
  private static RobotUpgrade currentBottom = null;
  private static Set<RobotUpgrade> currentCPU = new HashSet<>();

  public static void reset() {
    currentLeft = null;
    currentTop = null;
    currentRight = null;
    currentBottom = null;
    currentCPU.clear();
  }

  public static void loadConfiguration() {
    currentLeft = indexToLeft.get(Preferences.getInteger("youRobotLeft"));
    currentRight = indexToLeft.get(Preferences.getInteger("youRobotRight"));
    currentTop = indexToLeft.get(Preferences.getInteger("youRobotTop"));
    currentBottom = indexToLeft.get(Preferences.getInteger("youRobotBottom"));
    for (String keyword : Preferences.getString("youRobotCPUUpgrades").split(",")) {
      RobotUpgrade upgrade = keywordToCPU.get(keyword);
      if (upgrade != null) {
        currentCPU.add(upgrade);
      }
    }
  }

  public static void saveConfiguration() {
    Preferences.setInteger("youRobotLeft", currentLeft == null ? 0 : currentLeft.getIndex());
    Preferences.setInteger("youRobotRight", currentRight == null ? 0 : currentRight.getIndex());
    Preferences.setInteger("youRobotTop", currentTop == null ? 0 : currentTop.getIndex());
    Preferences.setInteger("youRobotBottom", currentBottom == null ? 0 : currentBottom.getIndex());
    String value =
        currentCPU.stream().map(RobotUpgrade::getKeyword).sorted().collect(Collectors.joining(","));
    Preferences.setString("youRobotCPUUpgrades", value);
  }

  public static boolean hasEquipped(String name) {
    RobotUpgrade upgrade = nameToUpgrade.get(name);
    return (upgrade == null) ? false : hasEquipped(upgrade);
  }

  public static boolean hasEquipped(RobotUpgrade upgrade) {
    switch (upgrade.getPart()) {
      case TOP:
        return currentTop == upgrade;
      case LEFT:
        return currentLeft == upgrade;
      case RIGHT:
        return currentRight == upgrade;
      case BOTTOM:
        return currentBottom == upgrade;
      case CPU:
        return currentCPU.contains(upgrade);
    }
    return false;
  }

  // *** Public methods to parse robot info from KoL responses

  // Parse avatar from:
  //     CharSheetRequest
  //     CharPaneRequest
  //     ChoiceManager (Reassembly Station)

  private static final Pattern AVATAR =
      Pattern.compile("(otherimages/robot/(left|right|top|bottom|body)(\\d+).png)\"");

  public static void parseAvatar(final String text) {
    List<String> images = new ArrayList<>();
    Matcher m = AVATAR.matcher(text);
    while (m.find()) {
      images.add(m.group(1));
      String section = m.group(2);
      int config = StringUtilities.parseInt(m.group(3));
      Preferences.setInteger("youRobot" + StringUtilities.toTitleCase(section), config);
    }
    KoLCharacter.setAvatar(images.toArray(new String[images.size()]));
  }

  // Parse CPU upgrades from:
  //     ChoiceManager (Reassembly Station)

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

  // Parse Statbot cost from:
  //     ChoiceManager (Statbot 5000)

  private static final Pattern STATBOT_COST =
      Pattern.compile("Current upgrade cost: <b>(\\d+) energy</b>");

  public static void parseStatbotCost(final String responseText) {
    Matcher m = STATBOT_COST.matcher(responseText);

    if (m.find()) {
      int cost = StringUtilities.parseInt(m.group(1));
      Preferences.setInteger("statbotUses", cost - 10);
    }
  }

  // *** Public methods to hide internal implementation, which currently
  // *** depend on use of user-visible properties.

  // Used by KoLCharacter.recalculateAdjustments
  public static void addRobotModifiers(Modifiers mods) {
    mods.add(Modifiers.getModifiers("RobotTop", Preferences.getString("youRobotTop")));
    mods.add(Modifiers.getModifiers("RobotRight", Preferences.getString("youRobotRight")));
    mods.add(Modifiers.getModifiers("RobotBottom", Preferences.getString("youRobotBottom")));
    mods.add(Modifiers.getModifiers("RobotLeft", Preferences.getString("youRobotLeft")));

    for (String cpuUpgrade : Preferences.getString("youRobotCPUUpgrades").split(",")) {
      mods.add(Modifiers.getModifiers("RobotCPU", cpuUpgrade));
    }
  }

  // Used by FamiliarData.canEquip
  public static boolean canUseFamiliars() {
    return Preferences.getInteger("youRobotTop") == 2;
  }

  // Used by EquipmentManager.canEquip
  public static boolean canEquip(final int type) {
    switch (type) {
      case KoLConstants.EQUIP_HAT:
        return Preferences.getInteger("youRobotTop") == 4;
      case KoLConstants.EQUIP_WEAPON:
        return Preferences.getInteger("youRobotLeft") == 4;
      case KoLConstants.EQUIP_OFFHAND:
        return Preferences.getInteger("youRobotRight") == 4;
      case KoLConstants.EQUIP_PANTS:
        return Preferences.getInteger("youRobotBottom") == 4;
      case KoLConstants.EQUIP_SHIRT:
        return Preferences.getString("youRobotCPUUpgrades").contains("robot_shirt");
    }
    return true;
  }

  // Used by KoLCharacter.canUsePotions
  public static boolean canUsePotions() {
    return Preferences.getString("youRobotCPUUpgrades").contains("robot_potions");
  }

  // *** Interface for ChoiceManager

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;
    switch (ChoiceManager.lastChoice) {
      case 1445: // Reassembly Station
        parseAvatar(text);

        if (request.getURLString().contains("show=cpus")) {
          parseCPUUpgrades(text);
        }
        break;

      case 1447: // Statbot 5000
        {
          parseStatbotCost(text);
          break;
        }
    }
  }

  public static void postChoice1(final String urlString, final GenericRequest request) {
    String text = request.responseText;

    switch (ChoiceManager.lastChoice) {
      case 1445: // Reassembly Station
        {
          // KoL may have unequipped some items based on our selection
          Matcher partMatcher = Pattern.compile("part=([^&]*)").matcher(urlString);
          Matcher chosenPartMatcher = Pattern.compile("p=([^&]*)").matcher(urlString);
          String part = partMatcher.find() ? partMatcher.group(1) : null;
          int chosenPart =
              chosenPartMatcher.find() ? StringUtilities.parseInt(chosenPartMatcher.group(1)) : 0;

          if (part != null && !part.equals("cpus") && chosenPart != 0) {
            // If we have set our "top" to anything other than 2, we now have no familiar
            if (part.equals("top") && chosenPart != 2) {
              KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
            }

            // If we've set any part of the main body to anything other than 4, we are now missing
            // an equip
            if (chosenPart != 4) {
              int slot = -1;

              switch (part) {
                case "top":
                  slot = EquipmentManager.HAT;
                  break;
                case "right":
                  slot = EquipmentManager.OFFHAND;
                  break;
                case "bottom":
                  slot = EquipmentManager.PANTS;
                  break;
                case "left":
                  slot = EquipmentManager.WEAPON;
                  break;
              }

              if (slot != -1) {
                EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
              }
            }
          }

          parseAvatar(text);

          if (urlString.contains("show=cpus")) {
            parseCPUUpgrades(text);
          }

          KoLCharacter.updateStatus();
          break;
        }
      case 1447: // Statbot 5000
        {
          parseStatbotCost(text);
          KoLCharacter.updateStatus();
          break;
        }
    }
  }

  private static Map<Integer, String> optionToStat = new HashMap<>();

  static {
    optionToStat.put(1, "Muscle");
    optionToStat.put(2, "Mysticality");
    optionToStat.put(3, "Moxie");
  }

  // Why not use GenericRequest.extractField?
  //
  // Answer: It searches for last instance of the field name, which
  // REALLY does not work for "p=robot_hp"
  //
  // GenericRequest.extractField should have a test written and fixed

  public static final String extractField(String urlString, final String field) {
    // We need to find the last instance of a field, since KoL URLs can
    // have duplicate field names and only the last counts.
    //
    // We also only want to match the field name against a field name,
    // not a value

    int start = urlString.indexOf("?");
    while (start > 0) {
      int end = urlString.indexOf("&", start + 1);
      String extracted =
          (end == -1) ? urlString.substring(start + 1) : urlString.substring(start + 1, end);
      int equals = extracted.indexOf("=");
      String name = (equals == -1) ? extracted : extracted.substring(0, equals);
      if (name.equals(field)) {
        return extracted;
      }
      start = end;
    }
    return null;
  }

  public static final String extractFieldValue(final String urlString, final String field) {
    String extracted = extractField(urlString, field);
    if (extracted == null) {
      return "";
    }
    int equals = extracted.indexOf("=");
    if (equals == -1) {
      return extracted;
    }
    return extracted.substring(equals + 1);
  }

  public static final boolean registerRequest(final String urlString) {
    // This is called from ChoiceManager to handle the following choices:
    //   1445 - Reassembly Station
    //   1447 - Statbot 5000

    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);

    String message;

    // choice.php?whichchoice=1445&show=top
    // choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=7
    // choice.php?whichchoice=1445&show=cpus
    // choice.php?pwd&whichchoice=1445&part=cpus&show=cpus&option=2&p=robot_resist

    if (choice == 1445) {
      String showKeyword = extractFieldValue(urlString, "show");
      Part part = keywordToPart.get(showKeyword);
      if (decision == 0) {
        message = "Inspecting " + part + " options at the Reassembly Station.";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        return true;
      }

      // We are buying an attachment.
      String chosenPart = extractFieldValue(urlString, "p");
      if (chosenPart != null) {
        RobotUpgrade upgrade = urlFieldsToUpgrade(part, chosenPart);
        if (upgrade != null) {
          if (part == Part.CPU) {
            message =
                "Upgrading your CPU with " + upgrade + " for " + upgrade.getCost() + " energy.";
          } else {
            message =
                "Installing "
                    + upgrade
                    + " as your "
                    + part
                    + " for "
                    + upgrade.getCost()
                    + " scrap.";
          }
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          return true;
        }
      }

      // We don't expect to get here, but log something, at least.
      RequestLogger.updateSessionLog(urlString);
      return true;
    }

    // choice.php?whichchoice=1447&option=1
    // choice.php?whichchoice=1447&option=2
    // choice.php?whichchoice=1447&option=3

    if (choice == 1447) {
      if (decision != 0) {
        String stat = optionToStat.get(decision);
        if (stat != null) {
          int cost = Preferences.getInteger("statbotUses") + 10;
          message = "Spending " + cost + " energy to upgrade " + stat + " by 5 points.";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }
      return true;
    }

    return true;
  }
}
