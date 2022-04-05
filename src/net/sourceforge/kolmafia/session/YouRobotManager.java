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
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class YouRobotManager {

  private YouRobotManager() {}

  private static final Map<String, Part> keywordToPart = new HashMap<>();

  // Index upgrade maps
  private static final Map<Integer, RobotUpgrade> indexToLeft = new HashMap<>();
  private static final Map<Integer, RobotUpgrade> indexToRight = new HashMap<>();
  private static final Map<Integer, RobotUpgrade> indexToTop = new HashMap<>();
  private static final Map<Integer, RobotUpgrade> indexToBottom = new HashMap<>();
  private static final Map<String, RobotUpgrade> keywordToCPU = new HashMap<>();

  private static final Map<Part, Map<Integer, RobotUpgrade>> partToIndexMap = new HashMap<>();

  public static enum Part {
    TOP("top", "Top Attachment", Usable.HAT, indexToTop),
    LEFT("left", "Left Arm", Usable.WEAPON, indexToLeft),
    RIGHT("right", "Right Arm", Usable.OFFHAND, indexToRight),
    BOTTOM("bottom", "Propulsion System", Usable.PANTS, indexToBottom),
    CPU("cpus", "CPU Upgrade");

    String keyword;
    String section;
    String name;
    Usable usable;

    Part(String keyword, String name) {
      this(keyword, name, Usable.NONE, null);
    }

    Part(String keyword, String name, Usable usable, Map<Integer, RobotUpgrade> indexMap) {
      this.keyword = keyword;
      this.section = StringUtilities.toTitleCase(keyword);
      this.name = name;
      this.usable = usable;
      keywordToPart.put(keyword, this);
      if (indexMap != null) {
        partToIndexMap.put(this, indexMap);
      }
    }

    String getKeyword() {
      return this.keyword;
    }

    String getSection() {
      return this.section;
    }

    String getName() {
      return this.name;
    }

    Usable getUsable() {
      return this.usable;
    }

    int getSlot() {
      return this.usable.getSlot();
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static enum Effect {
    PASSIVE,
    COMBAT,
    EQUIP;
  }

  public static enum Usable {
    NONE("no special effect"),
    HAT("can equip hats", EquipmentManager.HAT),
    WEAPON("can equip weapons", EquipmentManager.WEAPON),
    OFFHAND("can equip offhands", EquipmentManager.OFFHAND),
    SHIRT("can equip shirts", EquipmentManager.SHIRT),
    PANTS("can equip pants", EquipmentManager.PANTS),
    FAMILIAR("can use familiars"),
    POTIONS("can use potions");

    String description;
    int consume;
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

  private static final Map<String, RobotUpgrade> nameToUpgrade = new HashMap<>();

  public static enum RobotUpgrade {
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
      this.index = 0;
      this.keyword = "";
      this.usable = Usable.NONE;
      addToUpgradeSets();
    }

    // COMBAT
    RobotUpgrade(String name, Part part, int index, int cost, Effect effect, String skill) {
      this(name, part, effect, cost);
      this.index = index;
      this.string = skill;
      addToIndexMaps();
    }

    // PASSIVE
    RobotUpgrade(String name, Part part, int index, int cost, String description) {
      this(name, part, Effect.PASSIVE, cost);
      this.index = index;
      this.string = description;
      addToIndexMaps();
    }

    // PASSIVE
    RobotUpgrade(String name, Part part, int index, int cost) {
      this(name, part, Effect.PASSIVE, cost);
      this.index = index;
      this.string = this.mods.getString("Modifiers");
      addToIndexMaps();
    }

    // EQUIP
    RobotUpgrade(String name, Part part, int index, int cost, Usable thing) {
      this(name, part, Effect.EQUIP, cost);
      this.index = index;
      this.string = thing.toString();
      this.usable = thing;
      addToIndexMaps();
    }

    // CPU PASSIVE
    RobotUpgrade(String name, String keyword, int cost, String string) {
      this(name, Part.CPU, Effect.PASSIVE, cost);
      this.keyword = keyword;
      this.string = string;
      addToIndexMaps();
    }

    // CPU PASSIVE
    RobotUpgrade(String name, String keyword, int cost) {
      this(name, Part.CPU, Effect.PASSIVE, cost);
      this.keyword = keyword;
      this.string = this.mods.getString("Modifiers");
      addToIndexMaps();
    }

    // CPU EQUIP
    RobotUpgrade(String name, String keyword, int cost, Usable thing) {
      this(name, Part.CPU, Effect.EQUIP, cost);
      this.keyword = keyword;
      this.string = thing.toString();
      this.usable = thing;
      addToIndexMaps();
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

    public Modifiers getMods() {
      return this.mods;
    }

    public Usable getUsable() {
      return this.usable;
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

    public void addCombatSkill() {
      if (this.effect == Effect.COMBAT) {
        KoLCharacter.addAvailableCombatSkill(this.string);
      }
    }

    public void removeCombatSkill() {
      if (this.effect == Effect.COMBAT) {
        KoLCharacter.removeAvailableCombatSkill(this.string);
      }
    }

    private void addToUpgradeSets() {
      nameToUpgrade.put(this.name, this);
    }

    private void addToIndexMaps() {
      switch (this.part) {
        case TOP:
          indexToTop.put(this.index, this);
          break;
        case LEFT:
          indexToLeft.put(this.index, this);
          break;
        case RIGHT:
          indexToRight.put(this.index, this);
          break;
        case BOTTOM:
          indexToBottom.put(this.index, this);
          break;
        case CPU:
          keywordToCPU.put(this.keyword, this);
          break;
      }
    }
  }

  static {
    // This forces the RobotUpgrade enum to be initialized, which will populate
    // all the various sets and maps from the constructors.
    RobotUpgrade[] values = RobotUpgrade.values();
  }

  private static RobotUpgrade urlFieldsToUpgrade(Part part, String chosenPart) {
    if (part == null || chosenPart == null) {
      return null;
    }
    return (part != Part.CPU)
        ? partToIndexMap.get(part).get(StringUtilities.parseInt(chosenPart))
        : keywordToCPU.get(chosenPart);
  }

  // *** Current state of Configuration

  private static final Map<Part, RobotUpgrade> currentParts = new HashMap<>();
  private static final Set<RobotUpgrade> currentCPU = new HashSet<>();

  // For testing
  public static void reset() {
    currentParts.clear();
    currentCPU.clear();
  }

  public static boolean hasEquipped(String name) {
    RobotUpgrade upgrade = nameToUpgrade.get(name);
    return (upgrade == null) ? false : hasEquipped(upgrade);
  }

  public static boolean hasEquipped(RobotUpgrade upgrade) {
    Part part = upgrade.getPart();
    switch (part) {
      case TOP:
      case LEFT:
      case RIGHT:
      case BOTTOM:
        return currentParts.get(part) == upgrade;
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
    Map<Part, RobotUpgrade> parts = new HashMap<>();
    List<String> images = new ArrayList<>();
    boolean changed = false;

    Matcher m = AVATAR.matcher(text);
    while (m.find()) {
      images.add(m.group(1));
      String section = m.group(2);
      int index = StringUtilities.parseInt(m.group(3));

      if (section.equals("body")) {
        // This never changes, but make sure it is set once, at least
        Preferences.setInteger("youRobotBody", index);
      } else {
        // Set the "current" configuration variables
        Part part = keywordToPart.get(section);
        RobotUpgrade upgrade = partToIndexMap.get(part).get(index);
        changed |= installUpgrade(part, upgrade);
      }
    }

    // Save the avatar so you can admire it on the Daily Deeds frame
    if (changed) {
      KoLCharacter.setAvatar(images);
      KoLCharacter.recalculateAdjustments();
    }
  }

  // Parse CPU upgrades from:
  //     ChoiceManager (Reassembly Station)

  private static final Pattern CPU_UPGRADE_INSTALLED =
      Pattern.compile("<button.*?value=\"([a-z0-9_]+)\"[^\\(]+\\(already installed\\)");

  public static void parseCPUUpgrades(final String text) {
    Matcher m = CPU_UPGRADE_INSTALLED.matcher(text);
    boolean changed = false;

    while (m.find()) {
      String keyword = m.group(1);
      RobotUpgrade upgrade = keywordToCPU.get(keyword);
      changed |= installCPUUpgrade(upgrade);
    }

    if (changed) {
      KoLCharacter.recalculateAdjustments();
    }
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

  // *** Public methods to hide internal implementation.

  // *** For use by tests
  public static void testInstallUpgrade(RobotUpgrade upgrade) {
    Part part = upgrade.getPart();
    if (part != Part.CPU) {
      installUpgrade(part, upgrade);
    } else {
      installCPUUpgrade(upgrade);
    }
  }

  private static boolean installUpgrade(Part part, RobotUpgrade upgrade) {
    if (upgrade == null) {
      return false;
    }

    // Remove current part, if any.
    RobotUpgrade previous = currentParts.get(part);
    if (upgrade == previous) {
      return false;
    }

    uninstallUpgrade(part, previous);

    // Add new part to current configuration
    currentParts.put(part, upgrade);

    switch (upgrade.getEffect()) {
      case COMBAT:
        upgrade.addCombatSkill();
        break;
      case EQUIP:
        EquipmentManager.updateEquipmentList(part.getSlot());
        EquipmentManager.updateNormalOutfits();
        break;
    }

    // Set the legacy properties for use by scripts
    Preferences.setInteger("youRobot" + part.getSection(), upgrade.getIndex());

    return true;
  }

  private static void uninstallUpgrade(Part part, RobotUpgrade upgrade) {
    if (upgrade == null) {
      return;
    }

    if (upgrade == RobotUpgrade.BIRD_CAGE) {
      // If replacing a Bird Cage, drop familiar
      KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
    } else if (upgrade.getEffect() == Effect.EQUIP) {
      // If replacing another equipment part, drop the equipment
      int slot = part.getSlot();
      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
      EquipmentManager.updateEquipmentList(slot);
      EquipmentManager.updateNormalOutfits();
    } else if (upgrade.getEffect() == Effect.COMBAT) {
      upgrade.removeCombatSkill();
    }

    // We could reset the legacy property, but this method is only called when
    // installing an upgrade to replace the current one
  }

  private static boolean installCPUUpgrade(RobotUpgrade upgrade) {
    if (upgrade == null) {
      return false;
    }

    if (currentCPU.contains(upgrade)) {
      return false;
    }
    currentCPU.add(upgrade);

    if (upgrade == RobotUpgrade.BIOMASS_PROCESSING_FUNCTION) {
      NamedListenerRegistry.fireChange("(potions)");
    } else if (upgrade == RobotUpgrade.TOPOLOGY_GRID) {
      // This was detected on the charsheet when you log in, but tests do not
      // parse the charasheet
      KoLCharacter.addAvailableSkill(SkillPool.TORSO);
      EquipmentManager.updateEquipmentList(EquipmentManager.SHIRT);
      EquipmentManager.updateNormalOutfits();
    } else {
      KoLCharacter.recalculateAdjustments();
    }

    String value =
        currentCPU.stream().map(RobotUpgrade::getKeyword).sorted().collect(Collectors.joining(","));
    Preferences.setString("youRobotCPUUpgrades", value);
    return true;
  }

  // Used by KoLCharacter.recalculateAdjustments
  public static void addRobotModifiers(Modifiers mods) {
    for (RobotUpgrade upgrade : currentParts.values()) {
      mods.add(upgrade.getMods());
    }

    for (RobotUpgrade upgrade : currentCPU) {
      mods.add(upgrade.getMods());
    }
  }

  // Used by FamiliarData.canEquip
  public static boolean canUseFamiliars() {
    return hasEquipped(RobotUpgrade.BIRD_CAGE);
  }

  // Used by EquipmentManager.canEquip
  public static boolean canEquip(final int type) {
    switch (type) {
      case KoLConstants.EQUIP_HAT:
        return hasEquipped(RobotUpgrade.MANNEQUIN_HEAD);
      case KoLConstants.EQUIP_WEAPON:
        return hasEquipped(RobotUpgrade.VICE_GRIPS);
      case KoLConstants.EQUIP_OFFHAND:
        return hasEquipped(RobotUpgrade.OMNI_CLAW);
      case KoLConstants.EQUIP_SHIRT:
        return hasEquipped(RobotUpgrade.TOPOLOGY_GRID);
      case KoLConstants.EQUIP_PANTS:
        return hasEquipped(RobotUpgrade.ROBO_LEGS);
    }

    // Any other slot is allowed
    return true;
  }

  // Used by KoLCharacter.canUsePotions
  public static boolean canUsePotions() {
    return hasEquipped(RobotUpgrade.BIOMASS_PROCESSING_FUNCTION);
  }

  // *** Interface for ChoiceManager

  public static void visitChoice(final GenericRequest request) {
    int choice = ChoiceManager.lastChoice;
    String text = request.responseText;

    if (choice == 1445) {
      // Reassembly Station
      parseAvatar(text);
      if (request.getURLString().contains("show=cpus")) {
        parseCPUUpgrades(text);
      }
      return;
    }

    if (choice == 1447) {
      // Statbot 5000
      parseStatbotCost(text);
      return;
    }
  }

  public static void postChoice1(final String urlString, final GenericRequest request) {

    int choice = ChoiceManager.lastChoice;
    String text = request.responseText;

    if (choice == 1445) {
      // Reassembly Station

      String showKeyword = extractFieldValue(urlString, "show");
      Part part = keywordToPart.get(showKeyword);
      String chosenPart = extractFieldValue(urlString, "p");
      RobotUpgrade upgrade = urlFieldsToUpgrade(part, chosenPart);
      boolean changed = false;

      if (upgrade == null) {
        return;
      }

      if (part == Part.CPU) {
        changed = installCPUUpgrade(upgrade);
        KoLCharacter.setYouRobotEnergy(KoLCharacter.getYouRobotEnergy() - upgrade.getCost());
      } else {
        changed = installUpgrade(part, upgrade);
        KoLCharacter.setYouRobotScraps(KoLCharacter.getYouRobotScraps() - upgrade.getCost());
      }

      if (changed) {
        parseAvatar(text);
      }

      KoLCharacter.updateStatus();
      return;
    }

    if (choice == 1447) {
      // Statbot 5000

      parseStatbotCost(text);
      if (!text.contains("You don't have enough Energy to do that.")) {
        // Cost starts at 10 and goes up by one each time.
        // If we are here, we have just parsed the NEXT cost.
        int cost = Preferences.getInteger("statbotUses") + 10 - 1;
        KoLCharacter.setYouRobotEnergy(KoLCharacter.getYouRobotEnergy() - cost);
        EquipmentManager.updateEquipmentLists();
        KoLCharacter.updateStatus();
      }
      return;
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
      return null;
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
      if (part == null) {
        RequestLogger.updateSessionLog(urlString);
        return true;
      }

      if (decision == 0) {
        message = "Inspecting " + part + " options at the Reassembly Station.";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        return true;
      }

      // We are buying an attachment.
      String chosenPart = extractFieldValue(urlString, "p");
      RobotUpgrade upgrade = urlFieldsToUpgrade(part, chosenPart);
      if (upgrade != null) {
        if (part == Part.CPU) {
          message = "Upgrading your CPU with " + upgrade + " for " + upgrade.getCost() + " energy.";
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
