package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  public enum Slot {
    TOP,
    LEFT,
    RIGHT,
    BOTTOM,
    CPU;
  }

  public enum Effect {
    PASSIVE,
    COMBAT,
    EQUIP;
  }

  public enum Usable {
    NONE("no special effect"),
    HAT("can equip hats"),
    WEAPON("can equip weapons"),
    OFFHAND("can equip offhands"),
    SHIRT("can equip shirts"),
    PANTS("can equip pants"),
    FAMILIAR("can use familiars"),
    POTIONS("can use potions hats");

    String description;

    Usable(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  public enum RobotUpgrade {
    PEA_SHOOTER("Pea Shooter", Slot.TOP, 1, Effect.COMBAT, "Shoot Pea"),
    BIRD_CAGE("Bird Cage", Slot.TOP, 2, Effect.EQUIP, Usable.FAMILIAR),
    SOLAR_PANEL("Solar Panel", Slot.TOP, 3, Effect.PASSIVE, "Energy: +1"),
    MANNEQUIN_HEAD("Mannequin Head", Slot.TOP, 4, Effect.EQUIP, Usable.HAT),
    MEAT_RADAR("Meat Radar", Slot.TOP, 5, Effect.PASSIVE, "Meat Drop: +50"),
    JUNK_CANNON("Junk Cannon", Slot.TOP, 6, Effect.COMBAT, "Junk Blast"),
    TESLA_BLASTER("Tesla Blaster", Slot.TOP, 7, Effect.COMBAT, "Tesla Blast"),
    SNOW_BLOWER("Snow Blower", Slot.TOP, 8, Effect.COMBAT, "Blow Snow"),

    POUND_O_TRON("Pound-O-Tron", Slot.LEFT, 1, Effect.COMBAT, "Swing Pound-O-Tron"),
    REFLECTIVE_SHARD("Reflective Shard", Slot.LEFT, 2, Effect.PASSIVE, "Resist All: +3"),
    METAL_DETECTOR("Metal Detector", Slot.LEFT, 3, Effect.PASSIVE, "Item Drop: +30"),
    VICE_GRIPS("Vice Grips", Slot.LEFT, 4, Effect.EQUIP, Usable.WEAPON),
    SNIPER_RIFLE("Sniper Rifle", Slot.LEFT, 5, Effect.COMBAT, "Snipe"),
    JUNK_MACE("Junk Mace", Slot.LEFT, 6, Effect.COMBAT, "Junk Mace Smash"),
    CAMOUFLAGE_CURTAIN("Camouflage Curtain", Slot.LEFT, 7, Effect.PASSIVE, "Combat Rate: -15"),
    GREASE_GUN("Grease Gun", Slot.LEFT, 8, Effect.COMBAT, "Shoot Grease"),

    SLAB_O_MATIC("Slab-O-Matic", Slot.RIGHT, 1, Effect.PASSIVE, "Maximum HP: +30"),
    JUNK_SHIELD(
        "Junk Shield",
        Slot.RIGHT,
        2,
        Effect.PASSIVE,
        "Damage Reduction: +10, Damage Absorption: +50"),
    HORSESHOE_MAGNET("Horseshoe Magnet", Slot.RIGHT, 3, Effect.PASSIVE, "Scrap: +1"),
    OMNI_CLAW("Omni-Claw", Slot.RIGHT, 4, Effect.EQUIP, Usable.OFFHAND),
    MAMMAL_PROD("Mammal Prod", Slot.RIGHT, 5, Effect.COMBAT, "Prod"),
    SOLENOID_PISTON("Solenoid Piston", Slot.RIGHT, 6, Effect.COMBAT, "Solenoid Slam"),
    BLARING_SPEAKER("Blaring Speaker", Slot.RIGHT, 7, Effect.PASSIVE, "Monster Level: +30"),
    SURPLUS_FLAMETHROWER("Surplus Flamethrower", Slot.RIGHT, 8, Effect.COMBAT, "Throw Flame"),

    BALD_TIRES("Bald Tires", Slot.BOTTOM, 1, Effect.PASSIVE, "Maximum HP: +10"),
    ROCKET_CROTCH("Rocket Crotch", Slot.BOTTOM, 2, Effect.COMBAT, "Crotch Burn"),
    MOTORCYCLE_WHEEL("Motorcycle Wheel", Slot.BOTTOM, 3, Effect.PASSIVE, "Initiative: +30"),
    ROBO_LEGS("Robo-Legs", Slot.BOTTOM, 4, Effect.EQUIP, Usable.PANTS),
    MAGNO_LEV("Magno-Lev", Slot.BOTTOM, 5, Effect.PASSIVE, "Item Drop: +30"),
    TANK_TREADS(
        "Tank Treads", Slot.BOTTOM, 6, Effect.PASSIVE, "Maximum HP: +50, Damage Reduction: +10"),
    SNOWPLOW("Snowplow", Slot.BOTTOM, 7, Effect.PASSIVE, "Scrap: +1"),

    CPU1("Leverage Coprocessing", "robot_muscle", "Muscle: +15"),
    CPU2("Dynamic Arcane Flux Modeling", "robot_mysticality", "Mysticality: +15"),
    CPU3("Upgraded Fashion Sensor", "robot_moxie", "Moxie: +15"),
    CPU4("Finance Neural Net", "robot_meat", "Meat Drop: +20"),
    CPU5("Spatial Compression Functions", "robot_hp1", "Maximum HP: +30"),
    CPU6("Self-Repair Routines", "robot_regen", "HP Regen Min: +10, HP Regen Max: +10"),
    CPU7("Weather Control Algorithms", "robot_resist", "Resist All: +2"),
    CPU8("Improved Optical Processing", "robot_items", "Item Drop: +20"),
    CPU9("Topology Grid", "robot_shirt", Usable.SHIRT),
    CPU10("Overclocking Coprocessing", "robot_energy", "Energy: +1"),
    CPU11("Biomass Processing Function", "robot_potions", Usable.POTIONS),
    CPU12("Holographic Deflector Projection", "robot_hp2", "Maximum HP: +30");

    String name;

    // For body parts
    Slot slot;
    int index;

    // For CPU upgrade
    String keyword;

    // PASSIVE, COMBAT, EQUIP
    Effect effect;

    // modifiers, skill
    String string;
    // usable
    Usable usable;

    // COMBAT or PASSIVE
    RobotUpgrade(String name, Slot slot, int index, Effect effect, String skill) {
      this.name = name;
      this.slot = slot;
      this.index = index;
      this.keyword = "";
      this.effect = effect;
      this.string = skill;
      this.usable = Usable.NONE;
    }

    // EQUIP
    RobotUpgrade(String name, Slot slot, int index, Effect effect, Usable thing) {
      this.name = name;
      this.slot = slot;
      this.index = index;
      this.keyword = "";
      this.effect = effect;
      this.string = thing.toString();
      this.usable = thing;
    }

    // PASSIVE
    RobotUpgrade(String name, String keyword, String string) {
      this.name = name;
      this.slot = Slot.CPU;
      this.index = 0;
      this.keyword = keyword;
      this.effect = Effect.PASSIVE;
      this.string = string;
      this.usable = Usable.NONE;
    }

    // PASSIVE
    RobotUpgrade(String name, String keyword, Usable thing) {
      this.name = name;
      this.slot = Slot.CPU;
      this.index = 0;
      this.keyword = keyword;
      this.effect = Effect.PASSIVE;
      this.string = thing.toString();
      this.usable = thing;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

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
  // *** depends on use of user-visible properties.

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
  // Used by KoLCharacter.isTorsoAware
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
          KoLCharacter.updateStatus();
          break;
        }
    }
  }

  public static final boolean registerRequest(final String urlString) {
    // *** This how Choice Manager logs non-special choices. Fix this to
    // *** log something more meaningful.

    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);
    if (decision != 0) {
      // Figure out which decision we took
      String desc = ChoiceManager.choiceDescription(choice, decision);
      RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
    }
    RequestLogger.updateSessionLog(urlString);

    return true;
  }
}
