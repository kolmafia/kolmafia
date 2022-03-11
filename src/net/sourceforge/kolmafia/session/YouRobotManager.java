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
